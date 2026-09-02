import CryptoKit
import Foundation
#if os(macOS)
import AppKit
#endif

@MainActor
final class ModelStore: ObservableObject {
    struct RemoteAsset: Hashable {
        let fileName: String
        let expectedBytes: Int64
        let sha256: String

        var sourceURL: URL {
            URL(
                string: "https://huggingface.co/brannala64/kokoro-82m-safetensors/resolve/\(ModelStore.sourceRevision)/\(fileName)"
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

    static let sourceRevision = "fb1f073314bb48cda1f1e371d0d97653cbaf9680"

    static let modelAsset = RemoteAsset(
        fileName: "kokoro-v1_0.safetensors",
        expectedBytes: 327_115_152,
        sha256: "4e9ecdf03b8b6cf906070390237feda473dc13327cb8d56a43deaa374c02acd8"
    )

    static let voiceAssets = [
        RemoteAsset(
            fileName: "af_heart.safetensors",
            expectedBytes: 522_339,
            sha256: "4e40b08984cd84a86b4d07960939bd85bb6b3747dd747b7de48dca3aaeab37ca"
        ),
        RemoteAsset(
            fileName: "af_bella.safetensors",
            expectedBytes: 522_339,
            sha256: "a18024b9332f5ff217c7f604cbe94449a3ca51c3b8d85500e31cd3cbdc4ef6ce"
        ),
        RemoteAsset(
            fileName: "am_michael.safetensors",
            expectedBytes: 522_339,
            sha256: "19a8661430456e2bbf0a68b52fa9b49678bb0fb7418619f868df77921f5aa43c"
        ),
        RemoteAsset(
            fileName: "bf_emma.safetensors",
            expectedBytes: 522_339,
            sha256: "ed92055e1ed96f2a0b4a52b76956dcfd76627bd548c33801743a62c0817dec01"
        ),
        RemoteAsset(
            fileName: "bm_george.safetensors",
            expectedBytes: 522_339,
            sha256: "a3a6682cde622e7aee35597b91947fa018f78be101a97587a100effe227e5a21"
        ),
    ]

    static let allAssets = [modelAsset] + voiceAssets

    @Published private(set) var state: State = .checking

    private var downloadTask: Task<Void, Never>?
    private let assets: [RemoteAsset]
    private let storageDirectoryOverride: URL?

    var storageDirectory: URL {
        if let storageDirectoryOverride {
            return storageDirectoryOverride
        }
        return applicationSupportDirectory
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
        assets.reduce(0) { $0 + $1.expectedBytes }
    }

    var isReady: Bool {
        state == .ready
    }

    init(storageDirectory: URL? = nil, assets: [RemoteAsset]? = nil) {
        storageDirectoryOverride = storageDirectory
        self.assets = assets ?? Self.allAssets
        if storageDirectory == nil {
            migrateLegacyModelIfNeeded()
        }
        refresh()
    }

    func voiceURL(for voice: KokoroVoice) -> URL {
        storageDirectory.appendingPathComponent(voice.fileName)
    }

    func refresh() {
        state = assets.allSatisfy(isAssetValid) ? .ready : .missing
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

                var completedBytes = assets
                    .filter(isAssetValid)
                    .reduce(Int64.zero) { $0 + $1.expectedBytes }

                for asset in assets where !isAssetValid(asset) {
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
                    defer { try? FileManager.default.removeItem(at: temporaryURL) }
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

        for asset in assets {
            let source = sourceDirectory.appendingPathComponent(asset.fileName)
            guard FileManager.default.fileExists(atPath: source.path) else {
                throw StoreError.missingFile(asset.fileName)
            }
            try validate(fileAt: source, as: asset)
        }

        try prepareStorageDirectory()

        for asset in assets {
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
        guard Int64(size) == asset.expectedBytes else { return false }
        return (try? sha256(fileAt: url)) == asset.sha256
    }

    private func validate(fileAt url: URL, as asset: RemoteAsset) throws {
        let values = try url.resourceValues(forKeys: [.fileSizeKey])
        guard
            Int64(values.fileSize ?? 0) == asset.expectedBytes,
            try sha256(fileAt: url) == asset.sha256
        else {
            throw StoreError.invalidFile(asset.fileName)
        }
    }

    private func sha256(fileAt url: URL) throws -> String {
        let handle = try FileHandle(forReadingFrom: url)
        defer { try? handle.close() }

        var hasher = SHA256()
        while let data = try handle.read(upToCount: 1024 * 1024), !data.isEmpty {
            hasher.update(data: data)
        }
        return hasher.finalize().map { String(format: "%02x", $0) }.joined()
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
