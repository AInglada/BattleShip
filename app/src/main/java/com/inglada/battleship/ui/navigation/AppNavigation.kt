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
import com.inglada.battleship.ui.screens.HelpScreen
import com.inglada.battleship.ui.screens.ResultsScreen

// Defines the unique routes for each screen in the app
sealed class AppScreens(val route: String) {
    object MainMenu : AppScreens("main_menu")
    object Help : AppScreens("help_screen")
    object Configuration : AppScreens("configuration_screen")

    // The Game route now expects 4 variables in its URL path
    object Game : AppScreens("game_screen/{playerName}/{gridSize}/{isTimeEnabled}/{timeLimit}/{isHardMode}") {
        // Helper function to build the final navigation string
        fun createRoute(playerName: String, gridSize: Int, isTimeEnabled: Boolean, timeLimit: Int, isHardMode: Boolean): String {
            return "game_screen/$playerName/$gridSize/$isTimeEnabled/$timeLimit/$isHardMode"
        }
    }

    // We pass the essential game data to build the Log in the Results screen
    object Results : AppScreens("results_screen/{playerName}/{gridSize}/{didWin}/{timeSpent}/{isHardMode}") {
        fun createRoute(playerName: String, gridSize: Int, didWin: Boolean, timeSpent: Int, isHardMode: Boolean): String {
            return "results_screen/$playerName/$gridSize/$didWin/$timeSpent/$isHardMode"
        }
    }
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
                onStartGameClicked = { playerName, gridSize, isTimeEnabled, timeLimit, isHardMode ->
                    navController.navigate(AppScreens.Game.createRoute(playerName, gridSize, isTimeEnabled, timeLimit, isHardMode))
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
                navArgument("timeLimit") { type = NavType.IntType },
                navArgument("isHardMode") { type = NavType.BoolType }
            )
        ) { backStackEntry ->
            // Extract the arguments
            val playerName = backStackEntry.arguments?.getString("playerName") ?: "Unknown"
            val gridSize = backStackEntry.arguments?.getInt("gridSize") ?: 8
            val isTimeEnabled = backStackEntry.arguments?.getBoolean("isTimeEnabled") ?: false
            val timeLimit = backStackEntry.arguments?.getInt("timeLimit") ?: 0
            val isHardMode = backStackEntry.arguments?.getBoolean("isHardMode") ?: false

            GameScreen(
                navController = navController,
                playerName = playerName,
                gridSize = gridSize,
                isTimeEnabled = isTimeEnabled,
                timeLimit = timeLimit,
                isHardMode = isHardMode
            )
        }

        // 4. Results Route
        composable(
            route = AppScreens.Results.route,
            arguments = listOf(
                navArgument("playerName") { type = NavType.StringType },
                navArgument("gridSize") { type = NavType.IntType },
                navArgument("didWin") { type = NavType.BoolType },
                navArgument("timeSpent") { type = NavType.IntType },
                navArgument("isHardMode") { type = NavType.BoolType }
            )
        ) { backStackEntry ->
            val playerName = backStackEntry.arguments?.getString("playerName") ?: "Unknown"
            val gridSize = backStackEntry.arguments?.getInt("gridSize") ?: 8
            val didWin = backStackEntry.arguments?.getBoolean("didWin") ?: false
            val timeSpent = backStackEntry.arguments?.getInt("timeSpent") ?: 0
            val isHardMode = backStackEntry.arguments?.getBoolean("isHardMode") ?: false

            ResultsScreen(
                navController = navController,
                playerName = playerName,
                gridSize = gridSize,
                didWin = didWin,
                timeSpent = timeSpent,
                isHardMode = isHardMode
            )
        }

        // 5. Help Route
        composable(route = AppScreens.Help.route) {
            HelpScreen(navController = navController)
        }
    }
}