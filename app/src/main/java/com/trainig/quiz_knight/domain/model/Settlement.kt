package com.trainig.quiz_knight.domain.model

import androidx.annotation.StringRes
import com.trainig.quiz_knight.R

/**
 * Represents a settlement (village or city) on the map.
 * Each settlement is a node in the map graph and hosts one quiz topic.
 */
data class Settlement(
    val id: String,
    val name: String,
    val type: SettlementType,
    val topic: QuizTopic,
    val position: MapPosition,       // Normalized 0..1 coordinates for Canvas rendering
    val connectedTo: List<String>,   // IDs of directly reachable settlements (graph edges)
    val isCompleted: Boolean = false,
    val lastScore: Int = 0           // Score out of 12 from the last quiz attempt
)

enum class SettlementType {
    VILLAGE, CITY
}

enum class QuizTopic(@StringRes val displayNameRes: Int) {
    LITERATURE(R.string.topic_literature),
    GENERAL_HISTORY(R.string.topic_general_history),
    SCIENCE_HISTORY(R.string.topic_science_history),
    ART(R.string.topic_art),
    GEOGRAPHY(R.string.topic_geography),
    BIOLOGY(R.string.topic_biology),
    CHEMISTRY(R.string.topic_chemistry),
    SPACE(R.string.topic_space),
    COMPUTER_SCIENCE(R.string.topic_computer_science),
    FILM(R.string.topic_film),
    ART_HISTORY(R.string.topic_art_history),
    PHYSICS(R.string.topic_physics)
}

/**
 * Normalized (0f..1f) position on the map canvas.
 * Multiplied by the actual canvas size at render time.
 */
data class MapPosition(val x: Float, val y: Float)
