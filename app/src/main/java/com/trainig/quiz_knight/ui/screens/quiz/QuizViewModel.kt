package com.trainig.quiz_knight.ui.screens.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trainig.quiz_knight.domain.model.Question
import com.trainig.quiz_knight.domain.usecase.GetQuestionsForTopicUseCase
import com.trainig.quiz_knight.domain.usecase.MapGraphProvider
import com.trainig.quiz_knight.domain.usecase.ObserveGameStateUseCase
import com.trainig.quiz_knight.domain.usecase.SubmitQuizResultUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuizUiState(
    val settlementName: String = "",
    val topicName: String = "",
    val questions: List<Question> = emptyList(),
    val currentIndex: Int = 0,
    val selectedOptionIndex: Int? = null,   // null = no answer yet
    val isAnswerRevealed: Boolean = false,
    val score: Int = 0,
    val isLoading: Boolean = true,
    val isFinished: Boolean = false,
    val passed: Boolean = false
)

@HiltViewModel
class QuizViewModel @Inject constructor(
    private val getQuestions: GetQuestionsForTopicUseCase,
    private val submitResult: SubmitQuizResultUseCase,
    private val mapGraphProvider: MapGraphProvider,
    private val observeGameState: ObserveGameStateUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private var settlementId: String = ""

    fun init(settlementId: String) {
        if (this.settlementId == settlementId) return  // already initialised
        this.settlementId = settlementId

        val settlement = mapGraphProvider.getSettlement(settlementId) ?: return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    settlementName = settlement.name,
                    topicName = settlement.topic.displayName,
                    isLoading = true
                )
            }
            val questions = getQuestions(settlement.topic)
            _uiState.update {
                it.copy(questions = questions, isLoading = false)
            }
        }
    }

    /** Player selects an answer option. */
    fun selectOption(index: Int) {
        if (_uiState.value.isAnswerRevealed) return
        _uiState.update { it.copy(selectedOptionIndex = index, isAnswerRevealed = true) }

        val current = _uiState.value
        val correct = current.questions[current.currentIndex].correctIndex == index
        if (correct) _uiState.update { it.copy(score = it.score + 1) }
    }

    /** Move to the next question or finish the quiz. */
    fun nextQuestion() {
        val current = _uiState.value
        val nextIndex = current.currentIndex + 1

        if (nextIndex >= current.questions.size) {
            // Quiz finished – submit result
            val finalScore = current.score
            val passed = finalScore.toFloat() / GetQuestionsForTopicUseCase.QUESTIONS_PER_QUIZ >=
                    SubmitQuizResultUseCase.PASSING_THRESHOLD

            viewModelScope.launch {
                val gameState = observeGameState().first()
                submitResult(gameState, settlementId, finalScore)
                _uiState.update { it.copy(isFinished = true, passed = passed) }
            }
        } else {
            _uiState.update {
                it.copy(
                    currentIndex = nextIndex,
                    selectedOptionIndex = null,
                    isAnswerRevealed = false
                )
            }
        }
    }
}

