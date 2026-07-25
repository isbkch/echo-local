import AVFoundation
import Foundation

@MainActor
final class AudioPlayerController: ObservableObject {
    enum PlaybackState: Equatable {
        case empty
        case ready
        case playing
        case paused
    }

    @Published private(set) var state: PlaybackState = .empty
    @Published private(set) var currentTime: TimeInterval = 0
    @Published private(set) var duration: TimeInterval = 0
    @Published private(set) var errorMessage: String?
    @Published var volume: Double = 1 {
        didSet {
            playerNode.volume = Float(min(max(volume, 0), 1))
        }
    }

    private let audioEngine = AVAudioEngine()
    private let playerNode = AVAudioPlayerNode()
    private let timePitch = AVAudioUnitTimePitch()
    private var samples: [Float] = []
    private var sampleRate: Double = 24_000
    private var baseSample = 0
    private var progressTimer: Timer?
    private var playbackID = UUID()

    init() {
        audioEngine.attach(playerNode)
        audioEngine.attach(timePitch)
    }

    func load(samples: [Float], sampleRate: Double) {
        stop()
        self.samples = samples
        self.sampleRate = sampleRate
        duration = samples.isEmpty ? 0 : Double(samples.count) / sampleRate
        currentTime = 0
        baseSample = 0
        state = samples.isEmpty ? .empty : .ready
        errorMessage = nil
        reconnectGraph()
    }

    func playPause() {
        switch state {
        case .empty:
            return
        case .playing:
            updateProgress()
            playerNode.pause()
            state = .paused
            stopProgressTimer()
        case .paused:
            playerNode.play()
            state = .playing
            startProgressTimer()
        case .ready:
            if currentTime >= duration - 0.01 {
                baseSample = 0
                currentTime = 0
            }
            scheduleAndPlay(from: baseSample)
        }
    }

    func stop() {
        playbackID = UUID()
        playerNode.stop()
        stopProgressTimer()
        baseSample = 0
        currentTime = 0
        if !samples.isEmpty {
            state = .ready
        }
    }

    func seek(to time: TimeInterval) {
        guard !samples.isEmpty else { return }

        let wasPlaying = state == .playing
        playbackID = UUID()
        playerNode.stop()
        stopProgressTimer()

        let clampedTime = min(max(time, 0), duration)
        baseSample = min(samples.count, Int(clampedTime * sampleRate))
        currentTime = clampedTime
        state = .ready

        if wasPlaying, baseSample < samples.count {
            scheduleAndPlay(from: baseSample)
        }
    }

    private func reconnectGraph() {
        audioEngine.stop()
        audioEngine.disconnectNodeOutput(playerNode)
        audioEngine.disconnectNodeOutput(timePitch)

        guard
            let format = AVAudioFormat(
                standardFormatWithSampleRate: sampleRate,
                channels: 1
            )
        else {
            errorMessage = "The audio output format could not be created."
            return
        }

        audioEngine.connect(playerNode, to: timePitch, format: format)
        audioEngine.connect(timePitch, to: audioEngine.mainMixerNode, format: format)
        audioEngine.prepare()

        do {
            try audioEngine.start()
        } catch {
            errorMessage = "Audio playback could not start: \(error.localizedDescription)"
        }
    }

    private func scheduleAndPlay(from startSample: Int) {
        guard startSample < samples.count else {
            currentTime = duration
            state = .ready
            return
        }

        guard
            let format = AVAudioFormat(
                standardFormatWithSampleRate: sampleRate,
                channels: 1
            ),
            let buffer = AVAudioPCMBuffer(
                pcmFormat: format,
                frameCapacity: AVAudioFrameCount(samples.count - startSample)
            ),
            let channel = buffer.floatChannelData?[0]
        else {
            errorMessage = "The generated speech could not be prepared for playback."
            return
        }

        let remaining = samples.count - startSample
        buffer.frameLength = AVAudioFrameCount(remaining)
        samples.withUnsafeBufferPointer { source in
            guard let baseAddress = source.baseAddress else { return }
            channel.update(from: baseAddress.advanced(by: startSample), count: remaining)
        }

        playerNode.stop()
        baseSample = startSample
        let currentPlaybackID = UUID()
        playbackID = currentPlaybackID

        playerNode.scheduleBuffer(
            buffer,
            completionCallbackType: .dataPlayedBack
        ) { [weak self] _ in
            DispatchQueue.main.async {
                guard let self, self.playbackID == currentPlaybackID else { return }
                self.stopProgressTimer()
                self.currentTime = self.duration
                self.baseSample = self.samples.count
                self.state = .ready
            }
        }

        playerNode.play()
        state = .playing
        startProgressTimer()
    }

    private func startProgressTimer() {
        stopProgressTimer()
        progressTimer = Timer.scheduledTimer(withTimeInterval: 0.05, repeats: true) { [weak self] _ in
            DispatchQueue.main.async {
                self?.updateProgress()
            }
        }
    }

    private func stopProgressTimer() {
        progressTimer?.invalidate()
        progressTimer = nil
    }

    private func updateProgress() {
        guard
            state == .playing,
            let renderTime = playerNode.lastRenderTime,
            let playerTime = playerNode.playerTime(forNodeTime: renderTime)
        else {
            return
        }

        let playedSamples = max(0, Int(playerTime.sampleTime))
        currentTime = min(duration, Double(baseSample + playedSamples) / sampleRate)
    }
}
