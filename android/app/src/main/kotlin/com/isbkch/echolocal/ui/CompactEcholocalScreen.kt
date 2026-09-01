package com.isbkch.echolocal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.isbkch.echolocal.app.EcholocalUiState
import com.isbkch.echolocal.ui.components.EcholocalQuietButton
import com.isbkch.echolocal.ui.components.EcholocalWordmark
import com.isbkch.echolocal.ui.components.ListeningRail
import com.isbkch.echolocal.ui.components.ScriptEditor
import com.isbkch.echolocal.ui.theme.EcholocalAccent
import com.isbkch.echolocal.ui.theme.LocalEcholocalColors
import kotlin.math.roundToInt

@Composable
fun CompactEcholocalScreen(
    state: EcholocalUiState,
    actions: EcholocalActions,
    onVoiceDirection: () -> Unit,
    showDirectionButton: Boolean = true,
) {
    val colors = LocalEcholocalColors.current
    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            actions.dismissError()
        }
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colors.paper,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { ListeningRail(state, actions) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).background(colors.paper)) {
            Row(
                Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EcholocalWordmark()
                Spacer(Modifier.weight(1f))
                if (showDirectionButton) {
                    TextButton(
                        onClick = onVoiceDirection,
                        modifier = Modifier.semantics { contentDescription = "Voice direction" },
                    ) { Text("Direction", color = EcholocalAccent, fontWeight = FontWeight.SemiBold) }
                }
            }
            HorizontalDivider(thickness = 0.5.dp, color = colors.line)
            Row(
                Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("SCRIPT", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp, color = colors.secondaryInk)
                Spacer(Modifier.weight(1f))
                EcholocalQuietButton(onClick = {
                    clipboard.getText()?.text?.let(actions.onPaste)
                }) { Text("Paste") }
                Spacer(Modifier.padding(horizontal = 4.dp))
                EcholocalQuietButton(onClick = actions.onClear, enabled = state.text.isNotEmpty()) { Text("Clear") }
            }
            HorizontalDivider(thickness = 0.5.dp, color = colors.line)
            ScriptEditor(state.text, actions.onTextChanged, Modifier.weight(1f))
            HorizontalDivider(thickness = 0.5.dp, color = colors.line)
            Row(
                Modifier.fillMaxWidth().height(38.dp).padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${state.metrics.wordCount} words", fontSize = 10.sp, color = colors.secondaryInk)
                Text("${state.metrics.characterCount} characters", fontSize = 10.sp, color = colors.secondaryInk, modifier = Modifier.padding(start = 14.dp))
                Spacer(Modifier.weight(1f))
                if (state.metrics.estimatedDurationSeconds > 0) {
                    Text(
                        "about ${formatDuration(state.metrics.estimatedDurationSeconds)}",
                        fontSize = 10.sp,
                        color = colors.secondaryInk,
                    )
                }
            }
        }
    }
}

private fun formatDuration(seconds: Double): String {
    val rounded = seconds.roundToInt().coerceAtLeast(0)
    return "%d:%02d".format(rounded / 60, rounded % 60)
}
