package com.isbkch.echolocal.tts

import audio.soniqo.speech.SpeechSynthesizer
import audio.soniqo.speech.SpeechSynthesizerConfig
import audio.soniqo.speech.TtsModel
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SoniqoTtsEngine(
    private val synthesizer: SpeechSynthesizer,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default.limitedParallelism(1),
) : TtsEngine {
    override suspend fun synthesize(text: String, language: String, voiceId: String): PcmAudio =
        withContext(dispatcher) {
            val result = synthesizer.synthesize(text, language, voiceId)
            require(result.pcm16.size % Short.SIZE_BYTES == 0) {
                "Soniqo returned an incomplete PCM16 sample."
            }
            val bytes = ByteBuffer.wrap(result.pcm16).order(ByteOrder.LITTLE_ENDIAN)
            val samples = ShortArray(result.pcm16.size / Short.SIZE_BYTES) { bytes.short }
            PcmAudio(samples, result.sampleRate)
        }

    override fun cancel() = synthesizer.stop()

    override fun close() = synthesizer.close()
}

class SoniqoTtsEngineFactory : TtsEngineFactory {
    override fun create(modelDirectory: File): TtsEngine = SoniqoTtsEngine(
        SpeechSynthesizer(
            SpeechSynthesizerConfig(
                modelDir = modelDirectory.absolutePath,
                useNnapi = false,
                ttsModel = TtsModel.KOKORO_SHORT_TURN,
            ),
        ),
    )
}
