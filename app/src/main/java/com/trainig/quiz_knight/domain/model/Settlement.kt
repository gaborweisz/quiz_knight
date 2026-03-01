package com.trainig.quiz_knight.domain.model

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

enum class QuizTopic(val displayName: String) {
    LITERATURE("Literature"),
    GENERAL_HISTORY("General History"),
    SCIENCE_HISTORY("Science History"),
    ART("Art"),
    GEOGRAPHY("Geography"),
    BIOLOGY("Biology"),
    CHEMISTRY("Chemistry"),
    SPACE("Space"),
    COMPUTER_SCIENCE("Computer Science")
}

/**
 * Normalized (0f..1f) position on the map canvas.
 * Multiplied by the actual canvas size at render time.
 */
data class MapPosition(val x: Float, val y: Float)

