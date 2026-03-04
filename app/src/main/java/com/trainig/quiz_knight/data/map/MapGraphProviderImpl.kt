package com.trainig.quiz_knight.data.map

import com.trainig.quiz_knight.domain.model.*
import com.trainig.quiz_knight.domain.usecase.MapGraphProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Defines the static medieval map graph.
 * 12 settlements (matching the 12 quiz topics) connected by roads.
 * Positions are normalized (0f..1f) for Canvas rendering.
 *
 * Graph layout (roughly):
 *
 *   [Ironwood]---[Ashford]---[Duskholm]
 *       |            |            |
 *   [Millhaven]--[Stonekeep]--[Brightwater]
 *       |            |            |
 *   [Ferndale]---[Ravenspire]--[Solaris]
 *       |            |            |
 *   [Cinehaven]--[Gallerymoor]-[Newtonia]
 */
@Singleton
class MapGraphProviderImpl @Inject constructor() : MapGraphProvider {

    private val settlements: List<Settlement> = listOf(
        Settlement(
            id = "ironwood",
            name = "Ironwood",
            type = SettlementType.VILLAGE,
            topic = QuizTopic.LITERATURE,
            position = MapPosition(0.12f, 0.12f),
            connectedTo = listOf("ashford", "millhaven")
        ),
        Settlement(
            id = "ashford",
            name = "Ashford",
            type = SettlementType.CITY,
            topic = QuizTopic.GENERAL_HISTORY,
            position = MapPosition(0.45f, 0.10f),
            connectedTo = listOf("ironwood", "duskholm", "stonekeep")
        ),
        Settlement(
            id = "duskholm",
            name = "Duskholm",
            type = SettlementType.VILLAGE,
            topic = QuizTopic.SCIENCE_HISTORY,
            position = MapPosition(0.80f, 0.12f),
            connectedTo = listOf("ashford", "brightwater")
        ),
        Settlement(
            id = "millhaven",
            name = "Millhaven",
            type = SettlementType.VILLAGE,
            topic = QuizTopic.ART,
            position = MapPosition(0.12f, 0.38f),
            connectedTo = listOf("ironwood", "stonekeep", "ferndale")
        ),
        Settlement(
            id = "stonekeep",
            name = "Stonekeep",
            type = SettlementType.CITY,
            topic = QuizTopic.GEOGRAPHY,
            position = MapPosition(0.45f, 0.37f),
            connectedTo = listOf("ashford", "millhaven", "brightwater", "ravenspire")
        ),
        Settlement(
            id = "brightwater",
            name = "Brightwater",
            type = SettlementType.VILLAGE,
            topic = QuizTopic.BIOLOGY,
            position = MapPosition(0.80f, 0.38f),
            connectedTo = listOf("duskholm", "stonekeep", "solaris")
        ),
        Settlement(
            id = "ferndale",
            name = "Ferndale",
            type = SettlementType.VILLAGE,
            topic = QuizTopic.CHEMISTRY,
            position = MapPosition(0.12f, 0.63f),
            connectedTo = listOf("millhaven", "ravenspire", "cinehaven")
        ),
        Settlement(
            id = "ravenspire",
            name = "Ravenspire",
            type = SettlementType.CITY,
            topic = QuizTopic.SPACE,
            position = MapPosition(0.45f, 0.62f),
            connectedTo = listOf("stonekeep", "ferndale", "solaris", "gallerymoor")
        ),
        Settlement(
            id = "solaris",
            name = "Solaris",
            type = SettlementType.CITY,
            topic = QuizTopic.COMPUTER_SCIENCE,
            position = MapPosition(0.80f, 0.63f),
            connectedTo = listOf("brightwater", "ravenspire", "newtonia")
        ),
        Settlement(
            id = "cinehaven",
            name = "Cinehaven",
            type = SettlementType.VILLAGE,
            topic = QuizTopic.FILM,
            position = MapPosition(0.12f, 0.88f),
            connectedTo = listOf("ferndale", "gallerymoor")
        ),
        Settlement(
            id = "gallerymoor",
            name = "Gallerymoor",
            type = SettlementType.CITY,
            topic = QuizTopic.ART_HISTORY,
            position = MapPosition(0.45f, 0.87f),
            connectedTo = listOf("ravenspire", "cinehaven", "newtonia")
        ),
        Settlement(
            id = "newtonia",
            name = "Newtonia",
            type = SettlementType.CITY,
            topic = QuizTopic.PHYSICS,
            position = MapPosition(0.80f, 0.88f),
            connectedTo = listOf("solaris", "gallerymoor")
        )
    )

    private val settlementMap = settlements.associateBy { it.id }

    override fun getAllSettlements(): List<Settlement> = settlements

    override fun getSettlement(id: String): Settlement? = settlementMap[id]

    /** Returns the starting settlement for a fresh game. */
    fun getStartingSettlementId(): String = "stonekeep"
}
