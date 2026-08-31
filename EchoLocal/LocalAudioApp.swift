import SwiftUI
#if os(iOS) && !targetEnvironment(simulator)
import MLX
#endif

@main
struct EchoLocalApp: App {
    @StateObject private var model = AppModel()

    init() {
        #if os(iOS) && !targetEnvironment(simulator)
        // Keep MLX's reusable Metal buffers bounded below iOS's jetsam threshold.
        Memory.cacheLimit = 50 * 1024 * 1024
        Memory.memoryLimit = 900 * 1024 * 1024
        #endif
    }

    var body: some Scene {
        #if os(macOS)
        Window("Echolocal", id: "main") {
            ContentView(model: model)
        }
        .defaultSize(width: 1180, height: 760)
        .windowResizability(.contentMinSize)
        .windowStyle(.hiddenTitleBar)
        .commands {
            CommandMenu("Speech") {
                Button("Generate Speech") {
                    model.generateSpeech()
                }
                .keyboardShortcut(.return, modifiers: [.command])
                .disabled(!model.canGenerate)

                Button(model.player.state == .playing ? "Pause" : "Play") {
                    model.playPauseOrGenerate()
                }
                .keyboardShortcut(.space, modifiers: [.command, .shift])
                .disabled(model.player.state == .empty && !model.canGenerate)

                Divider()

                Button("Export WAV…") {
                    model.exportWAV()
                }
                .keyboardShortcut("e", modifiers: [.command, .shift])
                .disabled(model.player.state == .empty)
            }
        }
        #else
        WindowGroup {
            IPhoneContentView(model: model)
        }
        #endif
    }
}
