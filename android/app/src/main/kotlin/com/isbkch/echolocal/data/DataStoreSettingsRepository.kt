package com.isbkch.echolocal.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.isbkch.echolocal.domain.SpeechSettings
import com.isbkch.echolocal.domain.VoiceCatalog
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class DataStoreSettingsRepository(
    private val preferences: DataStore<Preferences>,
) : SettingsRepository {
    override val settings: Flow<SpeechSettings> = preferences.data
        .catch { failure ->
            if (failure is IOException) emit(emptyPreferences()) else throw failure
        }
        .map { values ->
            val defaults = SpeechSettings()
            SpeechSettings(
                voiceId = values[VOICE]
                    ?.takeIf { candidate -> VoiceCatalog.curated.any { it.id == candidate } }
                    ?: defaults.voiceId,
                paragraphPauseSeconds = values[PARAGRAPH_PAUSE] ?: defaults.paragraphPauseSeconds,
                normalizesAudio = values[NORMALIZE] ?: defaults.normalizesAudio,
                trimsSilence = values[TRIM] ?: defaults.trimsSilence,
                autoPlays = values[AUTOPLAY] ?: defaults.autoPlays,
            )
        }

    override suspend fun save(settings: SpeechSettings) {
        preferences.edit { values ->
            values[VOICE] = VoiceCatalog.voice(settings.voiceId).id
            values[PARAGRAPH_PAUSE] = settings.paragraphPauseSeconds
            values[NORMALIZE] = settings.normalizesAudio
            values[TRIM] = settings.trimsSilence
            values[AUTOPLAY] = settings.autoPlays
        }
    }

    private companion object {
        val VOICE = stringPreferencesKey("voice")
        val PARAGRAPH_PAUSE = doublePreferencesKey("paragraph_pause")
        val NORMALIZE = booleanPreferencesKey("normalize")
        val TRIM = booleanPreferencesKey("trim")
        val AUTOPLAY = booleanPreferencesKey("autoplay")
    }
}
