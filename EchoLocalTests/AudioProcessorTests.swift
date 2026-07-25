import XCTest
@testable import EchoLocal

final class AudioProcessorTests: XCTestCase {
    func testNormalizationTargetsMinusOneDecibel() {
        let input: [Float] = [0, 0.1, -0.2, 0.05, 0]
        let output = AudioProcessor.finish(
            input,
            sampleRate: 100,
            normalize: true,
            trimSilence: false
        )

        let peak = output.map(abs).max() ?? 0
        XCTAssertEqual(peak, 0.891, accuracy: 0.002)
    }

    func testSilenceTrimmingKeepsSmallPadding() {
        let input = Array(repeating: Float.zero, count: 100)
            + [0.2, 0.4, 0.2]
            + Array(repeating: Float.zero, count: 100)

        let output = AudioProcessor.finish(
            input,
            sampleRate: 100,
            normalize: false,
            trimSilence: true
        )

        XCTAssertLessThan(output.count, input.count)
        XCTAssertGreaterThan(output.count, 3)
    }

    func testWaveformProducesABoundedEnvelope() {
        let input = (0..<1_000).map { Float(sin(Double($0) / 15)) }
        let envelope = AudioProcessor.waveform(from: input, bucketCount: 100)

        XCTAssertEqual(envelope.count, 100)
        XCTAssertTrue(envelope.allSatisfy { $0 >= 0 && $0 <= 1 })
    }
}

