import AppKit
import Combine
import Foundation
import KokoroSwift
import UniformTypeIdentifiers

@MainActor
final class AppModel: ObservableObject {
    enum WorkState: Equatable {
        case idle
        case generating(Double)
        case ready(TimeInterval)
        case exporting
        case failed(String)
    }

    @Published var text = ""
    @Published var selectedVoiceID: String {
        didSet { defaults.set(selectedVoiceID, forKey: Keys.voice) }
    }
    @Published var speed: Double {
        didSet { defaults.set(speed, forKey: Keys.speed) }
    }
    @Published var paragraphPause: Double {
        didSet { defaults.set(paragraphPause, forKey: Keys.paragraphPause) }
    }
    @Published var normalizesAudio: Bool {
        didSet { defaults.set(normalizesAudio, forKey: Keys.normalize) }
    }
    @Published var trimsSilence: Bool {
        didSet { defaults.set(trimsSilence, forKey: Keys.trimSilence) }
    }
    @Published var autoPlays: Bool {
        didSet { defaults.set(autoPlays, forKey: Keys.autoPlay) }
    }
    @Published private(set) var workState: WorkState = .idle
    @Published private(set) var waveform: [Float] = []

    let modelStore: ModelStore
    let player: AudioPlayerController

    private let speechEngine = KokoroSpeechEngine()
    private let defaults: UserDefaults
    private var audioSamples: [Float] = []
    private var audioSampleRate: Double = 24_000
    private var activeGenerationID = UUID()
    private var modelStoreObservation: AnyCancellable?

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        modelStore = ModelStore()
        player = AudioPlayerController()

        let storedVoice = defaults.string(forKey: Keys.voice) ?? KokoroVoice.curated[0].id
        selectedVoiceID = KokoroVoice.curated.contains(where: { $0.id == storedVoice })
            ? storedVoice
            : KokoroVoice.curated[0].id

        let storedSpeed = defaults.object(forKey: Keys.speed) as? Double
        speed = min(max(storedSpeed ?? 1, 0.72), 1.28)

        let storedPause = defaults.object(forKey: Keys.paragraphPause) as? Double
        paragraphPause = min(max(storedPause ?? 0.42, 0.15), 0.9)

        normalizesAudio = defaults.object(forKey: Keys.normalize) as? Bool ?? true
        trimsSilence = defaults.object(forKey: Keys.trimSilence) as? Bool ?? true
        autoPlays = defaults.object(forKey: Keys.autoPlay) as? Bool ?? true

        modelStoreObservation = modelStore.objectWillChange.sink { [weak self] _ in
            self?.objectWillChange.send()
        }
    }

    var selectedVoice: KokoroVoice {
        KokoroVoice.voice(withID: selectedVoiceID)
    }

    var wordCount: Int {
        text.split(whereSeparator: \.isWhitespace).count
    }

    var characterCount: Int {
        text.count
    }

    var estimatedDuration: TimeInterval {
        guard wordCount > 0 else { return 0 }
        let speakingMinutes = Double(wordCount) / (155 * speed)
        let paragraphCount = text
            .components(separatedBy: .newlines)
            .filter { !$0.trimmingCharacters(in: .whitespaces).isEmpty }
            .count
        return speakingMinutes * 60 + Double(max(0, paragraphCount - 1)) * paragraphPause
    }

    var isGenerating: Bool {
        if case .generating = workState { return true }
        return false
    }

    var canGenerate: Bool {
        modelStore.isReady
            && !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            && !isGenerating
    }

    var statusText: String {
        switch workState {
        case .idle:
            return modelStore.isReady ? "Ready when you are" : "Install the local voice model to begin"
        case .generating(let progress):
            return "Shaping speech · \(Int(progress * 100))%"
        case .ready(let duration):
            return "Generated \(duration.formattedClock) of local audio"
        case .exporting:
            return "Saving WAV…"
        case .failed(let message):
            return message
        }
    }

    func pasteFromClipboard() {
        guard let clipboardText = NSPasteboard.general.string(forType: .string) else { return }
        text = clipboardText
    }

    func clearText() {
        text = ""
    }

    func generateSpeech() {
        guard canGenerate else { return }

        let generationID = UUID()
        activeGenerationID = generationID
        workState = .generating(0)

        let settings = SpeechSettings(
            voice: selectedVoice,
            speed: Float(speed),
            paragraphPause: paragraphPause,
            normalizesAudio: normalizesAudio,
            trimsSilence: trimsSilence
        )
        let sourceText = text
        let modelURL = modelStore.modelURL
        let voiceURL = modelStore.voiceURL(for: selectedVoice)

        Task {
            do {
                let result = try await self.speechEngine.synthesize(
                    text: sourceText,
                    settings: settings,
                    modelURL: modelURL,
                    voiceURL: voiceURL
                ) { progress in
                    DispatchQueue.main.async {
                        guard self.activeGenerationID == generationID else { return }
                        self.workState = .generating(progress)
                    }
                }

                guard self.activeGenerationID == generationID else { return }
                self.audioSamples = result.samples
                self.audioSampleRate = result.sampleRate
                self.waveform = AudioProcessor.waveform(from: result.samples)
                self.player.load(samples: result.samples, sampleRate: result.sampleRate)
                self.workState = .ready(result.duration)

                if self.autoPlays {
                    self.player.playPause()
                }
            } catch {
                guard self.activeGenerationID == generationID else { return }
                self.workState = .failed(self.friendlyError(error))
            }
        }
    }

    func playPauseOrGenerate() {
        if player.state == .empty {
            generateSpeech()
        } else {
            player.playPause()
        }
    }

    func exportWAV() {
        guard !audioSamples.isEmpty else { return }

        let panel = NSSavePanel()
        panel.title = "Export generated speech"
        panel.nameFieldLabel = "Save as:"
        panel.nameFieldStringValue = suggestedFileName()
        panel.allowedContentTypes = [.wav]
        panel.canCreateDirectories = true

        panel.begin { [weak self] response in
            guard let self, response == .OK, let url = panel.url else { return }
            self.workState = .exporting

            let samples = self.audioSamples
            let sampleRate = self.audioSampleRate

            Task.detached(priority: .userInitiated) {
                do {
                    try WAVEncoder.write(samples: samples, sampleRate: sampleRate, to: url)
                    await MainActor.run {
                        self.workState = .ready(Double(samples.count) / sampleRate)
                    }
                } catch {
                    await MainActor.run {
                        self.workState = .failed("WAV export failed: \(error.localizedDescription)")
                    }
                }
            }
        }
    }

    func chooseExistingModelFolder() {
        let panel = NSOpenPanel()
        panel.title = "Choose a Kokoro model folder"
        panel.message = "Select a folder containing kokoro-v1_0.safetensors and the five Echolocal voice files."
        panel.prompt = "Use Folder"
        panel.canChooseFiles = false
        panel.canChooseDirectories = true
        panel.allowsMultipleSelection = false

        panel.begin { [weak self] response in
            guard let self, response == .OK, let url = panel.url else { return }
            do {
                try self.modelStore.installExisting(from: url)
            } catch {
                self.workState = .failed(error.localizedDescription)
            }
        }
    }

    private func friendlyError(_ error: Error) -> String {
        if let kokoroError = error as? KokoroTTS.KokoroTTSError {
            switch kokoroError {
            case .tooManyTokens:
                return "This passage is too dense for one speech segment. Add a sentence break and try again."
            @unknown default:
                break
            }
        }
        return "Speech generation failed: \(error.localizedDescription)"
    }

    private func suggestedFileName() -> String {
        let words = text
            .split(whereSeparator: \.isWhitespace)
            .prefix(5)
            .map(String.init)
            .joined(separator: " ")

        let safeName = words
            .components(separatedBy: CharacterSet.alphanumerics.inverted)
            .filter { !$0.isEmpty }
            .joined(separator: "-")
            .lowercased()

        return (safeName.isEmpty ? "echolocal" : safeName) + ".wav"
    }

    private enum Keys {
        static let voice = "speech.voice"
        static let speed = "speech.speed"
        static let paragraphPause = "speech.paragraphPause"
        static let normalize = "speech.normalize"
        static let trimSilence = "speech.trimSilence"
        static let autoPlay = "speech.autoPlay"
    }
}

extension TimeInterval {
    var formattedClock: String {
        guard isFinite, self >= 0 else { return "0:00" }
        let totalSeconds = Int(self.rounded())
        let hours = totalSeconds / 3600
        let minutes = (totalSeconds % 3600) / 60
        let seconds = totalSeconds % 60

        if hours > 0 {
            return String(format: "%d:%02d:%02d", hours, minutes, seconds)
        }
        return String(format: "%d:%02d", minutes, seconds)
    }
}
