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
}
