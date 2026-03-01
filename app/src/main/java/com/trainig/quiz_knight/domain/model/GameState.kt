package com.trainig.quiz_knight.domain.model

/**
 * Snapshot of the knight's current state on the map.
 */
data class KnightState(
    val currentSettlementId: String,
    val isMoving: Boolean = false
)

/**
 * Top-level game state – persisted across sessions.
 */
data class GameState(
    val knightState: KnightState,
    val completedSettlementIds: Set<String> = emptySet(),
    val settlementScores: Map<String, Int> = emptyMap(), // settlementId -> score out of 12
    val isGameComplete: Boolean = false
)

