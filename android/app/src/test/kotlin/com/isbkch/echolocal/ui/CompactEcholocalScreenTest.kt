package com.isbkch.echolocal.ui

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.isbkch.echolocal.app.EcholocalUiState
import com.isbkch.echolocal.domain.SpeechSettings
import com.isbkch.echolocal.model.ModelInstallState
import com.isbkch.echolocal.ui.theme.EcholocalTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CompactEcholocalScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun missingModelBlocksEditorWithExplicitSetup() {
        compose.setContent {
            EcholocalTheme {
                EcholocalApp(
                    state = EcholocalUiState(modelState = ModelInstallState.Missing),
                    actions = EcholocalActions(),
                    expanded = false,
                )
            }
        }

        compose.onNodeWithText("Private speech,\non this device.").assertIsDisplayed()
        compose.onNodeWithText("Download local model").assertHasClickAction()
    }

    @Test
    fun compactScreenShowsEditorAndListeningRailWhenReady() {
        compose.setContent {
            EcholocalTheme {
                EcholocalApp(
                    state = readyState(text = "A page worth hearing."),
                    actions = EcholocalActions(),
                    expanded = false,
                )
            }
        }

        compose.onNodeWithText("SCRIPT").assertIsDisplayed()
        compose.onNodeWithContentDescription("Text to turn into speech").assertIsDisplayed()
        compose.onNodeWithText("Generate").assertIsEnabled()
        compose.onNodeWithText("On-device").assertIsDisplayed()
    }

    @Test
    fun voiceDirectionHasApprovedControlsAndNoPace() {
        var selectedVoice = ""
        compose.setContent {
            EcholocalTheme {
                EcholocalApp(
                    state = readyState(),
                    actions = EcholocalActions(onSelectVoice = { selectedVoice = it }),
                    expanded = false,
                )
            }
        }

        compose.onNodeWithContentDescription("Voice direction").performClick()
        compose.onNodeWithText("Emma").performClick()

        assertEquals("bf_emma", selectedVoice)
        compose.onNodeWithText("Space between paragraphs").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Even out loudness").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Clean the edges").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Play when ready").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Pace").assertDoesNotExist()
    }

    private fun readyState(text: String = "") = EcholocalUiState(
        text = text,
        settings = SpeechSettings(),
        modelState = ModelInstallState.Ready(File("/verified/model")),
    )
}
