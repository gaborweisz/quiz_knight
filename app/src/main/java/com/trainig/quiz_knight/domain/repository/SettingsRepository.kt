package com.trainig.quiz_knight.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Persists user-facing settings (e.g. music on/off, whether intro was shown).
 */
interface SettingsRepository {
    fun observeMusicEnabled(): Flow<Boolean>
    suspend fun setMusicEnabled(enabled: Boolean)

    // Whether the Intro screen has already been shown (so it won't appear on subsequent launches)
    fun observeIntroShown(): Flow<Boolean>
    suspend fun setIntroShown(shown: Boolean)
}
