package com.isbkch.echolocal.tts

import com.isbkch.echolocal.audio.GeneratedAudioStore
import com.isbkch.echolocal.domain.SpeechSettings
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class SpeechGeneratorTest {
    @Test
    fun generationAddsParagraphSilenceAndPublishesWav() = runTest {
        withDirectories { modelDirectory, cacheDirectory ->
            val engine = FakeTtsEngine(shortArrayOf(3_000, -3_000), sampleRate = 100)
            val generator = SpeechGenerator(TtsEngineFactory { engine }, GeneratedAudioStore(cacheDirectory))
            val progress = mutableListOf<Double>()

            val result = generator.generate(
                GenerationRequest(
                    "First.\n\nSecond.",
                    SpeechSettings(paragraphPauseSeconds = 0.5, normalizesAudio = false, trimsSilence = false),
                ),
                modelDirectory,
                progress::add,
            )

            assertEquals(listOf("First.", "Second."), engine.requests.map(SynthesisRequest::text))
            assertTrue(result.samples.size >= 54)
            assertEquals(1.0, progress.last(), 0.001)
            assertTrue(result.wavFile.isFile)
            assertTrue(result.waveform.isNotEmpty())
        }
    }

    @Test
    fun blankTextDoesNotCreateTheNativeEngine() = runTest {
        withDirectories { modelDirectory, cacheDirectory ->
            var creations = 0
            val generator = SpeechGenerator(
                TtsEngineFactory {
                    creations += 1
                    FakeTtsEngine(shortArrayOf(), 24_000)
                },
                GeneratedAudioStore(cacheDirectory),
            )

            assertThrows(IllegalArgumentException::class.java) {
                kotlinx.coroutines.runBlocking {
                    generator.generate(GenerationRequest(" \n ", SpeechSettings()), modelDirectory) { }
                }
            }
            assertEquals(0, creations)
        }
    }

    @Test
    fun unknownVoiceFallsBackAndMetricsNeverReceiveSourceText() = runTest {
        withDirectories { modelDirectory, cacheDirectory ->
            val engine = FakeTtsEngine(shortArrayOf(1_000), 24_000)
            val metrics = mutableListOf<GenerationMetric>()
            val generator = SpeechGenerator(
                TtsEngineFactory { engine },
                GeneratedAudioStore(cacheDirectory),
                metrics::add,
            )

            generator.generate(
                GenerationRequest("Private sentence", SpeechSettings(voiceId = "not-a-voice")),
                modelDirectory,
            ) { }

            assertEquals("af_heart", engine.requests.single().voiceId)
            assertEquals("af_heart", metrics.single().voiceId)
            assertEquals(1, metrics.single().sampleCount)
        }
    }

    @Test
    fun failurePreservesPriorArtifactAndDiscardsTheEngine() = runTest {
        withDirectories { modelDirectory, cacheDirectory ->
            val store = GeneratedAudioStore(cacheDirectory)
            val prior = store.publish(byteArrayOf(1, 2, 3))
            val engines = mutableListOf<FakeTtsEngine>()
            val generator = SpeechGenerator(
                TtsEngineFactory {
                    FakeTtsEngine(shortArrayOf(), 24_000, failure = IllegalStateException("inference failed"))
                        .also(engines::add)
                },
                store,
            )

            repeat(2) {
                runCatching {
                    generator.generate(GenerationRequest("Hello", SpeechSettings()), modelDirectory) { }
                }
            }

            assertTrue(prior.isFile)
            assertEquals(2, engines.size)
            assertTrue(engines.all(FakeTtsEngine::closed))
            assertEquals(1, cacheDirectory.listFiles()?.count { it.extension == "wav" })
        }
    }

    private data class SynthesisRequest(val text: String, val language: String, val voiceId: String)

    private class FakeTtsEngine(
        private val samplesPerCall: ShortArray,
        private val sampleRate: Int,
        private val failure: Throwable? = null,
    ) : TtsEngine {
        val requests = mutableListOf<SynthesisRequest>()
        var closed = false

        override suspend fun synthesize(text: String, language: String, voiceId: String): PcmAudio {
            requests += SynthesisRequest(text, language, voiceId)
            failure?.let { throw it }
            return PcmAudio(samplesPerCall, sampleRate)
        }

        override fun cancel() = Unit

        override fun close() {
            closed = true
        }
    }

    private suspend fun withDirectories(block: suspend (File, File) -> Unit) {
        val root = Files.createTempDirectory("echolocal-speech-generator").toFile()
        val model = root.resolve("model").apply(File::mkdirs)
        val cache = root.resolve("cache")
        try {
            block(model, cache)
        } finally {
            root.deleteRecursively()
        }
    }
}
