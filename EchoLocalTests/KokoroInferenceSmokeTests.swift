import Foundation
import XCTest
@testable import EchoLocal

final class KokoroInferenceSmokeTests: XCTestCase {
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
