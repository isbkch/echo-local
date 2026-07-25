import SwiftUI

@main
struct EchoLocalApp: App {
    @StateObject private var model = AppModel()

    var body: some Scene {
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
    }
}
