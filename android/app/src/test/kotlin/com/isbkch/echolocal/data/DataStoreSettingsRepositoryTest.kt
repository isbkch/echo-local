package com.isbkch.echolocal.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.isbkch.echolocal.domain.SpeechSettings
import java.nio.file.Files
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DataStoreSettingsRepositoryTest {
    @Test
    fun defaultsMatchTheProductSettings() = withRepository { repository, _ ->
        assertEquals(SpeechSettings(), repository.settings.first())
    }

    @Test
    fun savedVoiceRoundTrips() = withRepository { repository, _ ->
        repository.save(SpeechSettings(voiceId = "bf_emma", paragraphPauseSeconds = 0.7))

        assertEquals("bf_emma", repository.settings.first().voiceId)
        assertEquals(0.7, repository.settings.first().paragraphPauseSeconds, 0.001)
    }

    @Test
    fun unknownVoiceFallsBackWithoutDeletingOtherSettings() = withRepository { repository, preferences ->
        preferences.edit {
            it[stringPreferencesKey("voice")] = "unknown"
            it[androidx.datastore.preferences.core.doublePreferencesKey("paragraph_pause")] = 0.9
            it[androidx.datastore.preferences.core.booleanPreferencesKey("normalize")] = false
        }

        val settings = repository.settings.first()

        assertEquals("af_heart", settings.voiceId)
        assertEquals(0.9, settings.paragraphPauseSeconds, 0.001)
        assertEquals(false, settings.normalizesAudio)
    }

    private fun withRepository(block: suspend (DataStoreSettingsRepository, DataStore<Preferences>) -> Unit) = runTest {
        val root = Files.createTempDirectory("echolocal-settings").toFile()
        try {
            val preferences = PreferenceDataStoreFactory.create(scope = backgroundScope) {
                root.resolve("settings.preferences_pb")
            }
            block(DataStoreSettingsRepository(preferences), preferences)
        } finally {
            root.deleteRecursively()
        }
    }
}
