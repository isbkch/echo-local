import Foundation
import MLX
import XCTest
@testable import EchoLocal

final class KokoroInferenceSmokeTests: XCTestCase {
    #if !os(iOS) || !targetEnvironment(simulator)
    func testSynthesisReleasesModelAndCacheBeforeReturningAndCanGenerateAgain() async throws {
        let assets = try smokeAssets()
        let engine = KokoroSpeechEngine()
        Stream.gpu.synchronize()
        Memory.clearCache()
        let baseline = Memory.snapshot()

        for generation in 1...2 {
            var memoryDuringGeneration = baseline
            let result = try await engine.synthesize(
                text: "Hello, this is Echolocal.\n\nThe model can rest once the speech is ready.",
                settings: smokeSettings,
                modelURL: assets.model,
                voiceURL: assets.voice,
                progress: { progress in
                    if progress == 1 {
                        memoryDuringGeneration = Memory.snapshot()
                    }
                }
            )
            let after = Memory.snapshot()
            print("Generation \(generation) MLX memory during: \(memoryDuringGeneration), after: \(after)")

            XCTAssertLessThanOrEqual(
                after.activeMemory,
                baseline.activeMemory + 1024 * 1024,
                "A completed generation must release its model and voice even while the engine stays alive."
            )
            XCTAssertEqual(after.cacheMemory, 0, "Return reusable Metal buffers before reporting completion.")
            XCTAssertEqual(result.sampleRate, 24_000)
            XCTAssertGreaterThan(result.duration, 1)
            XCTAssertGreaterThan(result.samples.map(abs).max() ?? 0, 0.05)
        }
        withExtendedLifetime(engine) {}
    }

    func testSynthesisReleasesModelAndCacheAfterVoiceLoadingFails() async throws {
        let assets = try smokeAssets()
        let invalidVoice = FileManager.default.temporaryDirectory
            .appendingPathComponent("\(UUID().uuidString).safetensors")
        defer { try? FileManager.default.removeItem(at: invalidVoice) }
        try MLX.save(arrays: ["missing_voice_key": MLXArray([Float(0)])], url: invalidVoice)

        let engine = KokoroSpeechEngine()
        Stream.gpu.synchronize()
        Memory.clearCache()
        let baseline = Memory.snapshot()
        let previousAudio = try await engine.synthesize(
            text: "Keep this audio available if the next generation fails.",
            settings: smokeSettings,
            modelURL: assets.model,
            voiceURL: assets.voice,
            progress: { _ in }
        )
        var failedSettings = smokeSettings
        failedSettings.voice = KokoroVoice.voice(withID: "bf_emma")
        do {
            _ = try await engine.synthesize(
                text: "This voice file is missing its embedding.",
                settings: failedSettings,
                modelURL: assets.model,
                voiceURL: invalidVoice,
                progress: { _ in }
            )
            XCTFail("A voice file without its embedding must fail after loading the model.")
        } catch let error as KokoroSpeechEngine.EngineError {
            guard case .unavailableVoiceData = error else {
                return XCTFail("Expected unavailable voice data, got \(error).")
            }
        }
        let after = Memory.snapshot()
        print("Failed generation MLX memory after: \(after)")
        XCTAssertLessThanOrEqual(after.activeMemory, baseline.activeMemory + 1024 * 1024)
        XCTAssertEqual(after.cacheMemory, 0, "Failed generations must also return cached Metal buffers.")
        XCTAssertGreaterThan(previousAudio.duration, 1)
        XCTAssertGreaterThan(previousAudio.samples.map(abs).max() ?? 0, 0.05)
        withExtendedLifetime(engine) {}
    }

    private var smokeSettings: SpeechSettings {
        SpeechSettings(
            voice: KokoroVoice.voice(withID: "af_heart"),
            speed: 1,
            paragraphPause: 0.4,
            normalizesAudio: false,
            trimsSilence: true
        )
    }

    private func smokeAssets() throws -> (model: URL, voice: URL) {
        let directory = ProcessInfo.processInfo.environment["LOCAL_AUDIO_SMOKE_MODEL_DIR"]
            .map { URL(fileURLWithPath: $0, isDirectory: true) }
            ?? URL(fileURLWithPath: "/tmp/echo-local-smoke-model", isDirectory: true)
        let model = directory.appendingPathComponent("kokoro-v1_0.safetensors")
        let voice = directory.appendingPathComponent("af_heart.safetensors")
        guard
            FileManager.default.fileExists(atPath: model.path),
            FileManager.default.fileExists(atPath: voice.path)
        else {
            throw XCTSkip("Place Kokoro weights and af_heart in \(directory.path) to test memory release.")
        }
        return (model, voice)
    }
    #endif

    #if os(iOS) && targetEnvironment(simulator)
    func testSynthesisRejectsIOSSimulatorBeforeInitializingMLX() async {
        do {
            _ = try await KokoroSpeechEngine().synthesize(
                text: "This must not reach MLX on the iOS Simulator.",
                settings: SpeechSettings(
                    voice: KokoroVoice.voice(withID: "af_heart"),
                    speed: 1,
                    paragraphPause: 0.4,
                    normalizesAudio: false,
                    trimsSilence: true
                ),
                modelURL: URL(fileURLWithPath: "/model-must-not-be-read.safetensors"),
                voiceURL: URL(fileURLWithPath: "/voice-must-not-be-read.safetensors"),
                progress: { _ in }
            )
            XCTFail("iOS Simulator synthesis should be rejected before MLX initialization.")
        } catch let error as KokoroSpeechEngine.EngineError {
            guard case .unsupportedIOSSimulator = error else {
                return XCTFail("Expected the simulator preflight error, got \(error).")
            }
            XCTAssertEqual(
                error.errorDescription,
                "Use a physical iPhone to generate speech."
            )
        } catch {
            XCTFail("Expected the simulator preflight error, got \(error).")
        }
    }
    #endif

    func testKokoroProducesBoundedLongFormAudioWhenSmokeModelIsAvailable() async throws {
        let fallbackDirectory = URL(
            fileURLWithPath: "/tmp/echo-local-smoke-model",
            isDirectory: true
        )
        let modelDirectory = ProcessInfo.processInfo.environment["LOCAL_AUDIO_SMOKE_MODEL_DIR"]
            .map { URL(fileURLWithPath: $0, isDirectory: true) }
            ?? fallbackDirectory

        let modelURL = modelDirectory.appendingPathComponent("kokoro-v1_0.safetensors")
        let voiceURL = modelDirectory.appendingPathComponent("af_heart.safetensors")

        guard
            FileManager.default.fileExists(atPath: modelURL.path),
            FileManager.default.fileExists(atPath: voiceURL.path)
        else {
            throw XCTSkip(
                "Place the Kokoro weights and af_heart voice in \(modelDirectory.path) to run inference."
            )
        }

        let result = try await KokoroSpeechEngine().synthesize(
            text: """
            Biotech labs, hospital storage, and clean rooms rely on thousands of sensors \
            across hundreds of facilities. Those readings flow through a local system into \
            a data platform where teams make careful compliance decisions. Clear, dependable \
            speech matters because every sentence should remain easy to understand from \
            beginning to end, without static, tones, or missing sections.
            """,
            settings: SpeechSettings(
                voice: KokoroVoice.voice(withID: "af_heart"),
                speed: 1,
                paragraphPause: 0.4,
                normalizesAudio: false,
                trimsSilence: true
            ),
            modelURL: modelURL,
            voiceURL: voiceURL,
            progress: { _ in }
        )

        XCTAssertEqual(result.sampleRate, 24_000)
        XCTAssertGreaterThan(result.duration, 10)

        let peak = result.samples.map(abs).max() ?? 0
        XCTAssertGreaterThan(peak, 0.05)
        XCTAssertLessThan(
            peak,
            2,
            "Unbounded samples indicate corrupted MLX transposed-convolution output."
        )
    }

    func testKokoroRefinesARejectedLongFormSegmentWhenSmokeModelIsAvailable() async throws {
        let fallbackDirectory = URL(
            fileURLWithPath: "/tmp/echo-local-smoke-model",
            isDirectory: true
        )
        let modelDirectory = ProcessInfo.processInfo.environment["LOCAL_AUDIO_SMOKE_MODEL_DIR"]
            .map { URL(fileURLWithPath: $0, isDirectory: true) }
            ?? fallbackDirectory

        let modelURL = modelDirectory.appendingPathComponent("kokoro-v1_0.safetensors")
        let voiceURL = modelDirectory.appendingPathComponent("bm_george.safetensors")

        guard
            FileManager.default.fileExists(atPath: modelURL.path),
            FileManager.default.fileExists(atPath: voiceURL.path)
        else {
            throw XCTSkip(
                "Place the Kokoro weights and bm_george voice in \(modelDirectory.path) to run inference."
            )
        }

        let text = """
        Assumptions and the actual state of runtime processes need a rollback path. We need to test that before moving forward. We have had a deliberate bounding on the tests because without bounds these experiments waste time and money.

        But here is the part I want you to sit with, because it is the honest and slightly uncomfortable ending. That experiment has never run. As I write this, the engines are still fully built and still unauthorized. The decision record is still open, production still points at Pub/Sub, and do not switch without an experiment is still a live line in my task list rather than a closed item. A few years ago, fully built and never authorized would have described wasted effort. Today it describes a control system working as designed. The capability sits behind a gate it has not earned its way through, and that is the correct state for it.
        """
        let initialChunkCount = TextChunker.chunks(
            from: text,
            paragraphPause: 0.42
        ).count
        var progressValues: [Double] = []

        let result = try await KokoroSpeechEngine().synthesize(
            text: text,
            settings: SpeechSettings(
                voice: KokoroVoice.voice(withID: "bm_george"),
                speed: 1,
                paragraphPause: 0.42,
                normalizesAudio: false,
                trimsSilence: true
            ),
            modelURL: modelURL,
            voiceURL: voiceURL,
            progress: { progressValues.append($0) }
        )

        XCTAssertGreaterThan(result.duration, 30)
        XCTAssertEqual(progressValues.last, 1)
        XCTAssertGreaterThan(
            progressValues.count,
            initialChunkCount + 1,
            "The smoke passage should exercise token-limit refinement, not only initial chunking."
        )
    }
}
