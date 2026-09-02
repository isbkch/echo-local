import CryptoKit
import Foundation
import XCTest
@testable import EchoLocal

@MainActor
final class ModelStoreTests: XCTestCase {
    func testProductionManifestPinsImmutableIntegrityCheckedAssets() {
        let expectedVoiceFiles = Set(KokoroVoice.curated.map(\.fileName))

        XCTAssertEqual(Set(ModelStore.voiceAssets.map(\.fileName)), expectedVoiceFiles)
        XCTAssertEqual(ModelStore.allAssets.count, 6)
        for asset in ModelStore.allAssets {
            XCTAssertTrue(
                asset.sourceURL.absoluteString.contains("/resolve/\(ModelStore.sourceRevision)/"),
                "Model downloads must use the reviewed immutable revision."
            )
            XCTAssertFalse(asset.sourceURL.absoluteString.contains("/resolve/main/"))
            XCTAssertEqual(asset.sha256.count, 64)
            XCTAssertTrue(asset.sha256.allSatisfy { $0.isHexDigit })
        }
    }

    func testSameSizeCorruptionIsNotTreatedAsReady() throws {
        try withTemporaryDirectories { installed, _ in
            let expected = Data("right".utf8)
            let asset = testAsset(fileName: "model.bin", payload: expected)
            try Data("wrong".utf8).write(to: installed.appendingPathComponent(asset.fileName))

            let store = ModelStore(storageDirectory: installed, assets: [asset])

            XCTAssertEqual(store.state, .missing)
            XCTAssertFalse(store.isReady)
        }
    }

    func testCorruptImportCannotReplaceLastKnownGoodModel() throws {
        try withTemporaryDirectories { installed, source in
            let expected = Data("right".utf8)
            let asset = testAsset(fileName: "model.bin", payload: expected)
            let installedFile = installed.appendingPathComponent(asset.fileName)
            try expected.write(to: installedFile)
            try Data("wrong".utf8).write(to: source.appendingPathComponent(asset.fileName))
            let store = ModelStore(storageDirectory: installed, assets: [asset])
            XCTAssertEqual(store.state, .ready)

            XCTAssertThrowsError(try store.installExisting(from: source))

            XCTAssertEqual(try Data(contentsOf: installedFile), expected)
            XCTAssertEqual(store.state, .ready)
        }
    }

    func testVerifiedImportBecomesReady() throws {
        try withTemporaryDirectories { installed, source in
            let payload = Data("verified-model".utf8)
            let asset = testAsset(fileName: "model.bin", payload: payload)
            try payload.write(to: source.appendingPathComponent(asset.fileName))
            let store = ModelStore(storageDirectory: installed, assets: [asset])
            XCTAssertEqual(store.state, .missing)

            try store.installExisting(from: source)

            XCTAssertEqual(store.state, .ready)
            XCTAssertEqual(
                try Data(contentsOf: installed.appendingPathComponent(asset.fileName)),
                payload
            )
        }
    }

    private func testAsset(fileName: String, payload: Data) -> ModelStore.RemoteAsset {
        ModelStore.RemoteAsset(
            fileName: fileName,
            expectedBytes: Int64(payload.count),
            sha256: SHA256.hash(data: payload).map { String(format: "%02x", $0) }.joined()
        )
    }

    private func withTemporaryDirectories(
        _ body: (URL, URL) throws -> Void
    ) throws {
        let root = FileManager.default.temporaryDirectory
            .appendingPathComponent("echolocal-model-store-tests-\(UUID().uuidString)", isDirectory: true)
        let installed = root.appendingPathComponent("installed", isDirectory: true)
        let source = root.appendingPathComponent("source", isDirectory: true)
        try FileManager.default.createDirectory(at: installed, withIntermediateDirectories: true)
        try FileManager.default.createDirectory(at: source, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: root) }
        try body(installed, source)
    }
}
