package com.isbkch.echolocal.tts

data class PcmAudio(
    val samples: ShortArray,
    val sampleRate: Int,
)
