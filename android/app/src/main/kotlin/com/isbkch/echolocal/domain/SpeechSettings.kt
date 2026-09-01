package com.isbkch.echolocal.domain

data class SpeechSettings(
    val voiceId: String = "af_heart",
    val paragraphPauseSeconds: Double = 0.42,
    val normalizesAudio: Boolean = true,
    val trimsSilence: Boolean = true,
    val autoPlays: Boolean = true,
)
