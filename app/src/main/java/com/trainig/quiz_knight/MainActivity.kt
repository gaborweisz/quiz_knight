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
import com.trainig.quiz_knight.domain.repository.SettingsRepository
import com.trainig.quiz_knight.ui.navigation.QuizKnightNavHost
import com.trainig.quiz_knight.ui.theme.Quiz_knightTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var musicManager: MusicManager
    @Inject lateinit var settingsRepository: SettingsRepository

    private var isResumed = false
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

                    // Read the persisted introShown flag so Splash can route correctly
                    val introShown by settingsRepository.observeIntroShown()
                        .collectAsState(initial = false)

                    QuizKnightNavHost(
                        navController = navController,
                        introShown = introShown,
                        onMarkIntroShown = {
                            lifecycleScope.launch {
                                settingsRepository.setIntroShown(true)
                            }
                            musicManager.resumeBackground()
                        },
                        onIntroVisible = { musicManager.playIntroNow() },
                        onResetIntroRequested = {
                            lifecycleScope.launch {
                                settingsRepository.setIntroShown(false)
                            }
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
        isResumed = true
        musicManager.onResume()
    }

    override fun onPause() {
        super.onPause()
        isResumed = false
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
