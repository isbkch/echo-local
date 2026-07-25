import XCTest
@testable import EchoLocal

final class WAVEncoderTests: XCTestCase {
    func testWritesMonoSixteenBitPCMHeader() throws {
        let samples: [Float] = [0, 0.5, -0.5, 1, -1]
        let data = try WAVEncoder.data(samples: samples, sampleRate: 24_000)

        XCTAssertEqual(String(data: data[0..<4], encoding: .ascii), "RIFF")
        XCTAssertEqual(String(data: data[8..<12], encoding: .ascii), "WAVE")
        XCTAssertEqual(String(data: data[36..<40], encoding: .ascii), "data")
        XCTAssertEqual(data.count, 44 + samples.count * 2)
    }
}

