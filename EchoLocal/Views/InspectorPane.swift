import SwiftUI

struct InspectorPane: View {
    @ObservedObject var model: AppModel
    @ObservedObject private var modelStore: ModelStore

    init(model: AppModel) {
        self.model = model
        _modelStore = ObservedObject(wrappedValue: model.modelStore)
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                inspectorHeader
                divider
                voiceSection
                divider
                deliverySection
                divider
                finishSection
                divider
                modelSection
            }
        }
        .background(EchoLocalTheme.canvas)
    }

    private var inspectorHeader: some View {
        HStack {
            Text("VOICE DIRECTION")
                .font(.system(size: 10, weight: .bold))
                .tracking(1.4)
                .foregroundStyle(EchoLocalTheme.secondaryInk)
            Spacer()
            Image(systemName: "slider.horizontal.3")
                .foregroundStyle(EchoLocalTheme.faintInk)
        }
        .padding(.horizontal, 22)
        .frame(height: 54)
    }

    private var voiceSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            sectionLabel("Voice")

            Picker("Voice", selection: $model.selectedVoiceID) {
                ForEach(KokoroVoice.curated) { voice in
                    Text("\(voice.name) · \(voice.region)")
                        .tag(voice.id)
                }
            }
            .labelsHidden()
            .pickerStyle(.menu)
            .frame(maxWidth: .infinity, alignment: .leading)

            HStack(spacing: 10) {
                ZStack {
                    Circle()
                        .fill(EchoLocalTheme.accentSoft)
                    Text(String(model.selectedVoice.name.prefix(1)))
                        .font(.system(size: 16, weight: .semibold, design: .serif))
                        .foregroundStyle(EchoLocalTheme.accent)
                }
                .frame(width: 38, height: 38)

                VStack(alignment: .leading, spacing: 2) {
                    Text(model.selectedVoice.name)
                        .font(.system(size: 13, weight: .semibold))
                    Text("\(model.selectedVoice.character) · \(model.selectedVoice.region)")
                        .font(.system(size: 11))
                        .foregroundStyle(EchoLocalTheme.secondaryInk)
                        .lineLimit(1)
                }
            }
        }
        .padding(22)
    }

    private var deliverySection: some View {
        VStack(alignment: .leading, spacing: 20) {
            VStack(alignment: .leading, spacing: 10) {
                HStack {
                    sectionLabel("Pace")
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
                .font(.system(size: 9, weight: .medium))
                .foregroundStyle(EchoLocalTheme.faintInk)
            }

            VStack(alignment: .leading, spacing: 10) {
                HStack {
                    sectionLabel("Space between paragraphs")
                    Spacer()
                    valuePill(String(format: "%.2fs", model.paragraphPause))
                }

                Slider(value: $model.paragraphPause, in: 0.15...0.9, step: 0.05)
                    .tint(EchoLocalTheme.accent)
            }
        }
        .padding(22)
    }

    private var finishSection: some View {
        VStack(alignment: .leading, spacing: 15) {
            sectionLabel("Finish")

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
        .padding(22)
    }

    private var modelSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            sectionLabel("Local model")

            HStack(spacing: 8) {
                Circle()
                    .fill(modelStore.isReady ? EchoLocalTheme.success : EchoLocalTheme.faintInk)
                    .frame(width: 7, height: 7)
                Text(modelStore.isReady ? "Kokoro is ready" : "Kokoro is not installed")
                    .font(.system(size: 12, weight: .medium))
            }

            if modelStore.isReady {
                Button("Show model files") {
                    modelStore.revealModelFolder()
                }
                .buttonStyle(.link)
                .font(.system(size: 11))
            }
        }
        .padding(22)
    }

    private var divider: some View {
        Rectangle()
            .fill(EchoLocalTheme.line)
            .frame(height: 0.7)
    }

    private func sectionLabel(_ title: String) -> some View {
        Text(title)
            .font(.system(size: 11, weight: .semibold))
            .foregroundStyle(EchoLocalTheme.ink)
    }

    private func valuePill(_ value: String) -> some View {
        Text(value)
            .font(.system(size: 10, weight: .semibold, design: .monospaced))
            .foregroundStyle(EchoLocalTheme.secondaryInk)
            .padding(.horizontal, 7)
            .padding(.vertical, 3)
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
                    .font(.system(size: 12, weight: .medium))
                Text(detail)
                    .font(.system(size: 10))
                    .foregroundStyle(EchoLocalTheme.secondaryInk)
                    .fixedSize(horizontal: false, vertical: true)
            }
            Spacer(minLength: 4)
            Toggle("", isOn: isOn)
                .labelsHidden()
                .toggleStyle(.switch)
                .controlSize(.small)
                .tint(EchoLocalTheme.accent)
        }
    }
}
