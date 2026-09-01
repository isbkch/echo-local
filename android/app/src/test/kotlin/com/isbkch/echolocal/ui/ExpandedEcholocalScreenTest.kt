package com.isbkch.echolocal.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.isbkch.echolocal.app.EcholocalUiState
import com.isbkch.echolocal.model.ModelInstallState
import com.isbkch.echolocal.ui.theme.EcholocalTheme
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExpandedEcholocalScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun expandedLayoutKeepsVoiceDirectionVisible() {
        compose.setContent {
            EcholocalTheme {
                EcholocalApp(readyState(), EcholocalActions(), EcholocalLayoutMode.Expanded)
            }
        }

        compose.onNodeWithText("SCRIPT").assertIsDisplayed()
        compose.onNodeWithText("VOICE").assertIsDisplayed()
        compose.onNodeWithText("Heart").assertIsDisplayed()
        compose.onNodeWithContentDescription("Voice direction").assertDoesNotExist()
    }

    @Test
    fun compactLayoutKeepsVoiceDirectionBehindButton() {
        compose.setContent {
            EcholocalTheme {
                EcholocalApp(readyState(), EcholocalActions(), EcholocalLayoutMode.Compact)
            }
        }

        compose.onNodeWithContentDescription("Voice direction").assertIsDisplayed()
    }

    private fun readyState() = EcholocalUiState(
        text = "A wider page.",
        modelState = ModelInstallState.Ready(File("/verified/model")),
    )
}
