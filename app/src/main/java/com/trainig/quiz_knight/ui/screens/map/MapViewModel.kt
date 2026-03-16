package com.trainig.quiz_knight.ui.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trainig.quiz_knight.domain.model.GameState
import com.trainig.quiz_knight.domain.model.Settlement
import com.trainig.quiz_knight.domain.repository.GameStateRepository
import com.trainig.quiz_knight.domain.repository.SettingsRepository
import com.trainig.quiz_knight.domain.usecase.MapGraphProvider
import com.trainig.quiz_knight.domain.usecase.MoveKnightUseCase
import com.trainig.quiz_knight.domain.usecase.ObserveGameStateUseCase
import com.trainig.quiz_knight.data.sound.SoundManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapUiState(
    val settlements: List<Settlement> = emptyList(),
    // Settled resting position (used when not moving)
    val knightSettlementId: String = "",
    // When non-null the knight is animating from → to
    val knightFromId: String? = null,
    val knightToId: String? = null,
    val completedIds: Set<String> = emptySet(),
    val isMoving: Boolean = false,
    val isGameComplete: Boolean = false,
    val errorMessage: String? = null,
    val shouldOpenQuiz: Boolean = false,
    val musicEnabled: Boolean = true,
    /** Non-null when the replay-confirmation dialog should be shown for this settlement id */
    val replaySettlementId: String? = null
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val observeGameState: ObserveGameStateUseCase,
    private val moveKnight: MoveKnightUseCase,
    private val mapGraphProvider: MapGraphProvider,
    private val gameStateRepository: GameStateRepository,
    private val soundManager: SoundManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private var latestGameState: GameState? = null

    init {
        val allSettlements = mapGraphProvider.getAllSettlements()
        _uiState.update { it.copy(settlements = allSettlements) }

        viewModelScope.launch {
            observeGameState().collect { gameState ->
                latestGameState = gameState
                val enriched = allSettlements.map { s ->
                    s.copy(
                        isCompleted = s.id in gameState.completedSettlementIds,
                        lastScore = gameState.settlementScores[s.id] ?: 0
                    )
                }
                _uiState.update {
                    it.copy(
                        settlements = enriched,
                        knightSettlementId = gameState.knightState.currentSettlementId,
                        completedIds = gameState.completedSettlementIds,
                        isGameComplete = gameState.isGameComplete
                    )
                }
            }
        }

        // Observe music setting
        viewModelScope.launch {
            settingsRepository.observeMusicEnabled().collect { enabled ->
                _uiState.update { it.copy(musicEnabled = enabled) }
            }
        }
    }

    /** Toggles music on/off and persists the preference. */
    fun toggleMusic() {
        viewModelScope.launch {
            settingsRepository.setMusicEnabled(!_uiState.value.musicEnabled)
        }
    }

    /** Called when the player taps a settlement node on the map. */
    fun onSettlementTapped(targetSettlementId: String) {
        val state = latestGameState ?: return
        if (_uiState.value.isMoving) return
        if (targetSettlementId == state.knightState.currentSettlementId) return

        val fromId = state.knightState.currentSettlementId

        viewModelScope.launch {
            // Play footstep sound when movement starts
            launch { soundManager.playFootstep() }

            _uiState.update {
                it.copy(
                    isMoving = true,
                    errorMessage = null,
                    shouldOpenQuiz = false,
                    knightFromId = fromId,
                    knightToId = targetSettlementId
                )
            }
            // Persist the move (no delay — animation runs in the UI layer)
            moveKnight(state, targetSettlementId)
                .onSuccess { newState ->
                    latestGameState = newState
                    // For a completed settlement, a normal tap does NOT start the quiz
                    val needsQuiz = targetSettlementId !in newState.completedSettlementIds
                    _uiState.update {
                        it.copy(
                            knightSettlementId = targetSettlementId,
                            shouldOpenQuiz = needsQuiz
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isMoving = false,
                            knightFromId = null,
                            knightToId = null,
                            errorMessage = err.message
                        )
                    }
                }
        }
    }

    /**
     * Called when the player long-presses a settlement.
     * Knight moves there, then if it is already completed the replay dialog is shown.
     */
    fun onSettlementLongPressed(targetSettlementId: String) {
        val state = latestGameState ?: return
        if (_uiState.value.isMoving) return

        val isCompleted = targetSettlementId in (state.completedSettlementIds)
        val isCurrent   = targetSettlementId == state.knightState.currentSettlementId

        // If knight is already here just show the dialog (or start quiz if not completed)
        if (isCurrent) {
            if (isCompleted) {
                _uiState.update { it.copy(replaySettlementId = targetSettlementId) }
            } else {
                // Not yet completed — treat like a normal tap and start the quiz
                _uiState.update { it.copy(shouldOpenQuiz = true) }
            }
            return
        }

        val fromId = state.knightState.currentSettlementId
        viewModelScope.launch {
            launch { soundManager.playFootstep() }
            _uiState.update {
                it.copy(
                    isMoving = true,
                    errorMessage = null,
                    shouldOpenQuiz = false,
                    knightFromId = fromId,
                    knightToId = targetSettlementId
                )
            }
            moveKnight(state, targetSettlementId)
                .onSuccess { newState ->
                    latestGameState = newState
                    val settled = targetSettlementId in newState.completedSettlementIds
                    _uiState.update {
                        it.copy(
                            knightSettlementId = targetSettlementId,
                            // If completed → show replay dialog after animation;
                            // if not completed → start quiz normally
                            shouldOpenQuiz = !settled,
                            replaySettlementId = if (settled) targetSettlementId else null
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isMoving = false,
                            knightFromId = null,
                            knightToId = null,
                            errorMessage = err.message
                        )
                    }
                }
        }
    }

    /** Player confirmed they want to replay the settlement quiz. */
    fun onReplayConfirmed() {
        val settlementId = _uiState.value.replaySettlementId ?: return
        viewModelScope.launch {
            // Strip the settlement from completed so the map shows it as un-conquered
            // immediately, and SubmitQuizResultUseCase starts from a clean slate.
            gameStateRepository.resetSettlement(settlementId)
            _uiState.update { it.copy(replaySettlementId = null, shouldOpenQuiz = true) }
        }
    }

    /** Player dismissed the replay dialog. */
    fun onReplayDismissed() {
        _uiState.update { it.copy(replaySettlementId = null) }
    }

    /** Called by the UI once the walk animation has finished. */
    fun onAnimationFinished() {
        // Play arrival chime when knight reaches destination
        viewModelScope.launch { soundManager.playArrival() }
        _uiState.update {
            it.copy(isMoving = false, knightFromId = null, knightToId = null)
        }
    }

    /** Called by the UI after it has consumed the quiz-open signal. */
    fun onQuizOpened() = _uiState.update { it.copy(shouldOpenQuiz = false) }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    fun resetProgress() {
        viewModelScope.launch { gameStateRepository.resetGameState() }
    }
}
