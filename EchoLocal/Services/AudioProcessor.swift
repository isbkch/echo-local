import Foundation

enum AudioProcessor {
    static func finish(
        _ samples: [Float],
        sampleRate: Double,
        normalize: Bool,
        trimSilence: Bool
    ) -> [Float] {
        guard !samples.isEmpty else { return [] }

        var output = samples.map { $0.isFinite ? $0 : 0 }

        if trimSilence {
            output = trim(output, sampleRate: sampleRate)
        }

        if normalize {
            output = normalizePeak(output)
        }

        applyEdgeFade(to: &output, sampleRate: sampleRate)
        return output
    }

    static func silence(duration: TimeInterval, sampleRate: Double) -> [Float] {
        Array(repeating: 0, count: max(0, Int(duration * sampleRate)))
    }

    static func waveform(from samples: [Float], bucketCount: Int = 420) -> [Float] {
        guard !samples.isEmpty, bucketCount > 0 else { return [] }
        let stride = max(1, samples.count / bucketCount)

        return Swift.stride(from: 0, to: samples.count, by: stride).map { start in
            let end = min(samples.count, start + stride)
            return samples[start..<end].reduce(0) { max($0, abs($1)) }
        }
    }

    private static func trim(_ samples: [Float], sampleRate: Double) -> [Float] {
        let threshold: Float = 0.004
        guard
            let first = samples.firstIndex(where: { abs($0) >= threshold }),
            let last = samples.lastIndex(where: { abs($0) >= threshold })
        else {
            return samples
        }

        let padding = Int(sampleRate * 0.025)
        let lower = max(samples.startIndex, first - padding)
        let upper = min(samples.index(before: samples.endIndex), last + padding)
        return Array(samples[lower...upper])
    }

    private static func normalizePeak(_ samples: [Float]) -> [Float] {
        let peak = samples.reduce(Float.zero) { max($0, abs($1)) }
        guard peak > 0.0001 else { return samples }

        let target = pow(10.0 as Float, -1.0 / 20.0)
        let gain = min(target / peak, 6)
        return samples.map { min(max($0 * gain, -1), 1) }
    }

    private static func applyEdgeFade(to samples: inout [Float], sampleRate: Double) {
        let fadeCount = min(samples.count / 2, max(1, Int(sampleRate * 0.008)))
        guard fadeCount > 1 else { return }

        for index in 0..<fadeCount {
            let gain = Float(index) / Float(fadeCount - 1)
            samples[index] *= gain
            samples[samples.count - 1 - index] *= gain
        }
    }
}

