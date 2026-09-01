package com.isbkch.echolocal.app

import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import com.isbkch.echolocal.audio.AudioPlayer
import com.isbkch.echolocal.audio.PlaybackSnapshot
import com.isbkch.echolocal.data.SettingsRepository
import com.isbkch.echolocal.domain.SpeechSettings
import com.isbkch.echolocal.model.ModelInstallState
import com.isbkch.echolocal.model.ModelRepository
import com.isbkch.echolocal.tts.GenerationRequest
import com.isbkch.echolocal.tts.GenerationResult
import com.isbkch.echolocal.tts.SpeechGenerating
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EcholocalViewModelTest {
    @Test
    fun generateIsEnabledOnlyForReadyModelAndNonblankText() = runTest {
        withHarness { harness ->
            harness.model.state.value = ModelInstallState.Ready(harness.modelDirectory)
            harness.viewModel.onTextChanged("Hello")
            advanceUntilIdle()
            assertTrue(harness.viewModel.state.value.canGenerate)

            harness.viewModel.onTextChanged("  ")
            assertFalse(harness.viewModel.state.value.canGenerate)
        }
    }

    @Test
    fun failedRegenerationKeepsLastGoodAudio() = runTest {
        withHarness { harness ->
            harness.readyWithText("Hello")
            advanceUntilIdle()
            harness.viewModel.onGenerate()
            advanceUntilIdle()
            val success = harness.result("first.wav")
            harness.generator.complete(0, Result.success(success))
            advanceUntilIdle()

            harness.viewModel.onGenerate()
            advanceUntilIdle()
            harness.generator.complete(1, Result.failure(IllegalStateException("boom")))
            advanceUntilIdle()

            assertEquals(success.wavFile, harness.viewModel.state.value.audio?.wavFile)
            assertNotNull(harness.viewModel.state.value.errorMessage)
        }
    }

    @Test
    fun staleGenerationCannotReplaceNewerResult() = runTest {
        withHarness { harness ->
            harness.readyWithText("Older text")
            advanceUntilIdle()
            harness.viewModel.onGenerate()
            advanceUntilIdle()
            harness.viewModel.onTextChanged("Newer text")
            harness.viewModel.onGenerate()
            advanceUntilIdle()

            val oldResult = harness.result("old.wav")
            val newResult = harness.result("new.wav")
            harness.generator.complete(0, Result.success(oldResult))
            advanceUntilIdle()
            assertTrue(harness.viewModel.state.value.audio?.wavFile != oldResult.wavFile)

            harness.generator.complete(1, Result.success(newResult))
            advanceUntilIdle()
            assertEquals(newResult.wavFile, harness.viewModel.state.value.audio?.wavFile)
        }
    }

    @Test
    fun cancellationStopsEngineWithoutBecomingFailure() = runTest {
        withHarness { harness ->
            harness.readyWithText("Hello")
            advanceUntilIdle()
            harness.viewModel.onGenerate()
            advanceUntilIdle()

            harness.viewModel.onCancelGeneration()
            advanceUntilIdle()

            assertTrue(harness.generator.cancelCalled)
            assertEquals(GenerationState.Idle, harness.viewModel.state.value.generationState)
            assertNull(harness.viewModel.state.value.errorMessage)
        }
    }

    private suspend fun kotlinx.coroutines.test.TestScope.withHarness(block: suspend (Harness) -> Unit) {
        val root = Files.createTempDirectory("echolocal-view-model").toFile()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val viewModelScope = CoroutineScope(SupervisorJob() + dispatcher)
        var generator: ControllableGenerator? = null
        try {
            val modelDirectory = root.resolve("model").apply(File::mkdirs)
            val model = FakeModelRepository()
            generator = ControllableGenerator()
            val audioPlayer = FakeAudioPlayer()
            val viewModel = EcholocalViewModel(
                savedStateHandle = SavedStateHandle(),
                settingsRepository = FakeSettingsRepository(),
                modelRepository = model,
                generator = generator,
                audioPlayer = audioPlayer,
                shareIntent = { Intent(Intent.ACTION_SEND) },
                scope = viewModelScope,
                mainDispatcher = dispatcher,
                generationDispatcher = dispatcher,
            )
            advanceUntilIdle()
            block(Harness(viewModel, model, generator, modelDirectory, root))
        } finally {
            generator?.finishPending()
            viewModelScope.cancel()
            root.deleteRecursively()
        }
    }

    private data class Harness(
        val viewModel: EcholocalViewModel,
        val model: FakeModelRepository,
        val generator: ControllableGenerator,
        val modelDirectory: File,
        val root: File,
    ) {
        fun readyWithText(text: String) {
            model.state.value = ModelInstallState.Ready(modelDirectory)
            viewModel.onTextChanged(text)
        }

        fun result(name: String): GenerationResult {
            val wav = root.resolve(name).apply { writeBytes(byteArrayOf(1)) }
            return GenerationResult(floatArrayOf(0f), 1_000, floatArrayOf(0f), wav, 12)
        }
    }

    private class FakeSettingsRepository : SettingsRepository {
        override val settings = MutableStateFlow(SpeechSettings())
        override suspend fun save(settings: SpeechSettings) {
            this.settings.value = settings
        }
    }

    private class FakeModelRepository : ModelRepository {
        override val state = MutableStateFlow<ModelInstallState>(ModelInstallState.Missing)
        override fun refresh() = Unit
        override fun startDownload() = Unit
        override fun readyDirectory(): File? = (state.value as? ModelInstallState.Ready)?.directory
    }

    private class ControllableGenerator : SpeechGenerating {
        private data class Call(val result: CompletableDeferred<GenerationResult>)
        private val calls = mutableListOf<Call>()
        var cancelCalled = false

        override suspend fun generate(
            request: GenerationRequest,
            modelDirectory: File,
            onProgress: (Double) -> Unit,
        ): GenerationResult {
            val call = Call(CompletableDeferred())
            calls += call
            return withContext(NonCancellable) { call.result.await() }
        }

        override fun cancel() {
            cancelCalled = true
        }

        fun complete(index: Int, result: Result<GenerationResult>) {
            result.fold(calls[index].result::complete, calls[index].result::completeExceptionally)
        }

        fun finishPending() {
            calls.forEach { it.result.completeExceptionally(CancellationException("test complete")) }
        }
    }

    private class FakeAudioPlayer : AudioPlayer {
        override val snapshot: StateFlow<PlaybackSnapshot> = MutableStateFlow(PlaybackSnapshot())
        override fun load(file: File) = Unit
        override fun playPause() = Unit
        override fun seekTo(positionMillis: Long) = Unit
        override fun stop() = Unit
        override fun close() = Unit
    }
}
