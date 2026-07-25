import Foundation
import KokoroSwift
import MLX

final class KokoroSpeechEngine: @unchecked Sendable {
    enum EngineError: LocalizedError {
        case emptyText
        case missingModel
        case missingVoice
        case unavailableVoiceData

        var errorDescription: String? {
            switch self {
            case .emptyText:
                return "Paste or type something before generating speech."
            case .missingModel:
                return "The local Kokoro model is not installed."
            case .missingVoice:
                return "The selected voice file is not installed."
            case .unavailableVoiceData:
                return "The selected voice file could not be read."
            }
        }
    }

    private let synthesisQueue = DispatchQueue(
        label: "com.isbkch.EchoLocal.synthesis",
        qos: .userInitiated
    )

    private var engine: KokoroTTS?
    private var loadedModelURL: URL?
    private var voiceCache: [String: MLXArray] = [:]

    func synthesize(
        text: String,
        settings: SpeechSettings,
        modelURL: URL,
        voiceURL: URL,
        progress: @escaping (Double) -> Void
    ) async throws -> SpeechSynthesisResult {
        try await withCheckedThrowingContinuation { continuation in
            synthesisQueue.async { [weak self] in
                guard let self else {
                    continuation.resume(throwing: CancellationError())
                    return
                }

                do {
                    let cleanedText = text.trimmingCharacters(in: .whitespacesAndNewlines)
                    guard !cleanedText.isEmpty else {
                        throw EngineError.emptyText
                    }
                    guard FileManager.default.fileExists(atPath: modelURL.path) else {
                        throw EngineError.missingModel
                    }
                    guard FileManager.default.fileExists(atPath: voiceURL.path) else {
                        throw EngineError.missingVoice
                    }

                    progress(0.02)

                    if engine == nil || loadedModelURL != modelURL {
                        engine = KokoroTTS(modelPath: modelURL)
                        loadedModelURL = modelURL
                        voiceCache.removeAll()
                    }

                    let voice: MLXArray
                    if let cached = voiceCache[settings.voice.id] {
                        voice = cached
                    } else {
                        let arrays = try MLX.loadArrays(url: voiceURL)
                        guard let loadedVoice = arrays["voice"] else {
                            throw EngineError.unavailableVoiceData
                        }
                        voiceCache[settings.voice.id] = loadedVoice
                        voice = loadedVoice
                    }

                    let sampleRate = Double(KokoroTTS.Constants.samplingRate)
                    let chunks = TextChunker.chunks(
                        from: cleanedText,
                        paragraphPause: settings.paragraphPause
                    )

                    guard !chunks.isEmpty else {
                        throw EngineError.emptyText
                    }

                    var combined: [Float] = []
                    combined.reserveCapacity(Int(Double(cleanedText.count) * sampleRate / 14))

                    for (index, chunk) in chunks.enumerated() {
                        let language: Language = settings.voice.isAmericanEnglish ? .enUS : .enGB
                        let (rawSamples, _) = try engine!.generateAudio(
                            voice: voice,
                            language: language,
                            text: chunk.text,
                            speed: settings.speed
                        )

                        let finishedChunk = AudioProcessor.finish(
                            rawSamples,
                            sampleRate: sampleRate,
                            normalize: false,
                            trimSilence: settings.trimsSilence
                        )
                        combined.append(contentsOf: finishedChunk)
                        combined.append(
                            contentsOf: AudioProcessor.silence(
                                duration: chunk.pauseAfter,
                                sampleRate: sampleRate
                            )
                        )

                        progress(Double(index + 1) / Double(chunks.count))
                    }

                    let finished = AudioProcessor.finish(
                        combined,
                        sampleRate: sampleRate,
                        normalize: settings.normalizesAudio,
                        trimSilence: false
                    )

                    continuation.resume(
                        returning: SpeechSynthesisResult(samples: finished, sampleRate: sampleRate)
                    )
                } catch {
                    continuation.resume(throwing: error)
                }
            }
        }
    }
}
