import SwiftUI

struct EditorPane: View {
    @ObservedObject var model: AppModel
    @FocusState private var editorIsFocused: Bool

    var body: some View {
        VStack(spacing: 0) {
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
                .accessibilityHint("Replaces the editor with text from the clipboard")

                Button("Clear") {
                    model.clearText()
                    editorIsFocused = true
                }
                .buttonStyle(QuietButtonStyle())
                .disabled(model.text.isEmpty)
            }
            .padding(.horizontal, 24)
            .frame(height: 54)

            Rectangle()
                .fill(EchoLocalTheme.line)
                .frame(height: 0.7)

            ZStack(alignment: .topLeading) {
                if model.text.isEmpty {
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Paste something worth hearing.")
                            .font(.system(size: 25, weight: .regular, design: .serif))
                            .foregroundStyle(EchoLocalTheme.faintInk)

                        Text("An article, a draft, a page of notes—your text stays on this Mac.")
                            .font(.system(size: 13))
                            .foregroundStyle(EchoLocalTheme.faintInk)
                    }
                    .padding(.horizontal, 30)
                    .padding(.top, 28)
                    .allowsHitTesting(false)
                }

                TextEditor(text: $model.text)
                    .font(.system(size: 20, weight: .regular, design: .serif))
                    .lineSpacing(8)
                    .foregroundStyle(EchoLocalTheme.ink)
                    .scrollContentBackground(.hidden)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 18)
                    .focused($editorIsFocused)
                    .accessibilityLabel("Text to turn into speech")
            }
            .background(EchoLocalTheme.paper)

            Rectangle()
                .fill(EchoLocalTheme.line)
                .frame(height: 0.7)

            HStack(spacing: 16) {
                Text("\(model.wordCount) words")
                Text("\(model.characterCount) characters")
                Spacer()
                if model.estimatedDuration > 0 {
                    Label("about \(model.estimatedDuration.formattedClock)", systemImage: "clock")
                }
            }
            .font(.system(size: 11, weight: .medium))
            .foregroundStyle(EchoLocalTheme.secondaryInk)
            .padding(.horizontal, 24)
            .frame(height: 40)
        }
        .background(EchoLocalTheme.paper)
        .onAppear {
            editorIsFocused = true
        }
    }
}

