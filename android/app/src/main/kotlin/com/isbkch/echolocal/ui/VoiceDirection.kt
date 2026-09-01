package com.isbkch.echolocal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.isbkch.echolocal.app.EcholocalUiState
import com.isbkch.echolocal.domain.VoiceCatalog
import com.isbkch.echolocal.model.ModelInstallState
import com.isbkch.echolocal.ui.theme.EcholocalAccent
import com.isbkch.echolocal.ui.theme.EcholocalSuccess
import com.isbkch.echolocal.ui.theme.LocalEcholocalColors

@Composable
fun VoiceDirection(
    state: EcholocalUiState,
    actions: EcholocalActions,
    onDone: (() -> Unit)? = null,
    showHeader: Boolean = true,
) {
    val colors = LocalEcholocalColors.current
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).background(colors.canvas)) {
        if (showHeader) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Voice direction", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                if (onDone != null) TextButton(onClick = onDone) { Text("Done") }
            }
        }
        SectionLabel("VOICE")
        Column(Modifier.background(colors.paper)) {
            VoiceCatalog.curated.forEach { voice ->
                Row(
                    Modifier.fillMaxWidth().clickable { actions.onSelectVoice(voice.id) }
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.compose.foundation.layout.Box(
                        Modifier.size(38.dp).clip(CircleShape)
                            .background(if (voice.id == state.settings.voiceId) colors.accentSoft else colors.raised),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(voice.name.take(1), fontFamily = FontFamily.Serif, color = if (voice.id == state.settings.voiceId) EcholocalAccent else colors.secondaryInk)
                    }
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(voice.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("${voice.character} · ${voice.region}", fontSize = 11.sp, color = colors.secondaryInk)
                    }
                    Spacer(Modifier.weight(1f))
                    if (voice.id == state.settings.voiceId) Text("✓", color = EcholocalAccent, fontWeight = FontWeight.Bold)
                }
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = colors.line)
        SectionLabel("DELIVERY")
        Column(Modifier.fillMaxWidth().background(colors.paper).padding(18.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text("Space between paragraphs", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text("%.2fs".format(state.settings.paragraphPauseSeconds), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = colors.secondaryInk)
            }
            Slider(
                value = state.settings.paragraphPauseSeconds.toFloat().coerceIn(0.15f, 0.9f),
                onValueChange = { actions.onParagraphPauseChanged(it.toDouble()) },
                valueRange = 0.15f..0.9f,
            )
        }
        HorizontalDivider(thickness = 0.5.dp, color = colors.line)
        SectionLabel("FINISH")
        Column(Modifier.background(colors.paper).padding(horizontal = 18.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            SettingToggle("Even out loudness", "Bring the peak to a clean, consistent level.", state.settings.normalizesAudio, actions.onNormalizeChanged)
            SettingToggle("Clean the edges", "Trim excess silence around each passage.", state.settings.trimsSilence, actions.onTrimChanged)
            SettingToggle("Play when ready", "Start listening as soon as speech is generated.", state.settings.autoPlays, actions.onAutoplayChanged)
        }
        HorizontalDivider(thickness = 0.5.dp, color = colors.line)
        Row(Modifier.fillMaxWidth().background(colors.paper).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("●", color = if (state.modelState is ModelInstallState.Ready) EcholocalSuccess else colors.faintInk)
            Column(Modifier.padding(start = 9.dp)) {
                Text("Kokoro is on this device", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("Generation needs no network connection.", fontSize = 11.sp, color = colors.secondaryInk)
            }
        }
        TextButton(onClick = actions.onOpenLicenses, modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text("Open-source licenses")
        }
    }
}

@Composable
fun VoiceDirectionContent(state: EcholocalUiState, actions: EcholocalActions) {
    VoiceDirection(state = state, actions = actions, showHeader = false)
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp, color = LocalEcholocalColors.current.secondaryInk)
}

@Composable
private fun SettingToggle(title: String, detail: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(detail, fontSize = 11.sp, color = LocalEcholocalColors.current.secondaryInk)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(checkedTrackColor = EcholocalAccent),
        )
    }
}
