package com.trainig.quiz_knight.domain.usecase

import com.trainig.quiz_knight.domain.model.GameState
import com.trainig.quiz_knight.domain.model.KnightState
import com.trainig.quiz_knight.domain.repository.GameStateRepository
import javax.inject.Inject

/**
 * Moves the knight to a target settlement after validating the move is legal
 * (target must be in the connected list of the current settlement).
 */
class MoveKnightUseCase @Inject constructor(
    private val repository: GameStateRepository,
    private val mapProvider: MapGraphProvider
) {
    suspend operator fun invoke(currentState: GameState, targetSettlementId: String): Result<GameState> {
        val currentId = currentState.knightState.currentSettlementId
        val settlement = mapProvider.getSettlement(currentId)
            ?: return Result.failure(IllegalStateException("Current settlement not found"))

        if (targetSettlementId !in settlement.connectedTo) {
            return Result.failure(IllegalArgumentException("Target settlement is not reachable from current position"))
        }

        val newState = currentState.copy(
            knightState = KnightState(currentSettlementId = targetSettlementId)
        )
        repository.saveGameState(newState)
        return Result.success(newState)
    }
}
