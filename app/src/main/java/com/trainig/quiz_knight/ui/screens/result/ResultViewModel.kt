package com.trainig.quiz_knight.ui.screens.result

import androidx.lifecycle.ViewModel
import com.trainig.quiz_knight.data.sound.MusicManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ResultViewModel @Inject constructor(
    private val musicManager: MusicManager
) : ViewModel() {

    private var prepared = false

    /**
     * STEP 1 — call on first composition (SideEffect / first recompose guard).
     * Sets the mode and stops background synchronously, before onResume() fires.
     */
    fun prepare(passed: Boolean) {
        if (prepared) return
        prepared = true
        if (passed) musicManager.prepareVictory() else musicManager.prepareDefeat()
    }

    /**
     * STEP 2 — call from LaunchedEffect.
     * Actually starts the audio clip after the screen is composed.
     */
    fun onResultShown(passed: Boolean) {
        if (passed) musicManager.playVictory() else musicManager.playDefeat()
    }

    /** Call when the player taps "Return to Map". */
    fun onReturnToMap() {
        musicManager.resumeBackground()
    }
}
