package com.trainig.quiz_knight.domain.usecase

import com.trainig.quiz_knight.domain.model.GameState
import com.trainig.quiz_knight.domain.repository.GameStateRepository
import javax.inject.Inject

/**
 * Records a quiz result for a settlement.
 * Marks it completed if score >= passing threshold (default 70% = 9/12).
 * Also checks if ALL settlements are now completed → sets isGameComplete = true.
 */
class SubmitQuizResultUseCase @Inject constructor(
    private val repository: GameStateRepository,
    private val mapProvider: MapGraphProvider
) {
    companion object {
        const val PASSING_THRESHOLD = 0.70f
        const val TOTAL_QUESTIONS = 12
    }

    suspend operator fun invoke(
        currentState: GameState,
        settlementId: String,
        score: Int
    ): GameState {
        val passed = score.toFloat() / TOTAL_QUESTIONS >= PASSING_THRESHOLD

        val updatedCompleted = if (passed) {
            currentState.completedSettlementIds + settlementId
        } else {
            // On defeat, explicitly remove from completed (handles replay-defeat scenario)
            currentState.completedSettlementIds - settlementId
        }

        val updatedScores = currentState.settlementScores + (settlementId to score)

        val allSettlementIds = mapProvider.getAllSettlements().map { it.id }.toSet()
        val isGameComplete = allSettlementIds.all { it in updatedCompleted }

        val newState = currentState.copy(
            completedSettlementIds = updatedCompleted,
            settlementScores = updatedScores,
            isGameComplete = isGameComplete
        )
        repository.saveGameState(newState)
        return newState
    }
}
