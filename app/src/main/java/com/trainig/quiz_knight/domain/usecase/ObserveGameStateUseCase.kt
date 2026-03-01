package com.trainig.quiz_knight.domain.usecase

import com.trainig.quiz_knight.domain.model.GameState
import com.trainig.quiz_knight.domain.repository.GameStateRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Exposes the live game state stream to ViewModels. */
class ObserveGameStateUseCase @Inject constructor(
    private val repository: GameStateRepository
) {
    operator fun invoke(): Flow<GameState> = repository.observeGameState()
}

