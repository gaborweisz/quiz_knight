package com.trainig.quiz_knight

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.trainig.quiz_knight.data.sound.MusicManager
import com.trainig.quiz_knight.domain.repository.GameStateRepository
import com.trainig.quiz_knight.domain.repository.SettingsRepository
import com.trainig.quiz_knight.ui.navigation.QuizKnightNavHost
import com.trainig.quiz_knight.ui.theme.Quiz_knightTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var musicManager: MusicManager
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var gameStateRepository: GameStateRepository

    private var musicEnabled = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        lifecycleScope.launch {
            settingsRepository.observeMusicEnabled().collectLatest { enabled ->
                musicEnabled = enabled
                musicManager.setEnabled(enabled)
            }
        }

        setContent {
            Quiz_knightTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF1A0F00)
                ) {
                    val navController = rememberNavController()

                    // Intro is skipped when the player has already conquered at least one settlement.
                    // Use null as sentinel so we don't navigate until the real value is loaded.
                    val introShownOrNull by gameStateRepository.observeGameState()
                        .map { it.completedSettlementIds.isNotEmpty() }
                        .collectAsState(initial = null)

                    // Wait for the first real emission before rendering anything,
                    // so the Splash screen never sees a stale false and routes to Intro incorrectly.
                    val introShown = introShownOrNull ?: return@Surface

                    QuizKnightNavHost(
                        navController = navController,
                        introShown = introShown,
                        onMarkIntroShown = {
                            // No-op: intro visibility is now derived from game progress,
                            // not a separate persisted flag.
                            musicManager.resumeBackground()
                        },
                        onIntroVisible = { musicManager.playIntroNow() },
                        onResetIntroRequested = {
                            // No-op: resetting game state (done by MapViewModel.resetProgress)
                            // already clears completedSettlementIds → intro will show on next launch.
                        },
                        onQuit = {
                            musicManager.releaseAll()
                            finishAndRemoveTask()
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        musicManager.onResume()
    }

    override fun onPause() {
        super.onPause()
        musicManager.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        musicManager.releaseAll()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            val insetsController = WindowInsetsControllerCompat(window, window.decorView)
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
