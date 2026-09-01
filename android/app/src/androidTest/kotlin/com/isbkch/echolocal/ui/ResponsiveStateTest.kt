package com.isbkch.echolocal.ui

import android.content.Intent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.isbkch.echolocal.app.EcholocalViewModel
import com.isbkch.echolocal.audio.AudioPlayer
import com.isbkch.echolocal.audio.PlaybackSnapshot
import com.isbkch.echolocal.data.SettingsRepository
import com.isbkch.echolocal.domain.SpeechSettings
import com.isbkch.echolocal.model.ModelInstallState
import com.isbkch.echolocal.model.ModelRepository
import com.isbkch.echolocal.tts.GenerationRequest
import com.isbkch.echolocal.tts.GenerationResult
import com.isbkch.echolocal.tts.SpeechGenerating
import com.isbkch.echolocal.ui.theme.EcholocalTheme
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals

@RunWith(AndroidJUnit4::class)
class ResponsiveStateTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun compactToExpandedKeepsEditorAndVoiceInTheSameViewModel() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val modelDirectory = File(context.cacheDir, "responsive-model").apply(File::mkdirs)
        val settings = FakeSettingsRepository()
        val model = FakeModelRepository(modelDirectory)
        val viewModel = EcholocalViewModel(
            SavedStateHandle(),
            settings,
            model,
            NeverGenerating,
            FakeAudioPlayer(),
            { Intent(Intent.ACTION_SEND) },
        )
        val layoutMode = mutableStateOf(EcholocalLayoutMode.Compact)

        compose.setContent {
            val state by viewModel.state.collectAsState()
            EcholocalTheme {
                EcholocalApp(
                    state,
                    EcholocalActions(
                        onTextChanged = viewModel::onTextChanged,
                        onSelectVoice = viewModel::onSelectVoice,
                    ),
                    layoutMode.value,
                )
            }
        }
        compose.waitUntil { viewModel.state.value.modelState is ModelInstallState.Ready }
        compose.onNodeWithContentDescription("Text to turn into speech").performTextInput("Folded draft")
        compose.onNodeWithContentDescription("Voice direction").performClick()
        compose.onNodeWithText("Emma").performClick()

        compose.runOnIdle { layoutMode.value = EcholocalLayoutMode.Expanded }

        compose.onNodeWithContentDescription("Text to turn into speech").assertTextContains("Folded draft")
        compose.onNodeWithText("Emma").assertIsDisplayed()
        assertEquals("bf_emma", viewModel.state.value.settings.voiceId)
    }

    private class FakeSettingsRepository : SettingsRepository {
        override val settings = MutableStateFlow(SpeechSettings())
        override suspend fun save(settings: SpeechSettings) {
            this.settings.value = settings
        }
    }

    private class FakeModelRepository(directory: File) : ModelRepository {
        override val state = MutableStateFlow<ModelInstallState>(ModelInstallState.Ready(directory))
        override fun refresh() = Unit
        override fun startDownload() = Unit
        override fun readyDirectory(): File? = (state.value as ModelInstallState.Ready).directory
    }

    private object NeverGenerating : SpeechGenerating {
        override suspend fun generate(
            request: GenerationRequest,
            modelDirectory: File,
            onProgress: (Double) -> Unit,
        ): GenerationResult = error("Generation is outside this layout test.")

        override fun cancel() = Unit
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
