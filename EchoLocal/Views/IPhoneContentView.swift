#if os(iOS)
import SwiftUI
import UIKit

struct IPhoneContentView: View {
    @ObservedObject var model: AppModel
    @ObservedObject private var modelStore: ModelStore
    @State private var showsVoiceDirection = false
    @FocusState private var editorIsFocused: Bool

    init(model: AppModel) {
        self.model = model
        _modelStore = ObservedObject(wrappedValue: model.modelStore)
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                appHeader
                divider
                scriptHeader
                divider
                editor
                divider
                scriptFooter
            }
            .frame(maxWidth: 760)
            .frame(maxWidth: .infinity)
            .background(EchoLocalTheme.paper)
            .safeAreaInset(edge: .bottom, spacing: 0) {
                IPhoneListeningRail(model: model)
            }
            .toolbar(.hidden, for: .navigationBar)
        }
        .tint(EchoLocalTheme.accent)
        .sheet(isPresented: $showsVoiceDirection) {
            IPhoneVoiceDirectionView(model: model)
        }
        .sheet(item: exportArtifactBinding) { artifact in
            ActivityView(activityItems: [artifact.url])
                .presentationDetents([.medium, .large])
                .presentationDragIndicator(.visible)
        }
        .overlay {
            if !modelStore.isReady {
                IPhoneModelSetupView(store: modelStore)
                    .transition(.opacity)
            }
        }
        .animation(.easeInOut(duration: 0.2), value: modelStore.isReady)
    }

    private var exportArtifactBinding: Binding<WAVExportArtifact?> {
        Binding(
            get: { model.exportArtifact },
            set: { artifact in
                if artifact == nil {
                    model.clearExportArtifact()
                }
            }
        )
    }

    private var scriptHeader: some View {
        HStack(spacing: 10) {
            Text("SCRIPT")
                .font(.system(size: 10, weight: .bold))
                .tracking(1.4)
                .foregroundStyle(EchoLocalTheme.secondaryInk)

            Spacer()

            Button {
                model.pasteFromClipboard()
                editorIsFocused = true
            } label: {
                Label("Paste", systemImage: "doc.on.clipboard")
            }
            .buttonStyle(QuietButtonStyle())

            Button("Clear") {
                model.clearText()
                editorIsFocused = true
            }
            .buttonStyle(QuietButtonStyle())
            .disabled(model.text.isEmpty)
        }
        .padding(.horizontal, 18)
        .frame(height: 54)
        .background(EchoLocalTheme.paper)
    }

    private var appHeader: some View {
        HStack {
            wordmark
            Spacer()
            Button {
                showsVoiceDirection = true
            } label: {
                Image(systemName: "slider.horizontal.3")
                    .font(.system(size: 16, weight: .semibold))
                    .frame(width: 34, height: 34)
            }
            .buttonStyle(.plain)
            .foregroundStyle(EchoLocalTheme.accent)
            .background(Circle().fill(EchoLocalTheme.raised))
            .accessibilityLabel("Voice direction")
        }
        .padding(.horizontal, 18)
        .frame(height: 54)
        .background(.regularMaterial)
    }

    private var editor: some View {
        ZStack(alignment: .topLeading) {
            if model.text.isEmpty {
                VStack(alignment: .leading, spacing: 10) {
                    Text("Paste something worth hearing.")
                        .font(.system(size: 25, weight: .regular, design: .serif))
                        .foregroundStyle(EchoLocalTheme.faintInk)

                    Text("An article, a draft, a page of notes—your text stays on this device.")
                        .font(.system(size: 13))
                        .foregroundStyle(EchoLocalTheme.faintInk)
                        .lineSpacing(3)
                }
                .padding(.horizontal, 22)
                .padding(.top, 22)
                .allowsHitTesting(false)
            }

            TextEditor(text: $model.text)
                .font(.system(size: 19, weight: .regular, design: .serif))
                .lineSpacing(7)
                .foregroundStyle(EchoLocalTheme.ink)
                .scrollContentBackground(.hidden)
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .focused($editorIsFocused)
                .accessibilityLabel("Text to turn into speech")
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(EchoLocalTheme.paper)
    }

    private var scriptFooter: some View {
        HStack(spacing: 14) {
            Text("\(model.wordCount) words")
            Text("\(model.characterCount) characters")
            Spacer()
            if model.estimatedDuration > 0 {
                Label("about \(model.estimatedDuration.formattedClock)", systemImage: "clock")
            }
        }
        .font(.system(size: 10, weight: .medium))
        .foregroundStyle(EchoLocalTheme.secondaryInk)
        .padding(.horizontal, 18)
        .frame(height: 38)
        .background(EchoLocalTheme.paper)
    }

    private var wordmark: some View {
        HStack(spacing: 5) {
            Image(systemName: "waveform")
                .font(.system(size: 13, weight: .bold))
                .foregroundStyle(EchoLocalTheme.accent)
            Text("Echo")
                .font(.system(size: 16, weight: .semibold))
            Text("Local")
                .font(.system(size: 17, weight: .medium, design: .serif))
                .italic()
        }
        .foregroundStyle(EchoLocalTheme.ink)
        .accessibilityElement(children: .combine)
    }

    private var divider: some View {
        Rectangle()
            .fill(EchoLocalTheme.line)
            .frame(height: 0.5)
    }
}

private struct IPhoneListeningRail: View {
    @ObservedObject var model: AppModel
    @ObservedObject private var player: AudioPlayerController

    init(model: AppModel) {
        self.model = model
        _player = ObservedObject(wrappedValue: model.player)
    }

    var body: some View {
        VStack(spacing: 9) {
            HStack(spacing: 8) {
                Text(model.statusText)
                    .lineLimit(1)
                Spacer(minLength: 8)
                Label("On-device", systemImage: "lock.fill")
                    .labelStyle(.titleAndIcon)
            }
            .font(.system(size: 10, weight: .semibold))
            .foregroundStyle(statusColor)

            WaveformView(
                samples: model.waveform,
                progress: player.duration > 0 ? player.currentTime / player.duration : 0
            )
            .frame(height: 34)
            .overlay {
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
                .disabled(player.state == .empty)
                .accessibilityLabel("Playback position")
            }

            HStack {
                Text(player.currentTime.formattedClock)
                Spacer()
                Text(player.duration.formattedClock)
            }
            .font(.system(size: 9, weight: .medium, design: .monospaced))
            .foregroundStyle(EchoLocalTheme.secondaryInk)
            .monospacedDigit()

            HStack(spacing: 12) {
                Button {
                    model.playPauseOrGenerate()
                } label: {
                    ZStack {
                        Circle()
                            .fill(player.state == .empty ? EchoLocalTheme.raised : EchoLocalTheme.ink)
                            .overlay(Circle().stroke(EchoLocalTheme.line, lineWidth: 0.7))
                        Image(systemName: player.state == .playing ? "pause.fill" : "play.fill")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundStyle(
                                player.state == .empty
                                    ? EchoLocalTheme.faintInk
                                    : EchoLocalTheme.paper
                            )
                            .offset(x: player.state == .playing ? 0 : 1)
                    }
                    .frame(width: 42, height: 42)
                }
                .buttonStyle(.plain)
                .disabled(player.state == .empty && !model.canGenerate)
                .accessibilityLabel(player.state == .playing ? "Pause" : "Play")

                Spacer()

                Button {
                    model.exportWAV()
                } label: {
                    Image(systemName: "square.and.arrow.up")
                        .frame(width: 28, height: 28)
                }
                .buttonStyle(QuietButtonStyle())
                .disabled(player.state == .empty)
                .accessibilityLabel("Share WAV")

                Button {
                    model.generateSpeech()
                } label: {
                    HStack(spacing: 7) {
                        if model.isGenerating {
                            ProgressView()
                                .controlSize(.small)
                                .tint(.white)
                        }
                        Text(player.state == .empty ? "Generate" : "Regenerate")
                    }
                }
                .buttonStyle(PrimaryActionButtonStyle())
                .disabled(!model.canGenerate)
            }
        }
        .padding(.horizontal, 16)
        .padding(.top, 12)
        .padding(.bottom, 9)
        .background(.regularMaterial)
        .overlay(alignment: .top) {
            Rectangle()
                .fill(EchoLocalTheme.line)
                .frame(height: 0.5)
        }
    }

    private var statusColor: Color {
        if case .failed = model.workState {
            return EchoLocalTheme.accent
        }
        return EchoLocalTheme.secondaryInk
    }
}

private struct IPhoneVoiceDirectionView: View {
    @ObservedObject var model: AppModel
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    sectionLabel("VOICE")
                    voiceChoices
                    divider
                    sectionLabel("DELIVERY")
                    deliveryControls
                    divider
                    sectionLabel("FINISH")
                    finishControls
                    divider
                    localModelStatus
                }
                .padding(.bottom, 28)
            }
            .background(EchoLocalTheme.canvas)
            .navigationTitle("Voice direction")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                        .fontWeight(.semibold)
                }
            }
        }
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
    }

    private var voiceChoices: some View {
        VStack(spacing: 0) {
            ForEach(KokoroVoice.curated) { voice in
                Button {
                    model.selectedVoiceID = voice.id
                } label: {
                    HStack(spacing: 12) {
                        ZStack {
                            Circle()
                                .fill(
                                    voice.id == model.selectedVoiceID
                                        ? EchoLocalTheme.accentSoft
                                        : EchoLocalTheme.raised
                                )
                            Text(String(voice.name.prefix(1)))
                                .font(.system(size: 15, weight: .semibold, design: .serif))
                                .foregroundStyle(
                                    voice.id == model.selectedVoiceID
                                        ? EchoLocalTheme.accent
                                        : EchoLocalTheme.secondaryInk
                                )
                        }
                        .frame(width: 38, height: 38)

                        VStack(alignment: .leading, spacing: 2) {
                            Text(voice.name)
                                .font(.system(size: 14, weight: .semibold))
                            Text("\(voice.character) · \(voice.region)")
                                .font(.system(size: 11))
                                .foregroundStyle(EchoLocalTheme.secondaryInk)
                                .lineLimit(1)
                        }

                        Spacer()
                        if voice.id == model.selectedVoiceID {
                            Image(systemName: "checkmark")
                                .font(.system(size: 13, weight: .bold))
                                .foregroundStyle(EchoLocalTheme.accent)
                        }
                    }
                    .contentShape(Rectangle())
                    .padding(.horizontal, 18)
                    .frame(height: 58)
                }
                .buttonStyle(.plain)
            }
        }
        .background(EchoLocalTheme.paper)
    }

    private var deliveryControls: some View {
        VStack(spacing: 24) {
            VStack(alignment: .leading, spacing: 10) {
                HStack {
                    Text("Pace")
                    Spacer()
                    valuePill(String(format: "%.2f×", model.speed))
                }
                Slider(value: $model.speed, in: 0.72...1.28, step: 0.01)
                    .tint(EchoLocalTheme.accent)
                HStack {
                    Text("Unhurried")
                    Spacer()
                    Text("Brisk")
                }
                .font(.system(size: 10, weight: .medium))
                .foregroundStyle(EchoLocalTheme.faintInk)
            }

            VStack(alignment: .leading, spacing: 10) {
                HStack {
                    Text("Space between paragraphs")
                    Spacer()
                    valuePill(String(format: "%.2fs", model.paragraphPause))
                }
                Slider(value: $model.paragraphPause, in: 0.15...0.9, step: 0.05)
                    .tint(EchoLocalTheme.accent)
            }
        }
        .font(.system(size: 13, weight: .semibold))
        .padding(18)
        .background(EchoLocalTheme.paper)
    }

    private var finishControls: some View {
        VStack(spacing: 18) {
            settingToggle(
                title: "Even out loudness",
                detail: "Bring the peak to a clean, consistent level.",
                isOn: $model.normalizesAudio
            )
            settingToggle(
                title: "Clean the edges",
                detail: "Trim excess silence around each passage.",
                isOn: $model.trimsSilence
            )
            settingToggle(
                title: "Play when ready",
                detail: "Start listening as soon as speech is generated.",
                isOn: $model.autoPlays
            )
        }
        .padding(18)
        .background(EchoLocalTheme.paper)
    }

    private var localModelStatus: some View {
        HStack(spacing: 9) {
            Circle()
                .fill(EchoLocalTheme.success)
                .frame(width: 7, height: 7)
            VStack(alignment: .leading, spacing: 2) {
                Text("Kokoro is on this device")
                    .font(.system(size: 13, weight: .semibold))
                Text("Generation needs no network connection.")
                    .font(.system(size: 11))
                    .foregroundStyle(EchoLocalTheme.secondaryInk)
            }
            Spacer()
            Image(systemName: "lock.fill")
                .foregroundStyle(EchoLocalTheme.success)
        }
        .padding(18)
        .background(EchoLocalTheme.paper)
    }

    private func sectionLabel(_ title: String) -> some View {
        Text(title)
            .font(.system(size: 10, weight: .bold))
            .tracking(1.4)
            .foregroundStyle(EchoLocalTheme.secondaryInk)
            .padding(.horizontal, 18)
            .padding(.top, 22)
            .padding(.bottom, 9)
    }

    private var divider: some View {
        Rectangle()
            .fill(EchoLocalTheme.line)
            .frame(height: 0.5)
    }

    private func valuePill(_ value: String) -> some View {
        Text(value)
            .font(.system(size: 10, weight: .semibold, design: .monospaced))
            .foregroundStyle(EchoLocalTheme.secondaryInk)
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(Capsule().fill(EchoLocalTheme.raised))
    }

    private func settingToggle(
        title: String,
        detail: String,
        isOn: Binding<Bool>
    ) -> some View {
        HStack(alignment: .top, spacing: 12) {
            VStack(alignment: .leading, spacing: 3) {
                Text(title)
                    .font(.system(size: 13, weight: .semibold))
                Text(detail)
                    .font(.system(size: 11))
                    .foregroundStyle(EchoLocalTheme.secondaryInk)
            }
            Spacer(minLength: 8)
            Toggle("", isOn: isOn)
                .labelsHidden()
                .tint(EchoLocalTheme.accent)
        }
    }
}

private struct IPhoneModelSetupView: View {
    @ObservedObject var store: ModelStore

    var body: some View {
        ZStack {
            EchoLocalTheme.paper
                .ignoresSafeArea()

            ScrollView {
                VStack(spacing: 0) {
                    Spacer(minLength: 54)

                    setupMark
                        .padding(.bottom, 26)

                    Text("Private speech,\non this device.")
                        .font(.system(size: 36, weight: .semibold, design: .serif))
                        .foregroundStyle(EchoLocalTheme.ink)
                        .multilineTextAlignment(.center)

                    Text("Download Kokoro once. After that, your writing and generated speech stay here—even in Airplane Mode.")
                        .font(.system(size: 15))
                        .foregroundStyle(EchoLocalTheme.secondaryInk)
                        .multilineTextAlignment(.center)
                        .lineSpacing(5)
                        .frame(maxWidth: 340)
                        .padding(.top, 16)

                    setupAction
                        .padding(.top, 30)

                    VStack(spacing: 7) {
                        Label("About 330 MB", systemImage: "internaldrive")
                        Label("Five English voices", systemImage: "waveform")
                        Label("No network used for generation", systemImage: "lock.fill")
                    }
                    .font(.system(size: 11, weight: .medium))
                    .foregroundStyle(EchoLocalTheme.secondaryInk)
                    .padding(.top, 28)

                    Spacer(minLength: 30)
                }
                .frame(maxWidth: .infinity)
                .padding(.horizontal, 24)
            }
        }
    }

    private var setupMark: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .fill(EchoLocalTheme.ink)
            HStack(alignment: .center, spacing: 4) {
                ForEach([12.0, 25.0, 38.0, 23.0, 11.0], id: \.self) { height in
                    Capsule()
                        .fill(EchoLocalTheme.paper)
                        .frame(width: 3.4, height: height)
                }
            }
        }
        .frame(width: 82, height: 82)
        .accessibilityHidden(true)
    }

    @ViewBuilder
    private var setupAction: some View {
        switch store.state {
        case .checking:
            ProgressView("Checking this device…")
                .font(.system(size: 12, weight: .medium))
        case .missing:
            Button {
                store.download()
            } label: {
                Label("Download local model", systemImage: "arrow.down.circle.fill")
            }
            .buttonStyle(PrimaryActionButtonStyle())
            .accessibilityHint("Downloads Kokoro and five voices for offline generation")
        case .downloading(let progress, let fileName):
            VStack(spacing: 10) {
                ProgressView(value: progress)
                    .progressViewStyle(.linear)
                    .tint(EchoLocalTheme.accent)
                    .frame(maxWidth: 300)
                HStack {
                    Text("Downloading \(fileName)…")
                    Spacer()
                    Text("\(Int(progress * 100))%")
                        .monospacedDigit()
                }
                .font(.system(size: 11, weight: .medium))
                .foregroundStyle(EchoLocalTheme.secondaryInk)
                .frame(maxWidth: 300)
            }
        case .ready:
            Label("Ready offline", systemImage: "checkmark.circle.fill")
                .foregroundStyle(EchoLocalTheme.success)
        case .failed(let message):
            VStack(spacing: 14) {
                Text(message)
                    .font(.system(size: 12))
                    .foregroundStyle(EchoLocalTheme.accent)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: 330)
                Button("Try download again") {
                    store.download()
                }
                .buttonStyle(PrimaryActionButtonStyle())
            }
        }
    }
}

private struct ActivityView: UIViewControllerRepresentable {
    let activityItems: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: activityItems, applicationActivities: nil)
    }

    func updateUIViewController(_ viewController: UIActivityViewController, context: Context) {}
}
#endif
