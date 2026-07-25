import SwiftUI

struct ModelSetupView: View {
    @ObservedObject var store: ModelStore
    let chooseExisting: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            ZStack {
                Circle()
                    .fill(EchoLocalTheme.accentSoft)
                    .frame(width: 58, height: 58)

                Image(systemName: "waveform")
                    .font(.system(size: 23, weight: .semibold))
                    .foregroundStyle(EchoLocalTheme.accent)
            }
            .padding(.bottom, 18)

            Text("Bring Kokoro onto this Mac")
                .font(.system(size: 25, weight: .semibold, design: .serif))
                .foregroundStyle(EchoLocalTheme.ink)

            Text("One download gives Echolocal five natural voices. After that, your writing and speech never leave this computer.")
                .font(.system(size: 13))
                .foregroundStyle(EchoLocalTheme.secondaryInk)
                .multilineTextAlignment(.center)
                .lineSpacing(4)
                .frame(maxWidth: 390)
                .padding(.top, 9)

            setupAction
                .padding(.top, 24)

            if case .downloading = store.state {
                EmptyView()
            } else {
                Button("Use existing model files…", action: chooseExisting)
                    .buttonStyle(.link)
                    .font(.system(size: 11))
                    .padding(.top, 12)
            }

            Text("Kokoro 82M · MLX · about 330 MB")
                .font(.system(size: 10, weight: .medium, design: .monospaced))
                .foregroundStyle(EchoLocalTheme.faintInk)
                .padding(.top, 18)
        }
        .padding(.horizontal, 48)
        .padding(.vertical, 42)
        .background(
            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .fill(EchoLocalTheme.paper)
                .shadow(color: .black.opacity(0.11), radius: 30, y: 12)
                .overlay(
                    RoundedRectangle(cornerRadius: 22, style: .continuous)
                        .stroke(EchoLocalTheme.line, lineWidth: 0.7)
                )
        )
        .frame(width: 520)
    }

    @ViewBuilder
    private var setupAction: some View {
        switch store.state {
        case .checking:
            ProgressView()
                .controlSize(.small)
        case .missing:
            Button {
                store.download()
            } label: {
                Label("Download local model", systemImage: "arrow.down.circle.fill")
            }
            .buttonStyle(PrimaryActionButtonStyle())
        case .downloading(let progress, let fileName):
            VStack(spacing: 9) {
                ProgressView(value: progress)
                    .progressViewStyle(.linear)
                    .tint(EchoLocalTheme.accent)
                    .frame(width: 270)
                HStack {
                    Text("Downloading \(fileName)…")
                    Spacer()
                    Text("\(Int(progress * 100))%")
                        .monospacedDigit()
                }
                .font(.system(size: 10, weight: .medium))
                .foregroundStyle(EchoLocalTheme.secondaryInk)
                .frame(width: 270)
            }
        case .ready:
            Label("Kokoro is ready", systemImage: "checkmark.circle.fill")
                .foregroundStyle(EchoLocalTheme.success)
        case .failed(let message):
            VStack(spacing: 12) {
                Text(message)
                    .font(.system(size: 11))
                    .foregroundStyle(EchoLocalTheme.accent)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: 360)
                Button("Try download again") {
                    store.download()
                }
                .buttonStyle(PrimaryActionButtonStyle())
            }
        }
    }
}
