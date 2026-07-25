import Foundation

struct SpeechSettings {
    var voice: KokoroVoice
    var speed: Float
    var paragraphPause: Double
    var normalizesAudio: Bool
    var trimsSilence: Bool
}

struct SpeechSynthesisResult {
    let samples: [Float]
    let sampleRate: Double

    var duration: TimeInterval {
        guard sampleRate > 0 else { return 0 }
        return Double(samples.count) / sampleRate
    }
}

struct SpeechChunk: Equatable {
    let text: String
    let pauseAfter: TimeInterval
}

