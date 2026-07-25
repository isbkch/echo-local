import Foundation

enum WAVEncoder {
    enum EncodingError: LocalizedError {
        case tooManySamples

        var errorDescription: String? {
            "The recording is too long to export as a WAV file."
        }
    }

    static func data(samples: [Float], sampleRate: Double) throws -> Data {
        let bytesPerSample = 2
        let audioByteCount = samples.count * bytesPerSample
        guard audioByteCount <= Int(UInt32.max) - 44 else {
            throw EncodingError.tooManySamples
        }

        var output = Data(capacity: audioByteCount + 44)
        output.appendASCII("RIFF")
        output.appendLittleEndian(UInt32(audioByteCount + 36))
        output.appendASCII("WAVE")
        output.appendASCII("fmt ")
        output.appendLittleEndian(UInt32(16))
        output.appendLittleEndian(UInt16(1))
        output.appendLittleEndian(UInt16(1))
        output.appendLittleEndian(UInt32(sampleRate))
        output.appendLittleEndian(UInt32(sampleRate) * UInt32(bytesPerSample))
        output.appendLittleEndian(UInt16(bytesPerSample))
        output.appendLittleEndian(UInt16(16))
        output.appendASCII("data")
        output.appendLittleEndian(UInt32(audioByteCount))

        for sample in samples {
            let clamped = min(max(sample, -1), 1)
            let pcm = Int16(clamped * Float(Int16.max))
            output.appendLittleEndian(UInt16(bitPattern: pcm))
        }

        return output
    }

    static func write(samples: [Float], sampleRate: Double, to url: URL) throws {
        try data(samples: samples, sampleRate: sampleRate).write(to: url, options: .atomic)
    }
}

private extension Data {
    mutating func appendASCII(_ string: String) {
        append(string.data(using: .ascii)!)
    }

    mutating func appendLittleEndian<T: FixedWidthInteger>(_ value: T) {
        var littleEndian = value.littleEndian
        Swift.withUnsafeBytes(of: &littleEndian) { bytes in
            append(contentsOf: bytes)
        }
    }
}
