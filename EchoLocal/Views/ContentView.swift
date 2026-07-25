import SwiftUI

struct ContentView: View {
    @ObservedObject var model: AppModel
    @ObservedObject private var modelStore: ModelStore

    init(model: AppModel) {
        self.model = model
        _modelStore = ObservedObject(wrappedValue: model.modelStore)
    }

    var body: some View {
        ZStack {
            VStack(spacing: 0) {
                header

                Rectangle()
                    .fill(EchoLocalTheme.line)
                    .frame(height: 0.7)

                HStack(spacing: 0) {
                    EditorPane(model: model)
                        .frame(minWidth: 520, maxWidth: .infinity, maxHeight: .infinity)

                    Rectangle()
                        .fill(EchoLocalTheme.line)
                        .frame(width: 0.7)

                    InspectorPane(model: model)
                        .frame(width: 320)
                        .frame(maxHeight: .infinity)
                }

                Rectangle()
                    .fill(EchoLocalTheme.line)
                    .frame(height: 0.7)

                TransportBar(model: model)
                    .frame(height: 102)
            }
            .background(EchoLocalTheme.canvas)
            .disabled(!modelStore.isReady)

            if !modelStore.isReady {
                EchoLocalTheme.canvas.opacity(0.82)
                    .background(.ultraThinMaterial)
                    .ignoresSafeArea()

                ModelSetupView(
                    store: modelStore,
                    chooseExisting: model.chooseExistingModelFolder
                )
                .transition(.scale(scale: 0.98).combined(with: .opacity))
            }
        }
        .frame(minWidth: 920, minHeight: 650)
        .tint(EchoLocalTheme.accent)
        .animation(.easeInOut(duration: 0.2), value: modelStore.isReady)
    }

    private var header: some View {
        HStack(spacing: 12) {
            WaveMark()
                .frame(width: 31, height: 31)

            HStack(spacing: 5) {
                Text("Echo")
                    .font(.system(size: 17, weight: .semibold))
                Text("Local")
                    .font(.system(size: 18, weight: .medium, design: .serif))
                    .italic()
            }
            .foregroundStyle(EchoLocalTheme.ink)

            Text("PRIVATE TEXT TO SPEECH")
                .font(.system(size: 9, weight: .bold))
                .tracking(1.25)
                .foregroundStyle(EchoLocalTheme.faintInk)
                .padding(.leading, 5)

            Spacer()

            ModelStatusBadge(store: modelStore)
        }
        .padding(.leading, 82)
        .padding(.trailing, 20)
        .frame(height: 62)
        .background(.regularMaterial)
    }
}

private struct WaveMark: View {
    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 9, style: .continuous)
                .fill(EchoLocalTheme.ink)

            HStack(alignment: .center, spacing: 2.5) {
                ForEach([7.0, 14.0, 20.0, 12.0, 6.0], id: \.self) { height in
                    Capsule()
                        .fill(EchoLocalTheme.paper)
                        .frame(width: 2.2, height: height)
                }
            }
        }
        .accessibilityHidden(true)
    }
}

private struct ModelStatusBadge: View {
    @ObservedObject var store: ModelStore

    var body: some View {
        HStack(spacing: 7) {
            Circle()
                .fill(indicatorColor)
                .frame(width: 7, height: 7)

            Text(label)
                .font(.system(size: 10, weight: .semibold))
        }
        .foregroundStyle(EchoLocalTheme.secondaryInk)
        .padding(.horizontal, 10)
        .frame(height: 27)
        .background(
            Capsule()
                .fill(EchoLocalTheme.raised)
                .overlay(Capsule().stroke(EchoLocalTheme.line, lineWidth: 0.7))
        )
    }

    private var label: String {
        switch store.state {
        case .ready:
            return "Local model ready"
        case .downloading:
            return "Downloading model"
        case .failed:
            return "Model needs attention"
        case .checking:
            return "Checking model"
        case .missing:
            return "Model not installed"
        }
    }

    private var indicatorColor: Color {
        switch store.state {
        case .ready:
            return EchoLocalTheme.success
        case .downloading:
            return EchoLocalTheme.accent
        default:
            return EchoLocalTheme.faintInk
        }
    }
}
