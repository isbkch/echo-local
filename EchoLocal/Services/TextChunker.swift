import Foundation
import NaturalLanguage

enum TextChunker {
    static func chunks(
        from source: String,
        maximumCharacters: Int = 700,
        paragraphPause: TimeInterval
    ) -> [SpeechChunk] {
        let boundedMaximumCharacters = max(1, maximumCharacters)
        let paragraphs = source
            .components(separatedBy: .newlines)
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }

        var output: [SpeechChunk] = []

        for paragraph in paragraphs {
            let pieces = sentencePieces(
                in: paragraph,
                maximumCharacters: boundedMaximumCharacters
            )

            for (index, piece) in pieces.enumerated() {
                output.append(
                    SpeechChunk(
                        text: piece,
                        pauseAfter: index == pieces.count - 1 ? paragraphPause : 0.12
                    )
                )
            }
        }

        if !output.isEmpty {
            output[output.count - 1] = SpeechChunk(text: output[output.count - 1].text, pauseAfter: 0)
        }

        return output
    }

    static func refinedChunks(for chunk: SpeechChunk) -> [SpeechChunk] {
        guard chunk.text.count > 1 else { return [chunk] }

        let pieces = sentencePieces(
            in: chunk.text,
            maximumCharacters: max(1, chunk.text.count / 2)
        )
        guard pieces.count > 1 else { return [chunk] }

        return pieces.enumerated().map { index, piece in
            SpeechChunk(
                text: piece,
                pauseAfter: index == pieces.count - 1 ? chunk.pauseAfter : 0.12
            )
        }
    }

    private static func sentencePieces(in paragraph: String, maximumCharacters: Int) -> [String] {
        let tokenizer = NLTokenizer(unit: .sentence)
        tokenizer.string = paragraph

        var sentences: [String] = []
        tokenizer.enumerateTokens(in: paragraph.startIndex..<paragraph.endIndex) { range, _ in
            let sentence = paragraph[range].trimmingCharacters(in: .whitespacesAndNewlines)
            if !sentence.isEmpty {
                sentences.append(sentence)
            }
            return true
        }

        if sentences.isEmpty {
            sentences = [paragraph]
        }

        var pieces: [String] = []
        var current = ""

        for sentence in sentences.flatMap({ splitOversized($0, maximumCharacters: maximumCharacters) }) {
            let candidate = current.isEmpty ? sentence : "\(current) \(sentence)"
            if candidate.count <= maximumCharacters {
                current = candidate
            } else {
                if !current.isEmpty {
                    pieces.append(current)
                }
                current = sentence
            }
        }

        if !current.isEmpty {
            pieces.append(current)
        }

        return pieces
    }

    private static func splitOversized(_ text: String, maximumCharacters: Int) -> [String] {
        guard text.count > maximumCharacters else { return [text] }

        var pieces: [String] = []
        var current = ""

        for oversizedWord in text.split(whereSeparator: \.isWhitespace) {
            for word in splitWord(String(oversizedWord), maximumCharacters: maximumCharacters) {
                let candidate = current.isEmpty ? word : "\(current) \(word)"
                if candidate.count <= maximumCharacters {
                    current = candidate
                } else {
                    if !current.isEmpty {
                        pieces.append(current)
                    }
                    current = word
                }
            }
        }

        if !current.isEmpty {
            pieces.append(current)
        }

        return pieces
    }

    private static func splitWord(_ word: String, maximumCharacters: Int) -> [String] {
        guard word.count > maximumCharacters else { return [word] }

        var pieces: [String] = []
        var start = word.startIndex

        while start < word.endIndex {
            let end = word.index(
                start,
                offsetBy: maximumCharacters,
                limitedBy: word.endIndex
            ) ?? word.endIndex
            pieces.append(String(word[start..<end]))
            start = end
        }

        return pieces
    }
}
