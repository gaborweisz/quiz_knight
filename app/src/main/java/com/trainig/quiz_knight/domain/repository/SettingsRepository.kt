package com.trainig.quiz_knight.domain.repository

import com.trainig.quiz_knight.domain.model.AppLanguage
import kotlinx.coroutines.flow.Flow

/**
 * Persists user-facing settings (e.g. music on/off, whether intro was shown, language).
 */
interface SettingsRepository {
    fun observeMusicEnabled(): Flow<Boolean>
    suspend fun setMusicEnabled(enabled: Boolean)

    // Whether the Intro screen has already been shown (so it won't appear on subsequent launches)
    fun observeIntroShown(): Flow<Boolean>
    suspend fun setIntroShown(shown: Boolean)

    // The app's selected UI/quiz content language.
    fun observeLanguage(): Flow<AppLanguage>
    suspend fun setLanguage(language: AppLanguage)
}
