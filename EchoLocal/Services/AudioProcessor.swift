import Foundation

enum AudioProcessor {
    static func finish(
        _ samples: [Float],
        sampleRate: Double,
        normalize: Bool,
        trimSilence: Bool
    ) -> [Float] {
        var output = samples
        finishInPlace(
            &output,
            sampleRate: sampleRate,
            normalize: normalize,
            trimSilence: trimSilence
        )
        return output
    }

    static func finishInPlace(
        _ samples: inout [Float],
        sampleRate: Double,
        normalize: Bool,
        trimSilence: Bool
    ) {
        guard !samples.isEmpty else { return }

        for index in samples.indices where !samples[index].isFinite {
            samples[index] = 0
        }

        if trimSilence {
            samples = trim(samples, sampleRate: sampleRate)
        }

        if normalize {
            normalizePeak(&samples)
        }

        applyEdgeFade(to: &samples, sampleRate: sampleRate)
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

    private static func normalizePeak(_ samples: inout [Float]) {
        let peak = samples.reduce(Float.zero) { max($0, abs($1)) }
        guard peak > 0.0001 else { return }

        let target = pow(10.0 as Float, -1.0 / 20.0)
        let gain = min(target / peak, 6)
        for index in samples.indices {
            samples[index] = min(max(samples[index] * gain, -1), 1)
        }
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
