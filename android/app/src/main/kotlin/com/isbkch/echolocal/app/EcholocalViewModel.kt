package com.isbkch.echolocal.app

import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isbkch.echolocal.audio.AudioPlayer
import com.isbkch.echolocal.data.SettingsRepository
import com.isbkch.echolocal.domain.SpeechSettings
import com.isbkch.echolocal.domain.TextMetrics
import com.isbkch.echolocal.domain.VoiceCatalog
import com.isbkch.echolocal.model.ModelInstallState
import com.isbkch.echolocal.model.ModelRepository
import com.isbkch.echolocal.tts.GenerationRequest
import com.isbkch.echolocal.tts.SpeechGenerating
import java.io.File
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EcholocalViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val settingsRepository: SettingsRepository,
    private val modelRepository: ModelRepository,
    private val generator: SpeechGenerating,
    private val audioPlayer: AudioPlayer,
    private val shareIntent: (File) -> Intent,
    scope: CoroutineScope? = null,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    private val generationDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val coroutineScope = scope ?: viewModelScope
    private val mutableState = MutableStateFlow(
        EcholocalUiState(text = savedStateHandle[TEXT_KEY] ?: "").withUpdatedMetrics(),
    )
    val state: StateFlow<EcholocalUiState> = mutableState.asStateFlow()
    private val eventChannel = Channel<EcholocalEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()
    private val generationId = AtomicLong(0)
    private var generationJob: Job? = null

    init {
        coroutineScope.launch(mainDispatcher) {
            settingsRepository.settings.collect { settings ->
                mutableState.update { it.copy(settings = settings).withUpdatedMetrics() }
            }
        }
        coroutineScope.launch(mainDispatcher) {
            modelRepository.state.collect { modelState ->
                mutableState.update { it.copy(modelState = modelState) }
            }
        }
        coroutineScope.launch(mainDispatcher) {
            audioPlayer.snapshot.collect { playback ->
                mutableState.update { it.copy(playback = playback) }
            }
        }
    }

    fun onTextChanged(text: String) {
        savedStateHandle[TEXT_KEY] = text
        mutableState.update { it.copy(text = text).withUpdatedMetrics() }
    }

    fun onPaste(text: String) = onTextChanged(text)

    fun onClear() = onTextChanged("")

    fun onSelectVoice(voiceId: String) = updateSettings {
        copy(voiceId = VoiceCatalog.voice(voiceId).id)
    }

    fun onParagraphPauseChanged(seconds: Double) = updateSettings {
        copy(paragraphPauseSeconds = seconds.coerceIn(0.0, 2.0))
    }

    fun onNormalizeChanged(enabled: Boolean) = updateSettings { copy(normalizesAudio = enabled) }

    fun onTrimChanged(enabled: Boolean) = updateSettings { copy(trimsSilence = enabled) }

    fun onAutoplayChanged(enabled: Boolean) = updateSettings { copy(autoPlays = enabled) }

    fun onDownloadModel() = modelRepository.startDownload()

    fun onGenerate() {
        val requestState = mutableState.value
        val modelDirectory = (requestState.modelState as? ModelInstallState.Ready)?.directory ?: return
        if (requestState.text.isBlank()) return

        val id = generationId.incrementAndGet()
        generationJob?.cancel()
        generator.cancel()
        mutableState.update {
            it.copy(generationState = GenerationState.Generating(null), errorMessage = null)
        }
        generationJob = coroutineScope.launch(generationDispatcher) {
            try {
                val result = generator.generate(
                    GenerationRequest(requestState.text, requestState.settings),
                    modelDirectory,
                ) { progress ->
                    if (generationId.get() == id) {
                        mutableState.update {
                            it.copy(
                                generationState = if (progress >= 1.0) {
                                    GenerationState.Finishing
                                } else {
                                    GenerationState.Generating(progress.coerceIn(0.0, 1.0))
                                },
                            )
                        }
                    }
                }
                if (generationId.get() != id) return@launch

                val durationMillis = result.samples.size.toLong() * 1_000L / result.sampleRate
                withContext(mainDispatcher) {
                    if (generationId.get() != id) return@withContext
                    audioPlayer.load(result.wavFile)
                    mutableState.update {
                        it.copy(
                            generationState = GenerationState.Ready(durationMillis, result.elapsedMillis),
                            audio = GeneratedAudioUi(result.wavFile, result.waveform, durationMillis),
                            errorMessage = null,
                        )
                    }
                    if (requestState.settings.autoPlays) audioPlayer.playPause()
                }
            } catch (_: CancellationException) {
                if (generationId.get() == id) {
                    mutableState.update { it.copy(generationState = GenerationState.Idle, errorMessage = null) }
                }
            } catch (failure: Throwable) {
                if (generationId.get() == id) {
                    val message = failure.message ?: "Speech generation failed."
                    mutableState.update {
                        it.copy(generationState = GenerationState.Failed(message), errorMessage = message)
                    }
                }
            }
        }
    }

    fun onCancelGeneration() {
        generationId.incrementAndGet()
        generationJob?.cancel()
        generationJob = null
        generator.cancel()
        mutableState.update { it.copy(generationState = GenerationState.Idle, errorMessage = null) }
    }

    fun onPlayPause() = audioPlayer.playPause()

    fun onSeek(positionMillis: Long) = audioPlayer.seekTo(positionMillis)

    fun onShare() {
        val wavFile = mutableState.value.audio?.wavFile ?: return
        runCatching { shareIntent(wavFile) }
            .onSuccess { eventChannel.trySend(EcholocalEvent.Share(it)) }
            .onFailure { failure ->
                mutableState.update {
                    it.copy(errorMessage = failure.message ?: "Unable to share generated audio.")
                }
            }
    }

    fun dismissError() {
        mutableState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        generationId.incrementAndGet()
        generationJob?.cancel()
        generator.cancel()
        audioPlayer.close()
        eventChannel.close()
        super.onCleared()
    }

    private fun updateSettings(transform: SpeechSettings.() -> SpeechSettings) {
        val updated = mutableState.value.settings.transform()
        mutableState.update { it.copy(settings = updated).withUpdatedMetrics() }
        coroutineScope.launch(generationDispatcher) { settingsRepository.save(updated) }
    }

    private fun EcholocalUiState.withUpdatedMetrics() = copy(
        metrics = TextMetrics.from(text, settings.paragraphPauseSeconds),
    )

    private companion object {
        const val TEXT_KEY = "editor_text"
    }
}
