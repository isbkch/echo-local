package com.isbkch.echolocal.audio

import java.io.File
import kotlinx.coroutines.flow.StateFlow

enum class PlaybackStatus { Empty, Ready, Playing, Paused }

data class PlaybackSnapshot(
    val status: PlaybackStatus = PlaybackStatus.Empty,
    val positionMillis: Long = 0,
    val durationMillis: Long = 0,
    val errorMessage: String? = null,
)

interface AudioPlayer : AutoCloseable {
    val snapshot: StateFlow<PlaybackSnapshot>
    fun load(file: File)
    fun playPause()
    fun seekTo(positionMillis: Long)
    fun stop()
}
