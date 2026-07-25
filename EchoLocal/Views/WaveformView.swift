import SwiftUI

struct WaveformView: View {
    let samples: [Float]
    let progress: Double

    var body: some View {
        Canvas { context, size in
            let envelope = samples.isEmpty ? idleEnvelope(count: 120) : samples
            guard envelope.count > 1 else { return }

            let basePath = waveformPath(envelope: envelope, in: size)
            context.fill(basePath, with: .color(EchoLocalTheme.faintInk.opacity(0.24)))

            var playedContext = context
            let clipWidth = size.width * min(max(progress, 0), 1)
            playedContext.clip(to: Path(CGRect(x: 0, y: 0, width: clipWidth, height: size.height)))
            playedContext.fill(basePath, with: .color(EchoLocalTheme.accent))

            var center = Path()
            center.move(to: CGPoint(x: 0, y: size.height / 2))
            center.addLine(to: CGPoint(x: size.width, y: size.height / 2))
            context.stroke(center, with: .color(EchoLocalTheme.line.opacity(0.5)), lineWidth: 0.5)
        }
        .accessibilityHidden(true)
    }

    private func waveformPath(envelope: [Float], in size: CGSize) -> Path {
        let peak = max(envelope.max() ?? 1, 0.001)
        let centerY = size.height / 2
        let amplitude = size.height * 0.42
        let xStep = size.width / CGFloat(envelope.count - 1)

        var path = Path()
        path.move(to: CGPoint(x: 0, y: centerY))

        for index in envelope.indices {
            let normalized = CGFloat(envelope[index] / peak)
            let shaped = max(0.035, pow(normalized, 0.72))
            path.addLine(
                to: CGPoint(
                    x: CGFloat(index) * xStep,
                    y: centerY - shaped * amplitude
                )
            )
        }

        for index in envelope.indices.reversed() {
            let normalized = CGFloat(envelope[index] / peak)
            let shaped = max(0.035, pow(normalized, 0.72))
            path.addLine(
                to: CGPoint(
                    x: CGFloat(index) * xStep,
                    y: centerY + shaped * amplitude
                )
            )
        }

        path.closeSubpath()
        return path
    }

    private func idleEnvelope(count: Int) -> [Float] {
        (0..<count).map { index in
            let position = Double(index) / Double(max(1, count - 1))
            let breath = sin(position * .pi)
            let texture = 0.38 + 0.14 * sin(position * .pi * 7)
            return Float(max(0.04, breath * texture))
        }
    }
}

