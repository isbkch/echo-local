package com.isbkch.echolocal.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.isbkch.echolocal.app.EcholocalUiState
import com.isbkch.echolocal.model.ModelInstallState

data class EcholocalActions(
    val onTextChanged: (String) -> Unit = {},
    val onPaste: (String) -> Unit = {},
    val onClear: () -> Unit = {},
    val onSelectVoice: (String) -> Unit = {},
    val onParagraphPauseChanged: (Double) -> Unit = {},
    val onNormalizeChanged: (Boolean) -> Unit = {},
    val onTrimChanged: (Boolean) -> Unit = {},
    val onAutoplayChanged: (Boolean) -> Unit = {},
    val onDownloadModel: () -> Unit = {},
    val onGenerate: () -> Unit = {},
    val onCancelGeneration: () -> Unit = {},
    val onPlayPause: () -> Unit = {},
    val onSeek: (Long) -> Unit = {},
    val onShare: () -> Unit = {},
    val onOpenLicenses: () -> Unit = {},
    val dismissError: () -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcholocalApp(
    state: EcholocalUiState,
    actions: EcholocalActions,
    layoutModeOverride: EcholocalLayoutMode? = null,
) {
    var showsVoiceDirection by rememberSaveable { mutableStateOf(false) }
    val layoutMode = layoutModeOverride ?: LocalEcholocalLayoutMode.current

    when (layoutMode) {
        EcholocalLayoutMode.Compact -> CompactEcholocalScreen(
            state = state,
            actions = actions,
            onVoiceDirection = { showsVoiceDirection = true },
        )
        EcholocalLayoutMode.Expanded -> ExpandedEcholocalScreen(state, actions)
    }

    if (showsVoiceDirection && layoutMode == EcholocalLayoutMode.Compact) {
        ModalBottomSheet(onDismissRequest = { showsVoiceDirection = false }) {
            VoiceDirection(
                state = state,
                actions = actions,
                onDone = { showsVoiceDirection = false },
            )
        }
    }

    if (state.modelState !is ModelInstallState.Ready) {
        ModelSetupOverlay(state.modelState, actions.onDownloadModel)
    }
}
