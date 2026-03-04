package com.trainig.quiz_knight.ui.navigation

/**
 * All navigation destinations in the app.
 * Arguments are passed as route path segments.
 */
sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object MainMenu : Screen("main_menu")
    data object Map : Screen("map")
    data object Statistics : Screen("statistics")
    data object Quiz : Screen("quiz/{settlementId}") {
        fun createRoute(settlementId: String) = "quiz/$settlementId"
    }
    data object Result : Screen("result/{settlementId}/{score}/{passed}") {
        fun createRoute(settlementId: String, score: Int, passed: Boolean) =
            "result/$settlementId/$score/$passed"
    }
    data object Victory : Screen("victory")
}
