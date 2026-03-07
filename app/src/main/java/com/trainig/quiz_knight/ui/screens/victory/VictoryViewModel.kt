package com.trainig.quiz_knight.ui.screens.victory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trainig.quiz_knight.data.sound.MusicManager
import com.trainig.quiz_knight.domain.repository.GameStateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VictoryViewModel @Inject constructor(
    private val gameStateRepository: GameStateRepository,
    private val musicManager: MusicManager
) : ViewModel() {

    init {
        // STEP 1: set mode and stop background synchronously in init,
        // before MainActivity.onResume() can restart background music.
        musicManager.prepareVictory()
    }

    /** STEP 2: call from LaunchedEffect — plays triumph_1 then triumph_2, then background. */
    fun onScreenShown() {
        musicManager.playTriumph()
    }

    /** Call when the player taps "Play Again" — resets progress (background already resumes after triumph). */
    fun onPlayAgain() {
        // resumeBackground() is NOT needed here — playTriumph() chains into
        // background automatically when both tracks finish.
        // If the player taps before the triumph is done, force-resume now.
        musicManager.resumeBackground()
        viewModelScope.launch {
            gameStateRepository.resetGameState()
        }
    }
}
