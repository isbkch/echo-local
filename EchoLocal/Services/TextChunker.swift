import Foundation
import NaturalLanguage

enum TextChunker {
    static func chunks(
        from source: String,
        maximumCharacters: Int = 700,
        paragraphPause: TimeInterval
    ) -> [SpeechChunk] {
        let paragraphs = source
            .components(separatedBy: .newlines)
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }

        var output: [SpeechChunk] = []

        for paragraph in paragraphs {
            let pieces = sentencePieces(in: paragraph, maximumCharacters: maximumCharacters)

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

        for word in text.split(whereSeparator: \.isWhitespace) {
            let word = String(word)
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

        if !current.isEmpty {
            pieces.append(current)
        }

        return pieces
    }
}

