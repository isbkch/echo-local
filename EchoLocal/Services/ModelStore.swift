import Foundation
#if os(macOS)
import AppKit
#endif

@MainActor
final class ModelStore: ObservableObject {
    struct RemoteAsset: Hashable {
        let fileName: String
        let expectedBytes: Int64

        var sourceURL: URL {
            URL(
                string: "https://huggingface.co/brannala64/kokoro-82m-safetensors/resolve/main/\(fileName)"
            )!
        }
    }

    enum State: Equatable {
        case checking
        case missing
        case downloading(progress: Double, fileName: String)
        case ready
        case failed(String)
    }

    static let modelAsset = RemoteAsset(
        fileName: "kokoro-v1_0.safetensors",
        expectedBytes: 327_115_152
    )

    static let voiceAssets = KokoroVoice.curated.map {
        RemoteAsset(fileName: $0.fileName, expectedBytes: 522_339)
    }

    static let allAssets = [modelAsset] + voiceAssets

    @Published private(set) var state: State = .checking

    private var downloadTask: Task<Void, Never>?

    var storageDirectory: URL {
        applicationSupportDirectory
            .appendingPathComponent("Echolocal", isDirectory: true)
            .appendingPathComponent("Kokoro", isDirectory: true)
    }

    private var legacyStorageDirectory: URL {
        applicationSupportDirectory
            .appendingPathComponent("Local Audio", isDirectory: true)
            .appendingPathComponent("Kokoro", isDirectory: true)
    }

    private var applicationSupportDirectory: URL {
        FileManager.default
            .urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
    }

    var modelURL: URL {
        storageDirectory.appendingPathComponent(Self.modelAsset.fileName)
    }

    var totalDownloadSize: Int64 {
        Self.allAssets.reduce(0) { $0 + $1.expectedBytes }
    }

    var isReady: Bool {
        state == .ready
    }

    init() {
        migrateLegacyModelIfNeeded()
        refresh()
    }

    func voiceURL(for voice: KokoroVoice) -> URL {
        storageDirectory.appendingPathComponent(voice.fileName)
    }

    func refresh() {
        state = Self.allAssets.allSatisfy(isAssetValid) ? .ready : .missing
    }

    private func migrateLegacyModelIfNeeded() {
        let fileManager = FileManager.default

        guard !fileManager.fileExists(atPath: storageDirectory.path),
              fileManager.fileExists(atPath: legacyStorageDirectory.path) else {
            return
        }

        do {
            try fileManager.createDirectory(
                at: storageDirectory.deletingLastPathComponent(),
                withIntermediateDirectories: true
            )
            try fileManager.moveItem(at: legacyStorageDirectory, to: storageDirectory)
        } catch {
            // A later download can recover from a failed migration without
            // interrupting launch; the old files remain untouched.
        }
    }

    func download() {
        guard downloadTask == nil else { return }

        downloadTask = Task { [weak self] in
            guard let self else { return }
            defer { downloadTask = nil }

            do {
                try prepareStorageDirectory()

                var completedBytes = Self.allAssets
                    .filter(isAssetValid)
                    .reduce(Int64.zero) { $0 + $1.expectedBytes }

                for asset in Self.allAssets where !isAssetValid(asset) {
                    let baseBytes = completedBytes
                    let downloader = AssetDownloader { [weak self] writtenBytes in
                        Task { @MainActor in
                            guard let self else { return }
                            let fraction = Double(baseBytes + writtenBytes) / Double(self.totalDownloadSize)
                            self.state = .downloading(
                                progress: min(max(fraction, 0), 1),
                                fileName: self.friendlyName(for: asset)
                            )
                        }
                    }

                    state = .downloading(
                        progress: Double(completedBytes) / Double(totalDownloadSize),
                        fileName: friendlyName(for: asset)
                    )

                    let temporaryURL = try await downloader.download(from: asset.sourceURL)
                    try validate(fileAt: temporaryURL, as: asset)
                    try install(fileAt: temporaryURL, as: asset)
                    completedBytes += asset.expectedBytes
                }

                refresh()
            } catch {
                state = .failed(error.localizedDescription)
            }
        }
    }

    func installExisting(from sourceDirectory: URL) throws {
        let accessed = sourceDirectory.startAccessingSecurityScopedResource()
        defer {
            if accessed {
                sourceDirectory.stopAccessingSecurityScopedResource()
            }
        }

        for asset in Self.allAssets {
            let source = sourceDirectory.appendingPathComponent(asset.fileName)
            guard FileManager.default.fileExists(atPath: source.path) else {
                throw StoreError.missingFile(asset.fileName)
            }
            try validate(fileAt: source, as: asset)
        }

        try prepareStorageDirectory()

        for asset in Self.allAssets {
            try install(
                fileAt: sourceDirectory.appendingPathComponent(asset.fileName),
                as: asset,
                copiesSource: true
            )
        }

        refresh()
    }

    func revealModelFolder() {
        #if os(macOS)
        try? FileManager.default.createDirectory(
            at: storageDirectory,
            withIntermediateDirectories: true
        )
        NSWorkspace.shared.activateFileViewerSelecting([modelURL])
        #endif
    }

    private func friendlyName(for asset: RemoteAsset) -> String {
        if asset == Self.modelAsset {
            return "Kokoro model"
        }

        let voiceID = asset.fileName.replacingOccurrences(of: ".safetensors", with: "")
        return "\(KokoroVoice.voice(withID: voiceID).name) voice"
    }

    private func prepareStorageDirectory() throws {
        try FileManager.default.createDirectory(
            at: storageDirectory,
            withIntermediateDirectories: true
        )

        #if os(iOS)
        var resourceValues = URLResourceValues()
        resourceValues.isExcludedFromBackup = true
        var directory = storageDirectory
        try directory.setResourceValues(resourceValues)
        #endif
    }

    private func isAssetValid(_ asset: RemoteAsset) -> Bool {
        let url = storageDirectory.appendingPathComponent(asset.fileName)
        guard
            let values = try? url.resourceValues(forKeys: [.fileSizeKey]),
            let size = values.fileSize
        else {
            return false
        }
        return Int64(size) == asset.expectedBytes
    }

    private func validate(fileAt url: URL, as asset: RemoteAsset) throws {
        let values = try url.resourceValues(forKeys: [.fileSizeKey])
        guard Int64(values.fileSize ?? 0) == asset.expectedBytes else {
            throw StoreError.invalidFile(asset.fileName)
        }
    }

    private func install(
        fileAt sourceURL: URL,
        as asset: RemoteAsset,
        copiesSource: Bool = false
    ) throws {
        let fileManager = FileManager.default
        let destination = storageDirectory.appendingPathComponent(asset.fileName)

        if sourceURL.standardizedFileURL == destination.standardizedFileURL {
            return
        }

        let staging = storageDirectory.appendingPathComponent(".\(asset.fileName).installing")
        if fileManager.fileExists(atPath: staging.path) {
            try fileManager.removeItem(at: staging)
        }

        if copiesSource {
            try fileManager.copyItem(at: sourceURL, to: staging)
        } else {
            try fileManager.moveItem(at: sourceURL, to: staging)
        }

        if fileManager.fileExists(atPath: destination.path) {
            _ = try fileManager.replaceItemAt(destination, withItemAt: staging)
        } else {
            try fileManager.moveItem(at: staging, to: destination)
        }
    }
}

private extension ModelStore {
    enum StoreError: LocalizedError {
        case invalidFile(String)
        case missingFile(String)

        var errorDescription: String? {
            switch self {
            case .invalidFile(let fileName):
                return "\(fileName) is incomplete or is not the expected Kokoro file."
            case .missingFile(let fileName):
                return "The selected folder does not contain \(fileName)."
            }
        }
    }
}

private final class AssetDownloader: NSObject, URLSessionDownloadDelegate {
    typealias ProgressHandler = (Int64) -> Void

    private let progressHandler: ProgressHandler
    private var continuation: CheckedContinuation<URL, Error>?
    private var downloadedFileURL: URL?
    private var downloadError: Error?
    private var session: URLSession?

    init(progressHandler: @escaping ProgressHandler) {
        self.progressHandler = progressHandler
    }

    func download(from url: URL) async throws -> URL {
        try await withCheckedThrowingContinuation { continuation in
            self.continuation = continuation

            let configuration = URLSessionConfiguration.ephemeral
            configuration.timeoutIntervalForRequest = 60
            configuration.timeoutIntervalForResource = 60 * 30

            let delegateQueue = OperationQueue()
            delegateQueue.maxConcurrentOperationCount = 1

            let session = URLSession(
                configuration: configuration,
                delegate: self,
                delegateQueue: delegateQueue
            )
            self.session = session
            session.downloadTask(with: url).resume()
        }
    }

    func urlSession(
        _ session: URLSession,
        downloadTask: URLSessionDownloadTask,
        didWriteData bytesWritten: Int64,
        totalBytesWritten: Int64,
        totalBytesExpectedToWrite: Int64
    ) {
        progressHandler(totalBytesWritten)
    }

    func urlSession(
        _ session: URLSession,
        downloadTask: URLSessionDownloadTask,
        didFinishDownloadingTo location: URL
    ) {
        let temporaryURL = FileManager.default.temporaryDirectory
            .appendingPathComponent(UUID().uuidString)
            .appendingPathExtension("download")

        do {
            try FileManager.default.moveItem(at: location, to: temporaryURL)
            downloadedFileURL = temporaryURL
        } catch {
            downloadError = error
        }
    }

    func urlSession(
        _ session: URLSession,
        task: URLSessionTask,
        didCompleteWithError error: Error?
    ) {
        defer {
            continuation = nil
            self.session?.finishTasksAndInvalidate()
            self.session = nil
        }

        if let error = error ?? downloadError {
            continuation?.resume(throwing: error)
        } else if let downloadedFileURL {
            continuation?.resume(returning: downloadedFileURL)
        } else {
            continuation?.resume(throwing: URLError(.cannotCreateFile))
        }
    }
}
