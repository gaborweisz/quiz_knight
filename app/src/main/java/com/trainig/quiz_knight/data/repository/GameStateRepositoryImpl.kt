package com.trainig.quiz_knight.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.trainig.quiz_knight.data.map.MapGraphProviderImpl
import com.trainig.quiz_knight.domain.model.GameState
import com.trainig.quiz_knight.domain.model.KnightState
import com.trainig.quiz_knight.domain.repository.GameStateRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "game_state")

@Singleton
class GameStateRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson,
    private val mapGraphProvider: MapGraphProviderImpl
) : GameStateRepository {

    private object Keys {
        val KNIGHT_SETTLEMENT = stringPreferencesKey("knight_settlement")
        val COMPLETED_IDS = stringPreferencesKey("completed_ids")       // JSON array of strings
        val SETTLEMENT_SCORES = stringPreferencesKey("settlement_scores") // JSON object
        val IS_GAME_COMPLETE = booleanPreferencesKey("is_game_complete")
    }

    override fun observeGameState(): Flow<GameState> =
        context.dataStore.data.map { prefs ->
            val knightId = prefs[Keys.KNIGHT_SETTLEMENT]
                ?: mapGraphProvider.getStartingSettlementId()

            val completedJson = prefs[Keys.COMPLETED_IDS] ?: "[]"
            val completedIds: Set<String> = gson.fromJson<List<String>>(
                completedJson, List::class.java
            ).toSet()

            val scoresJson = prefs[Keys.SETTLEMENT_SCORES] ?: "{}"
            @Suppress("UNCHECKED_CAST")
            val rawScores: Map<String, Double> = gson.fromJson(scoresJson, Map::class.java)
                    as? Map<String, Double> ?: emptyMap()
            val scores: Map<String, Int> = rawScores.mapValues { it.value.toInt() }

            val isComplete = prefs[Keys.IS_GAME_COMPLETE] ?: false

            GameState(
                knightState = KnightState(currentSettlementId = knightId),
                completedSettlementIds = completedIds,
                settlementScores = scores,
                isGameComplete = isComplete
            )
        }

    override suspend fun saveGameState(gameState: GameState) {
        context.dataStore.edit { prefs ->
            prefs[Keys.KNIGHT_SETTLEMENT] = gameState.knightState.currentSettlementId
            prefs[Keys.COMPLETED_IDS] = gson.toJson(gameState.completedSettlementIds.toList())
            prefs[Keys.SETTLEMENT_SCORES] = gson.toJson(gameState.settlementScores)
            prefs[Keys.IS_GAME_COMPLETE] = gameState.isGameComplete
        }
    }

    override suspend fun resetGameState() {
        context.dataStore.edit { it.clear() }
    }
}

