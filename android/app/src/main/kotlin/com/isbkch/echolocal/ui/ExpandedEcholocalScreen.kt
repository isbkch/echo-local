package com.isbkch.echolocal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.isbkch.echolocal.app.EcholocalUiState
import com.isbkch.echolocal.ui.theme.LocalEcholocalColors

@Composable
fun ExpandedEcholocalScreen(state: EcholocalUiState, actions: EcholocalActions) {
    val colors = LocalEcholocalColors.current
    Row(Modifier.fillMaxSize().background(colors.canvas)) {
        Box(Modifier.weight(1.65f).fillMaxHeight()) {
            CompactEcholocalScreen(
                state = state,
                actions = actions,
                onVoiceDirection = {},
                showDirectionButton = false,
            )
        }
        VerticalDivider(Modifier.fillMaxHeight().width(0.5.dp), color = colors.line)
        Box(Modifier.weight(1f).fillMaxHeight().background(colors.canvas)) {
            VoiceDirectionContent(state, actions)
        }
    }
}
