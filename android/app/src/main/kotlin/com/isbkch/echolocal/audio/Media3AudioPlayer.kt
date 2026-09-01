package com.isbkch.echolocal.audio

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class Media3AudioPlayer(
    context: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
    private val ownsScope: Boolean = true,
) : AudioPlayer {
    private val player = ExoPlayer.Builder(context.applicationContext).build()
    private val mutableSnapshot = MutableStateFlow(PlaybackSnapshot())
    override val snapshot: StateFlow<PlaybackSnapshot> = mutableSnapshot.asStateFlow()
    private var positionJob: Job? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    updateSnapshot(PlaybackStatus.Playing)
                    startPositionUpdates()
                } else {
                    positionJob?.cancel()
                    if (mutableSnapshot.value.status == PlaybackStatus.Playing) {
                        updateSnapshot(PlaybackStatus.Paused)
                    }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> updateSnapshot(
                        if (player.isPlaying) PlaybackStatus.Playing else PlaybackStatus.Ready,
                    )
                    Player.STATE_ENDED -> {
                        player.pause()
                        player.seekTo(0)
                        updateSnapshot(PlaybackStatus.Ready, positionMillis = 0)
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                positionJob?.cancel()
                mutableSnapshot.value = currentSnapshot(
                    status = PlaybackStatus.Ready,
                    errorMessage = error.message ?: "Unable to play generated audio.",
                )
            }
        })
    }

    override fun load(file: File) {
        require(file.isFile) { "Generated audio is missing." }
        positionJob?.cancel()
        mutableSnapshot.value = PlaybackSnapshot(status = PlaybackStatus.Ready)
        player.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
        player.prepare()
    }

    override fun playPause() {
        if (player.currentMediaItem == null) return
        if (player.isPlaying) player.pause() else player.play()
    }

    override fun seekTo(positionMillis: Long) {
        if (player.currentMediaItem == null) return
        val duration = playableDuration()
        player.seekTo(positionMillis.coerceIn(0L, duration))
        updateSnapshot(mutableSnapshot.value.status, positionMillis.coerceIn(0L, duration))
    }

    override fun stop() {
        if (player.currentMediaItem == null) return
        player.pause()
        player.seekTo(0)
        updateSnapshot(PlaybackStatus.Ready, positionMillis = 0)
    }

    override fun close() {
        positionJob?.cancel()
        player.release()
        mutableSnapshot.value = PlaybackSnapshot()
        if (ownsScope) scope.cancel()
    }

    private fun startPositionUpdates() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (isActive && player.isPlaying) {
                updateSnapshot(PlaybackStatus.Playing)
                delay(POSITION_SAMPLE_MILLIS)
            }
        }
    }

    private fun updateSnapshot(status: PlaybackStatus, positionMillis: Long = player.currentPosition) {
        mutableSnapshot.value = currentSnapshot(status, positionMillis = positionMillis)
    }

    private fun currentSnapshot(
        status: PlaybackStatus,
        positionMillis: Long = player.currentPosition,
        errorMessage: String? = null,
    ) = PlaybackSnapshot(
        status = status,
        positionMillis = positionMillis.coerceIn(0L, playableDuration()),
        durationMillis = playableDuration(),
        errorMessage = errorMessage,
    )

    private fun playableDuration(): Long = player.duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: 0L

    companion object {
        private const val POSITION_SAMPLE_MILLIS = 50L
    }
}
