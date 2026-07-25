import SwiftUI

struct TransportBar: View {
    @ObservedObject var model: AppModel
    @ObservedObject private var player: AudioPlayerController

    init(model: AppModel) {
        self.model = model
        player = model.player
    }

    var body: some View {
        VStack(spacing: 8) {
            HStack(spacing: 16) {
                Button {
                    model.playPauseOrGenerate()
                } label: {
                    ZStack {
                        Circle()
                            .fill(player.state == .empty ? EchoLocalTheme.raised : EchoLocalTheme.ink)
                            .overlay(Circle().stroke(EchoLocalTheme.line, lineWidth: 0.7))
                        Image(systemName: player.state == .playing ? "pause.fill" : "play.fill")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundStyle(player.state == .empty ? EchoLocalTheme.faintInk : EchoLocalTheme.paper)
                            .offset(x: player.state == .playing ? 0 : 1)
                    }
                    .frame(width: 42, height: 42)
                }
                .buttonStyle(.plain)
                .disabled(player.state == .empty && !model.canGenerate)
                .accessibilityLabel(player.state == .playing ? "Pause" : "Play")

                VStack(spacing: 6) {
                    WaveformView(
                        samples: model.waveform,
                        progress: player.duration > 0 ? player.currentTime / player.duration : 0
                    )
                    .frame(height: 38)

                    HStack {
                        Text(player.currentTime.formattedClock)
                        Spacer()
                        Text(player.duration.formattedClock)
                    }
                    .font(.system(size: 9, weight: .medium, design: .monospaced))
                    .foregroundStyle(EchoLocalTheme.secondaryInk)
                }
                .overlay(alignment: .bottom) {
                    Slider(
                        value: Binding(
                            get: { player.currentTime },
                            set: { player.seek(to: $0) }
                        ),
                        in: 0...max(player.duration, 0.01)
                    )
                    .labelsHidden()
                    .tint(.clear)
                    .opacity(0.015)
                    .frame(height: 46)
                    .offset(y: -7)
                    .disabled(player.state == .empty)
                    .accessibilityLabel("Playback position")
                }

                Button {
                    model.exportWAV()
                } label: {
                    Image(systemName: "square.and.arrow.down")
                        .frame(width: 32, height: 32)
                }
                .buttonStyle(QuietButtonStyle())
                .disabled(player.state == .empty)
                .help("Export WAV")
                .accessibilityLabel("Export WAV")

                Button {
                    model.generateSpeech()
                } label: {
                    HStack(spacing: 8) {
                        if model.isGenerating {
                            ProgressView()
                                .controlSize(.small)
                                .tint(.white)
                        } else {
                            Image(systemName: "waveform.badge.plus")
                        }
                        Text(player.state == .empty ? "Generate speech" : "Regenerate")
                    }
                }
                .buttonStyle(PrimaryActionButtonStyle())
                .disabled(!model.canGenerate)
                .keyboardShortcut(.return, modifiers: [.command])
            }

            HStack {
                Text(model.statusText)
                    .lineLimit(1)
                Spacer()
                HStack(spacing: 5) {
                    Image(systemName: "lock.fill")
                    Text("On-device")
                }
            }
            .font(.system(size: 10, weight: .medium))
            .foregroundStyle(statusColor)
        }
        .padding(.horizontal, 22)
        .padding(.vertical, 14)
        .background(.regularMaterial)
    }

    private var statusColor: Color {
        if case .failed = model.workState {
            return EchoLocalTheme.accent
        }
        return EchoLocalTheme.secondaryInk
    }
}

