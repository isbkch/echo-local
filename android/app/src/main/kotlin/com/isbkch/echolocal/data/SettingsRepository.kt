package com.isbkch.echolocal.data

import com.isbkch.echolocal.domain.SpeechSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<SpeechSettings>
    suspend fun save(settings: SpeechSettings)
}
