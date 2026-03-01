package com.trainig.quiz_knight.domain.repository

import com.trainig.quiz_knight.domain.model.GameState
import kotlinx.coroutines.flow.Flow

/**
 * Contract for persisting and loading game progress.
 * Implementation uses DataStore Preferences.
 */
interface GameStateRepository {
    fun observeGameState(): Flow<GameState>
    suspend fun saveGameState(gameState: GameState)
    suspend fun resetGameState()
}

