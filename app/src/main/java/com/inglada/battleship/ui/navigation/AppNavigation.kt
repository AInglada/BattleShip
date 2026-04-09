package com.inglada.battleship.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.inglada.battleship.ui.screens.ConfigScreen
import com.inglada.battleship.ui.screens.MainMenuScreen
import com.inglada.battleship.ui.screens.GameScreen

// Defines the unique routes for each screen in the app
sealed class AppScreens(val route: String) {
    object MainMenu : AppScreens("main_menu")
    object Help : AppScreens("help_screen")
    object Configuration : AppScreens("configuration_screen")

    // The Game route now expects 4 variables in its URL path
    object Game : AppScreens("game_screen/{playerName}/{gridSize}/{isTimeEnabled}/{timeLimit}") {
        // Helper function to build the final navigation string
        fun createRoute(playerName: String, gridSize: Int, isTimeEnabled: Boolean, timeLimit: Int): String {
            return "game_screen/$playerName/$gridSize/$isTimeEnabled/$timeLimit"
        }
    }

    object Results : AppScreens("results_screen")
}

@Composable
fun AppNavigation() {
    // NavController manages app navigation within a NavHost
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppScreens.MainMenu.route
    ) {
        // 1. Main Menu Route
        composable(route = AppScreens.MainMenu.route) {
            MainMenuScreen(navController = navController)
        }

        // 2. Configuration Route
        composable(route = AppScreens.Configuration.route) {
            ConfigScreen(
                onStartGameClicked = { playerName, gridSize, isTimeEnabled, timeLimit ->
                    navController.navigate(AppScreens.Game.createRoute(playerName, gridSize, isTimeEnabled, timeLimit))
                }
            )
        }

        // 3. Game Route
        composable(
            route = AppScreens.Game.route,
            arguments = listOf(
                navArgument("playerName") { type = NavType.StringType },
                navArgument("gridSize") { type = NavType.IntType },
                navArgument("isTimeEnabled") { type = NavType.BoolType },
                navArgument("timeLimit") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            // Extract the arguments
            val playerName = backStackEntry.arguments?.getString("playerName") ?: "Unknown"
            val gridSize = backStackEntry.arguments?.getInt("gridSize") ?: 8
            val isTimeEnabled = backStackEntry.arguments?.getBoolean("isTimeEnabled") ?: false
            val timeLimit = backStackEntry.arguments?.getInt("timeLimit") ?: 0

            GameScreen(
                playerName = playerName,
                gridSize = gridSize,
                isTimeEnabled = isTimeEnabled,
                timeLimit = timeLimit
            )
        }

        // TODO: Add Help, and Results routes in future commits
    }
}