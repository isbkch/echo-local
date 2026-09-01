package com.isbkch.echolocal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.isbkch.echolocal.app.EcholocalUiState
import com.isbkch.echolocal.app.GenerationState
import com.isbkch.echolocal.audio.PlaybackStatus
import com.isbkch.echolocal.ui.EcholocalActions
import com.isbkch.echolocal.ui.theme.LocalEcholocalColors

@Composable
fun ListeningRail(state: EcholocalUiState, actions: EcholocalActions, modifier: Modifier = Modifier) {
    val colors = LocalEcholocalColors.current
    val duration = state.playback.durationMillis.takeIf { it > 0 } ?: state.audio?.durationMillis ?: 0L
    val position = state.playback.positionMillis.coerceIn(0L, duration.coerceAtLeast(0L))
    val progress = if (duration > 0) position.toFloat() / duration else 0f
    val generating = state.generationState is GenerationState.Generating || state.generationState is GenerationState.Finishing
    Column(
        modifier.fillMaxWidth().background(colors.raised).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(statusText(state), color = colors.secondaryInk, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text("●", color = colors.secondaryInk, fontSize = 9.sp)
            Text("On-device", color = colors.secondaryInk, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 5.dp))
        }
        Box(Modifier.fillMaxWidth().height(34.dp)) {
            EcholocalWaveform(state.audio?.waveform ?: floatArrayOf(), progress, Modifier.matchParentSize())
            Slider(
                value = position.toFloat(),
                onValueChange = { actions.onSeek(it.toLong()) },
                valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
                enabled = state.audio != null,
                modifier = Modifier.matchParentSize().alpha(0.01f).semantics {
                    contentDescription = "Playback position"
                },
            )
        }
        Row(Modifier.fillMaxWidth()) {
            Text(formatMillis(position), fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = colors.secondaryInk)
            Spacer(Modifier.weight(1f))
            Text(formatMillis(duration), fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = colors.secondaryInk)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = actions.onPlayPause,
                enabled = state.audio != null,
                modifier = Modifier.size(48.dp).semantics {
                    contentDescription = if (state.playback.status == PlaybackStatus.Playing) "Pause" else "Play"
                },
                shape = CircleShape,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.ink, contentColor = colors.paper),
            ) { Text(if (state.playback.status == PlaybackStatus.Playing) "Ⅱ" else "▶", fontSize = 15.sp) }
            Spacer(Modifier.weight(1f))
            EcholocalQuietButton(actions.onShare, enabled = state.audio != null) { Text("Share WAV") }
            EcholocalPrimaryButton(
                onClick = if (generating) actions.onCancelGeneration else actions.onGenerate,
                enabled = if (generating) true else state.canGenerate,
            ) {
                Text(
                    when {
                        generating -> "Cancel"
                        state.audio != null -> "Regenerate"
                        else -> "Generate"
                    },
                )
            }
        }
    }
}

private fun statusText(state: EcholocalUiState): String = when (val generation = state.generationState) {
    is GenerationState.Generating -> generation.progress?.let { "Generating ${ (it * 100).toInt() }%" } ?: "Generating speech…"
    GenerationState.Finishing -> "Finishing audio…"
    is GenerationState.Ready -> "Ready to listen"
    is GenerationState.Failed -> generation.message
    GenerationState.Idle -> if (state.audio == null) "Ready for text" else "Ready to listen"
}

private fun formatMillis(value: Long): String {
    val totalSeconds = (value / 1_000L).coerceAtLeast(0L)
    return "%d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}
