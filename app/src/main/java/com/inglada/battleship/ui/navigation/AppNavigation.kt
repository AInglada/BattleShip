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

/**
 * Defines the unique routes and navigation arguments for each screen in the application.
 *
 * @property route The string identifier for the destination.
 */
sealed class AppScreens(val route: String) {
    /** Route for the initial landing screen. */
    object MainMenu : AppScreens("main_menu")
    /** Route for the instructional screen. */
    object Help : AppScreens("help_screen")
    /** Route for the game configuration settings screen. */
    object Configuration : AppScreens("configuration_screen")

    /**
     * Route for the active game session.
     * Includes parameters for player preferences and game rules.
     */
    object Game : AppScreens("game_screen/{playerName}/{gridSize}/{isTimeEnabled}/{timeLimit}/{isHardMode}") {
        /**
         * Helper function to build the navigation route with the required arguments.
         */
        fun createRoute(playerName: String, gridSize: Int, isTimeEnabled: Boolean, timeLimit: Int, isHardMode: Boolean): String {
            return "game_screen/$playerName/$gridSize/$isTimeEnabled/$timeLimit/$isHardMode"
        }
    }

    /**
     * Route for the results summary screen shown after a match.
     * Includes metrics and outcome data for logging.
     */
    object Results : AppScreens("results_screen/{playerName}/{gridSize}/{didWin}/{isTimeOut}/{timeSpent}/{isHardMode}") {
        /**
         * Helper function to build the navigation route with the required arguments.
         */
        fun createRoute(playerName: String, gridSize: Int, didWin: Boolean, isTimeOut: Boolean, timeSpent: Int, isHardMode: Boolean): String {
            return "results_screen/$playerName/$gridSize/$didWin/$isTimeOut/$timeSpent/$isHardMode"
        }
    }
}

/**
 * The root navigation component that manages the screen transitions and back stack.
 *
 * It defines the [NavHost] and all its composable destinations, extracting
 * navigation arguments where necessary.
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppScreens.MainMenu.route
    ) {
        composable(route = AppScreens.MainMenu.route) {
            MainMenuScreen(navController = navController)
        }

        composable(route = AppScreens.Configuration.route) {
            ConfigScreen(
                onBackClicked = { navController.popBackStack() },
                onStartGameClicked = { playerName, gridSize, isTimeEnabled, timeLimit, isHardMode ->
                    navController.navigate(AppScreens.Game.createRoute(playerName, gridSize, isTimeEnabled, timeLimit, isHardMode))
                }
            )
        }

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

        composable(
            route = AppScreens.Results.route,
            arguments = listOf(
                navArgument("playerName") { type = NavType.StringType },
                navArgument("gridSize") { type = NavType.IntType },
                navArgument("didWin") { type = NavType.BoolType },
                navArgument("isTimeOut") { type = NavType.BoolType },
                navArgument("timeSpent") { type = NavType.IntType },
                navArgument("isHardMode") { type = NavType.BoolType }
            )
        ) { backStackEntry ->
            val playerName = backStackEntry.arguments?.getString("playerName") ?: "Unknown"
            val gridSize = backStackEntry.arguments?.getInt("gridSize") ?: 8
            val didWin = backStackEntry.arguments?.getBoolean("didWin") ?: false
            val isTimeOut = backStackEntry.arguments?.getBoolean("isTimeOut") ?: false
            val timeSpent = backStackEntry.arguments?.getInt("timeSpent") ?: 0
            val isHardMode = backStackEntry.arguments?.getBoolean("isHardMode") ?: false

            ResultsScreen(
                navController = navController,
                playerName = playerName,
                gridSize = gridSize,
                didWin = didWin,
                isTimeOut = isTimeOut,
                timeSpent = timeSpent,
                isHardMode = isHardMode
            )
        }

        composable(route = AppScreens.Help.route) {
            HelpScreen(navController = navController)
        }
    }
}