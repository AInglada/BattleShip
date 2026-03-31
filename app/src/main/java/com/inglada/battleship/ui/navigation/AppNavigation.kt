package com.inglada.battleship.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.inglada.battleship.ui.screens.ConfigScreen
import com.inglada.battleship.ui.screens.MainMenuScreen

// Defines the unique routes for each screen in the app
sealed class AppScreens(val route: String) {
    object MainMenu : AppScreens("main_menu")
    object Help : AppScreens("help_screen")
    object Configuration : AppScreens("configuration_screen")
    object Game : AppScreens("game_screen")
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
            ConfigScreen() // We don't pass navController yet as we just want to see it
        }

        // TODO: Add Help, Game, and Results routes in future commits
    }
}