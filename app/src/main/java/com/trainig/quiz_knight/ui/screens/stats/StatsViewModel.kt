package com.trainig.quiz_knight.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trainig.quiz_knight.domain.model.Settlement
import com.trainig.quiz_knight.domain.usecase.GetQuestionsForTopicUseCase
import com.trainig.quiz_knight.domain.usecase.MapGraphProvider
import com.trainig.quiz_knight.domain.usecase.ObserveGameStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettlementStats(
    val settlement: Settlement,
    val correct: Int,
    val total: Int,
    val percentage: Float,
    val attempted: Boolean
)

data class StatsUiState(
    val stats: List<SettlementStats> = emptyList(),
    val totalCorrect: Int = 0,
    val totalAttempted: Int = 0,
    val overallPercentage: Float = 0f
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val observeGameState: ObserveGameStateUseCase,
    private val mapGraphProvider: MapGraphProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    private val questionsPerQuiz = GetQuestionsForTopicUseCase.QUESTIONS_PER_QUIZ

    init {
        viewModelScope.launch {
            observeGameState().collect { gameState ->
                val allSettlements = mapGraphProvider.getAllSettlements()
                val statsList = allSettlements.map { s ->
                    val attempted = s.id in gameState.completedSettlementIds ||
                            gameState.settlementScores.containsKey(s.id)
                    val correct = gameState.settlementScores[s.id] ?: 0
                    val pct = if (attempted) correct.toFloat() / questionsPerQuiz.toFloat() * 100f else 0f
                    SettlementStats(
                        settlement = s.copy(
                            isCompleted = s.id in gameState.completedSettlementIds,
                            lastScore = correct
                        ),
                        correct = correct,
                        total = questionsPerQuiz,
                        percentage = pct,
                        attempted = attempted
                    )
                }

                val attempted = statsList.filter { it.attempted }
                val totalCorrect = attempted.sumOf { it.correct }
                val totalPossible = attempted.size * questionsPerQuiz
                val overallPct = if (totalPossible > 0) totalCorrect.toFloat() / totalPossible * 100f else 0f

                _uiState.update {
                    it.copy(
                        stats = statsList,
                        totalCorrect = totalCorrect,
                        totalAttempted = attempted.size,
                        overallPercentage = overallPct
                    )
                }
            }
        }
    }
}

