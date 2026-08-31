import XCTest
@testable import EchoLocal

final class TextChunkerTests: XCTestCase {
    private enum SimulatedSynthesisError: Error {
        case tooDense
    }

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

    func testOversizedSingleWordIsStillBounded() {
        let source = String(repeating: "a", count: 401)
        let chunks = TextChunker.chunks(
            from: source,
            maximumCharacters: 100,
            paragraphPause: 0.4
        )

        XCTAssertEqual(chunks.map(\.text).joined(), source)
        XCTAssertTrue(chunks.allSatisfy { $0.text.count <= 100 })
    }

    func testRejectedChunksAreRefinedUntilTheSynthesizerAcceptsThem() throws {
        let source = """
        Assumptions and the actual state of runtime processes need a rollback path. We need to test that before moving forward. We have had a deliberate bounding on the tests because without bounds these experiments waste time and money.

        But here is the part I want you to sit with, because it is the honest and slightly uncomfortable ending. That experiment has never run. As I write this, the engines are still fully built and still unauthorized. The decision record is still open, production still points at Pub/Sub, and do not switch without an experiment is still a live line in my task list rather than a closed item. A few years ago, fully built and never authorized would have described wasted effort. Today it describes a control system working as designed. The capability sits behind a gate it has not earned its way through, and that is the correct state for it.
        """
        let initialChunks = TextChunker.chunks(
            from: source,
            maximumCharacters: 700,
            paragraphPause: 0.42
        )
        var acceptedChunks: [SpeechChunk] = []
        var progressValues: [Double] = []

        XCTAssertTrue(initialChunks.contains { $0.text.count > 180 })

        try KokoroSpeechEngine.processChunksAdaptively(
            initialChunks,
            process: { chunk in
                guard chunk.text.count <= 180 else {
                    throw SimulatedSynthesisError.tooDense
                }
                acceptedChunks.append(chunk)
            },
            shouldSplit: { $0 is SimulatedSynthesisError },
            progress: { progressValues.append($0) }
        )

        XCTAssertGreaterThan(acceptedChunks.count, initialChunks.count)
        XCTAssertTrue(acceptedChunks.allSatisfy { $0.text.count <= 180 })
        XCTAssertEqual(
            acceptedChunks.flatMap { $0.text.split(whereSeparator: \.isWhitespace) },
            source.split(whereSeparator: \.isWhitespace)
        )
        XCTAssertEqual(acceptedChunks.last?.pauseAfter, 0)
        XCTAssertEqual(progressValues.last, 1)
        XCTAssertTrue(
            zip(progressValues, progressValues.dropFirst()).allSatisfy { earlier, later in
                earlier <= later
            },
            "Progress must not move backwards when a rejected chunk is split again."
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
