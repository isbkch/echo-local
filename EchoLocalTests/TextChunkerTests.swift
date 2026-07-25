import XCTest
@testable import EchoLocal

final class TextChunkerTests: XCTestCase {
    func testParagraphsReceiveConfiguredBreathExceptAtTheEnd() {
        let chunks = TextChunker.chunks(
            from: "First paragraph.\n\nSecond paragraph.",
            maximumCharacters: 700,
            paragraphPause: 0.55
        )

        XCTAssertEqual(chunks.count, 2)
        XCTAssertEqual(chunks[0].pauseAfter, 0.55, accuracy: 0.001)
        XCTAssertEqual(chunks[1].pauseAfter, 0, accuracy: 0.001)
    }

    func testOversizedPassageIsSplitWithoutDroppingWords() {
        let source = Array(repeating: "carefully", count: 220).joined(separator: " ")
        let chunks = TextChunker.chunks(
            from: source,
            maximumCharacters: 180,
            paragraphPause: 0.4
        )

        XCTAssertGreaterThan(chunks.count, 1)
        XCTAssertTrue(chunks.allSatisfy { $0.text.count <= 180 })
        XCTAssertEqual(
            chunks.flatMap { $0.text.split(separator: " ") }.count,
            220
        )
    }

    func testBlankTextProducesNoChunks() {
        XCTAssertTrue(
            TextChunker.chunks(
                from: " \n \n",
                maximumCharacters: 700,
                paragraphPause: 0.4
            ).isEmpty
        )
    }
}

