import Foundation
import XCTest
@testable import EchoLocal

final class KokoroInferenceSmokeTests: XCTestCase {
    func testKokoroProducesAudibleSamplesWhenSmokeModelIsAvailable() async throws {
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
            text: "Local speech, generated entirely on this Mac.",
            settings: SpeechSettings(
                voice: KokoroVoice.voice(withID: "af_heart"),
                speed: 1,
                paragraphPause: 0.4,
                normalizesAudio: true,
                trimsSilence: true
            ),
            modelURL: modelURL,
            voiceURL: voiceURL,
            progress: { _ in }
        )

        XCTAssertEqual(result.sampleRate, 24_000)
        XCTAssertGreaterThan(result.duration, 1)
        XCTAssertGreaterThan(result.samples.map(abs).max() ?? 0, 0.05)
    }
}
