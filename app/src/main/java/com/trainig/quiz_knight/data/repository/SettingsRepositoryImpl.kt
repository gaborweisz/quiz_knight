package com.trainig.quiz_knight.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.trainig.quiz_knight.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private object Keys {
        val MUSIC_ENABLED = booleanPreferencesKey("music_enabled")
        val INTRO_SHOWN = booleanPreferencesKey("intro_shown")
    }

    override fun observeMusicEnabled(): Flow<Boolean> =
        context.settingsDataStore.data.map { prefs ->
            prefs[Keys.MUSIC_ENABLED] ?: true   // on by default
        }

    override suspend fun setMusicEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.MUSIC_ENABLED] = enabled
        }
    }

    override fun observeIntroShown(): Flow<Boolean> =
        context.settingsDataStore.data.map { prefs ->
            prefs[Keys.INTRO_SHOWN] ?: false
        }

    override suspend fun setIntroShown(shown: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.INTRO_SHOWN] = shown
        }
    }
}
