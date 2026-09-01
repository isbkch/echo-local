package com.isbkch.echolocal.tts

import android.util.Log
import com.isbkch.echolocal.audio.AudioProcessor
import com.isbkch.echolocal.audio.GeneratedAudioStore
import com.isbkch.echolocal.audio.WavEncoder
import com.isbkch.echolocal.audio.WaveformReducer
import com.isbkch.echolocal.domain.ParagraphPlanner
import com.isbkch.echolocal.domain.SpeechSettings
import com.isbkch.echolocal.domain.VoiceCatalog
import java.io.File
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class GenerationRequest(
    val text: String,
    val settings: SpeechSettings,
)

data class GenerationResult(
    val samples: FloatArray,
    val sampleRate: Int,
    val waveform: FloatArray,
    val wavFile: File,
    val elapsedMillis: Long,
)

data class GenerationMetric(
    val elapsedMillis: Long,
    val sampleCount: Int,
    val voiceId: String,
)

class SpeechGenerator(
    private val engineFactory: TtsEngineFactory,
    private val audioStore: GeneratedAudioStore,
    private val metricsLogger: (GenerationMetric) -> Unit = { metric ->
        Log.i(
            METRICS_TAG,
            "elapsedMillis=${metric.elapsedMillis} sampleCount=${metric.sampleCount} voiceId=${metric.voiceId}",
        )
    },
    private val nanoTime: () -> Long = System::nanoTime,
) : SpeechGenerating {
    private data class CachedEngine(val modelDirectory: File, val engine: TtsEngine)

    private val generationMutex = Mutex()
    private val engineGuard = Any()
    @Volatile private var cachedEngine: CachedEngine? = null

    override suspend fun generate(
        request: GenerationRequest,
        modelDirectory: File,
        onProgress: (Double) -> Unit,
    ): GenerationResult = generationMutex.withLock {
        val segments = ParagraphPlanner.segments(request.text, request.settings.paragraphPauseSeconds)
        require(segments.isNotEmpty()) { "Enter text before generating speech." }
        require(modelDirectory.isDirectory) { "A verified local model is required." }

        val voiceId = VoiceCatalog.curated
            .firstOrNull { it.id == request.settings.voiceId }
            ?.id
            ?: VoiceCatalog.curated.first().id
        val startedAt = nanoTime()
        val engine = engineFor(modelDirectory)
        try {
            val chunks = ArrayList<FloatArray>(segments.size * 2)
            var totalSamples = 0L
            var sampleRate = 0
            var completedCharacters = 0
            val totalCharacters = segments.sumOf { it.text.length }.coerceAtLeast(1)

            segments.forEach { segment ->
                val pcm = engine.synthesize(segment.text, ENGLISH, voiceId)
                require(pcm.sampleRate > 0) { "Soniqo returned an invalid sample rate." }
                if (sampleRate == 0) sampleRate = pcm.sampleRate
                require(pcm.sampleRate == sampleRate) { "Soniqo changed sample rate during generation." }

                addChunk(chunks, pcm.samples.toFloatPcm(), totalSamples).also { totalSamples = it }
                if (segment.pauseAfterSeconds > 0.0) {
                    addChunk(
                        chunks,
                        AudioProcessor.silence(segment.pauseAfterSeconds, sampleRate),
                        totalSamples,
                    ).also { totalSamples = it }
                }
                completedCharacters += segment.text.length
                onProgress(completedCharacters.toDouble() / totalCharacters.toDouble())
            }

            val combined = FloatArray(totalSamples.toInt())
            var writeIndex = 0
            chunks.forEach { chunk ->
                chunk.copyInto(combined, writeIndex)
                writeIndex += chunk.size
            }
            val finished = AudioProcessor.finish(
                combined,
                sampleRate,
                normalize = request.settings.normalizesAudio,
                trimSilence = request.settings.trimsSilence,
            )
            val wavFile = audioStore.publish(WavEncoder.encode(finished, sampleRate))
            val metric = GenerationMetric(
                elapsedMillis = ((nanoTime() - startedAt) / 1_000_000L).coerceAtLeast(0L),
                sampleCount = finished.size,
                voiceId = voiceId,
            )
            runCatching { metricsLogger(metric) }
            GenerationResult(
                samples = finished,
                sampleRate = sampleRate,
                waveform = WaveformReducer.reduce(finished),
                wavFile = wavFile,
                elapsedMillis = metric.elapsedMillis,
            )
        } catch (failure: Throwable) {
            discard(engine)
            throw failure
        }
    }

    override fun cancel() {
        synchronized(engineGuard) { cachedEngine?.engine }?.cancel()
    }

    private fun engineFor(modelDirectory: File): TtsEngine = synchronized(engineGuard) {
        cachedEngine
            ?.takeIf { it.modelDirectory == modelDirectory }
            ?.engine
            ?: run {
                cachedEngine?.engine?.close()
                engineFactory.create(modelDirectory).also {
                    cachedEngine = CachedEngine(modelDirectory, it)
                }
            }
    }

    private fun discard(engine: TtsEngine) {
        synchronized(engineGuard) {
            if (cachedEngine?.engine === engine) cachedEngine = null
        }
        runCatching(engine::close)
    }

    private fun addChunk(chunks: MutableList<FloatArray>, chunk: FloatArray, currentTotal: Long): Long {
        val updated = currentTotal + chunk.size
        require(updated <= Int.MAX_VALUE) { "Generated audio is too long." }
        chunks += chunk
        return updated
    }

    private fun ShortArray.toFloatPcm(): FloatArray = FloatArray(size) { index ->
        val sample = this[index].toInt()
        if (sample == Short.MIN_VALUE.toInt()) -1f else sample / Short.MAX_VALUE.toFloat()
    }

    companion object {
        private const val ENGLISH = "en"
        private const val METRICS_TAG = "EcholocalMetrics"
    }
}
