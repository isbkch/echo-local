import SwiftUI
#if os(macOS)
import AppKit
#elseif os(iOS)
import UIKit
#endif

enum EchoLocalTheme {
    #if os(macOS)
    static let canvas = Color(nsColor: .windowBackgroundColor)
    static let paper = Color(nsColor: .textBackgroundColor)
    static let raised = Color(nsColor: .controlBackgroundColor)
    static let ink = Color(nsColor: .labelColor)
    static let secondaryInk = Color(nsColor: .secondaryLabelColor)
    static let faintInk = Color(nsColor: .tertiaryLabelColor)
    static let line = Color(nsColor: .separatorColor)
    #else
    static let canvas = Color(uiColor: .systemGroupedBackground)
    static let paper = Color(uiColor: .systemBackground)
    static let raised = Color(uiColor: .secondarySystemBackground)
    static let ink = Color(uiColor: .label)
    static let secondaryInk = Color(uiColor: .secondaryLabel)
    static let faintInk = Color(uiColor: .tertiaryLabel)
    static let line = Color(uiColor: .separator)
    #endif
    static let accent = Color(red: 0.80, green: 0.31, blue: 0.19)
    static let accentSoft = Color(red: 0.80, green: 0.31, blue: 0.19).opacity(0.12)
    static let success = Color(red: 0.18, green: 0.52, blue: 0.39)
}

struct PrimaryActionButtonStyle: ButtonStyle {
    @Environment(\.isEnabled) private var isEnabled

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.system(size: 13, weight: .semibold))
            .foregroundStyle(.white)
            .padding(.horizontal, 16)
            .frame(height: 38)
            .background(
                Capsule()
                    .fill(isEnabled ? EchoLocalTheme.accent : EchoLocalTheme.faintInk)
                    .opacity(configuration.isPressed ? 0.78 : 1)
            )
            .scaleEffect(configuration.isPressed ? 0.98 : 1)
            .animation(.easeOut(duration: 0.12), value: configuration.isPressed)
    }
}

struct QuietButtonStyle: ButtonStyle {
    @Environment(\.isEnabled) private var isEnabled

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.system(size: 12, weight: .medium))
            .foregroundStyle(isEnabled ? EchoLocalTheme.ink : EchoLocalTheme.faintInk)
            .padding(.horizontal, 12)
            .frame(height: 32)
            .background(
                Capsule()
                    .fill(EchoLocalTheme.raised.opacity(configuration.isPressed ? 0.65 : 1))
                    .overlay(Capsule().stroke(EchoLocalTheme.line, lineWidth: 0.7))
            )
    }
}
