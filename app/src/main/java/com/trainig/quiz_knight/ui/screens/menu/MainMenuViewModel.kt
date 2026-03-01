package com.trainig.quiz_knight.ui.screens.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trainig.quiz_knight.domain.repository.GameStateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainMenuViewModel @Inject constructor(
    private val gameStateRepository: GameStateRepository
) : ViewModel() {

    fun resetProgress() {
        viewModelScope.launch {
            gameStateRepository.resetGameState()
        }
    }
}

