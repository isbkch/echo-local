package com.isbkch.echolocal.app

import android.content.Intent
import com.isbkch.echolocal.audio.PlaybackSnapshot
import com.isbkch.echolocal.domain.SpeechSettings
import com.isbkch.echolocal.domain.TextMetrics
import com.isbkch.echolocal.model.ModelInstallState
import java.io.File

sealed interface GenerationState {
    data object Idle : GenerationState
    data class Generating(val progress: Double?) : GenerationState
    data object Finishing : GenerationState
    data class Ready(val durationMillis: Long, val elapsedMillis: Long) : GenerationState
    data class Failed(val message: String) : GenerationState
}

data class GeneratedAudioUi(
    val wavFile: File,
    val waveform: FloatArray,
    val durationMillis: Long,
)

data class EcholocalUiState(
    val text: String = "",
    val settings: SpeechSettings = SpeechSettings(),
    val metrics: TextMetrics = TextMetrics.from(""),
    val modelState: ModelInstallState = ModelInstallState.Checking,
    val generationState: GenerationState = GenerationState.Idle,
    val audio: GeneratedAudioUi? = null,
    val playback: PlaybackSnapshot = PlaybackSnapshot(),
    val errorMessage: String? = null,
) {
    val canGenerate: Boolean get() =
        modelState is ModelInstallState.Ready &&
            text.isNotBlank() &&
            generationState !is GenerationState.Generating &&
            generationState !is GenerationState.Finishing
}

sealed interface EcholocalEvent {
    data class Share(val intent: Intent) : EcholocalEvent
}
