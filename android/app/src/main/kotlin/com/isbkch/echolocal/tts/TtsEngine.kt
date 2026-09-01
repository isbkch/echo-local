package com.isbkch.echolocal.tts

import java.io.File

interface TtsEngine : AutoCloseable {
    suspend fun synthesize(text: String, language: String, voiceId: String): PcmAudio
    fun cancel()
}

fun interface TtsEngineFactory {
    fun create(modelDirectory: File): TtsEngine
}
