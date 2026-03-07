package com.trainig.quiz_knight.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.trainig.quiz_knight.ui.screens.map.MapScreen
import com.trainig.quiz_knight.ui.screens.quiz.QuizScreen
import com.trainig.quiz_knight.ui.screens.result.ResultScreen
import com.trainig.quiz_knight.ui.screens.splash.SplashScreen
import com.trainig.quiz_knight.ui.screens.intro.IntroScreen
import com.trainig.quiz_knight.ui.screens.stats.StatsScreen
import com.trainig.quiz_knight.ui.screens.victory.VictoryScreen

@Composable
fun QuizKnightNavHost(
    navController: NavHostController,
    introShown: Boolean,
    onMarkIntroShown: () -> Unit,
    onIntroVisible: () -> Unit,
    onResetIntroRequested: () -> Unit
) {
    // onResetIntroRequested should be provided by the Activity to clear the persisted flag
    // so that replaying the intro works even after the first-run flag was set.
    NavHost(
        navController = navController,
        // Choose start destination based on persisted intro flag
        startDestination = if (introShown) Screen.Map.route else Screen.Intro.route
    ) {
        // ── Intro ──────────────────────────────────────────────────────────
        composable(Screen.Intro.route) {
            IntroScreen(
                onContinue = {
                    onMarkIntroShown()
                    navController.navigate(Screen.Map.route) {
                        popUpTo(Screen.Intro.route) { inclusive = true }
                    }
                },
                onShown = { onIntroVisible() },
                onSkip = {
                    onMarkIntroShown()
                    navController.navigate(Screen.Map.route) {
                        popUpTo(Screen.Intro.route) { inclusive = true }
                    }
                }
            )
        }

        // ── Splash ──────────────────────────────────────────────────────────
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToMenu = {
                    navController.navigate(Screen.Map.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // ── Map ─────────────────────────────────────────────────────────────
        composable(Screen.Map.route) {
            MapScreen(
                onEnterSettlement = { settlementId ->
                    navController.navigate(Screen.Quiz.createRoute(settlementId))
                },
                onVictory = {
                    navController.navigate(Screen.Victory.route) {
                        popUpTo(Screen.Map.route) { inclusive = false }
                    }
                },
                onStatistics = {
                    navController.navigate(Screen.Statistics.route)
                },
                onReplayIntro = {
                    // First clear the persisted "intro shown" flag so Intro appears as first-run
                    onResetIntroRequested()
                    // Then navigate to the Intro route so the user sees it
                    navController.navigate(Screen.Intro.route)
                },

            )
        }

        // ── Statistics ───────────────────────────────────────────────────────
        composable(Screen.Statistics.route) {
            StatsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // ── Quiz ────────────────────────────────────────────────────────────
        composable(
            route = Screen.Quiz.route,
            arguments = listOf(navArgument("settlementId") { type = NavType.StringType })
        ) { backStackEntry ->
            val settlementId = backStackEntry.arguments?.getString("settlementId") ?: ""
            QuizScreen(
                settlementId = settlementId,
                onQuizFinished = { score, passed ->
                    navController.navigate(Screen.Result.createRoute(settlementId, score, passed)) {
                        popUpTo(Screen.Quiz.createRoute(settlementId)) { inclusive = true }
                    }
                },
                onBackToMap = {
                    navController.navigate(Screen.Map.route) {
                        popUpTo(Screen.Map.route) { inclusive = false }
                    }
                }
            )
        }

        // ── Result ──────────────────────────────────────────────────────────
        composable(
            route = Screen.Result.route,
            arguments = listOf(
                navArgument("settlementId") { type = NavType.StringType },
                navArgument("score") { type = NavType.IntType },
                navArgument("passed") { type = NavType.BoolType }
            )
        ) { backStackEntry ->
            val settlementId = backStackEntry.arguments?.getString("settlementId") ?: ""
            val score = backStackEntry.arguments?.getInt("score") ?: 0
            val passed = backStackEntry.arguments?.getBoolean("passed") ?: false
            ResultScreen(
                settlementId = settlementId,
                score = score,
                passed = passed,
                onContinue = {
                    navController.navigate(Screen.Map.route) {
                        popUpTo(Screen.Map.route) { inclusive = false }
                    }
                }
            )
        }

        // ── Victory ─────────────────────────────────────────────────────────
        composable(Screen.Victory.route) {
            VictoryScreen(
                onPlayAgain = {
                    navController.navigate(Screen.Map.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Note: Replay helper removed — the host (MainActivity) will handle clearing the persisted flag
        // when the Map HUD invokes the replay action (see MainActivity wiring for onResetIntroRequested).
    }
}
