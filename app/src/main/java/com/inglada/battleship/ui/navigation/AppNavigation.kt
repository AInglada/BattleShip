package com.inglada.battleship.ui.navigation

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.inglada.battleship.data.BattleshipDatabase
import com.inglada.battleship.data.GameMatchEntity
import com.inglada.battleship.data.MatchRepository
import com.inglada.battleship.data.UserPreferencesRepository
import com.inglada.battleship.data.dataStore
import com.inglada.battleship.ui.screens.ConfigScreen
import com.inglada.battleship.ui.screens.GameScreen
import com.inglada.battleship.ui.screens.HelpScreen
import com.inglada.battleship.ui.screens.HistoryScreen
import com.inglada.battleship.ui.screens.MainMenuScreen
import com.inglada.battleship.ui.screens.ResultsScreen
import com.inglada.battleship.viewmodel.ConfigViewModel
import com.inglada.battleship.viewmodel.ConfigViewModelFactory
import com.inglada.battleship.viewmodel.HistoryViewModel
import com.inglada.battleship.viewmodel.HistoryViewModelFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Date

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
    /** Route for the adaptive match history screen. */
    object History : AppScreens("history_screen")

    /**
     * Route for the active game session.
     * Includes parameters for player preferences and game rules.
     */
    object Game : AppScreens("game_screen/{playerName}/{gridSize}/{isTimeEnabled}/{timeLimit}/{isHardMode}") {
        fun createRoute(playerName: String, gridSize: Int, isTimeEnabled: Boolean, timeLimit: Int, isHardMode: Boolean): String {
            return "game_screen/$playerName/$gridSize/$isTimeEnabled/$timeLimit/$isHardMode"
        }
    }

    /**
     * Route for the results summary screen shown after a match.
     * Includes metrics and outcome data for logging.
     */
    object Results : AppScreens("results_screen/{playerName}/{gridSize}/{didWin}/{isTimeOut}/{timeSpent}/{isHardMode}") {
        fun createRoute(playerName: String, gridSize: Int, didWin: Boolean, isTimeOut: Boolean, timeSpent: Int, isHardMode: Boolean): String {
            return "results_screen/$playerName/$gridSize/$didWin/$isTimeOut/$timeSpent/$isHardMode"
        }
    }
}

/**
 * The root navigation component that manages the screen transitions and back stack.
 *
 * It defines the [NavHost], instantiates repositories, and configures all composable destinations.
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val safePopBackStack: () -> Unit = {
        if (navController.previousBackStackEntry != null) {
            navController.popBackStack()
        }
    }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Initialize the DataStore repository singleton
    val preferencesRepository = remember { UserPreferencesRepository(context.dataStore) }

    // Initialize the Room Database and Match Repository
    val database = remember { BattleshipDatabase.getDatabase(context) }
    val matchRepository = remember { MatchRepository(database.gameMatchDao()) }

    NavHost(
        navController = navController,
        startDestination = AppScreens.MainMenu.route
    ) {
        composable(route = AppScreens.MainMenu.route) {
            MainMenuScreen(
                repository = preferencesRepository,
                onNavigateToSettings = { navController.navigate(AppScreens.Configuration.route) },
                onStartGame = { pName, size, timeEnabled, limit, hard ->
                    navController.navigate(AppScreens.Game.createRoute(pName, size, timeEnabled, limit, hard))
                },
                onViewHistory = { navController.navigate(AppScreens.History.route) },
                onHelp = { navController.navigate(AppScreens.Help.route) },
                onExit = { (context as? Activity)?.finish() }
            )
        }

        composable(route = AppScreens.Configuration.route) {
            val configViewModel: ConfigViewModel = viewModel(
                factory = ConfigViewModelFactory(preferencesRepository)
            )
            val uiState by configViewModel.uiState.collectAsState()

            ConfigScreen(
                uiState = uiState,
                onBackClicked = { safePopBackStack() },
                onSaveConfigClicked = { playerName, gridSize, isTimeEnabled, timeLimit, isHardMode ->
                    configViewModel.saveConfig(playerName, gridSize, isTimeEnabled, timeLimit, isHardMode)
                    safePopBackStack()
                }
            )
        }

        composable(route = AppScreens.History.route) {
            val historyViewModel: HistoryViewModel = viewModel(
                factory = HistoryViewModelFactory(matchRepository)
            )
            HistoryScreen(
                viewModel = historyViewModel,
                onBackClicked = { safePopBackStack() }
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
                playerName = playerName,
                gridSize = gridSize,
                isTimeEnabled = isTimeEnabled,
                timeLimit = timeLimit,
                isHardMode = isHardMode,
                onNavigateToResults = { pName, size, win, timeout, time, hard, logs ->

                    val finalOutcome = when {
                        win -> "Victory"
                        timeout -> "Defeat (Timeout)"
                        else -> "Defeat (AI)"
                    }
                    val matchEntity = GameMatchEntity(
                        playerName = pName,
                        timestamp = Date().time,
                        gridSize = size,
                        timeSpent = time,
                        outcome = finalOutcome,
                        moveLogs = logs
                    )
                    coroutineScope.launch {
                        matchRepository.insertMatch(matchEntity)
                    }

                    navController.navigate(
                        AppScreens.Results.createRoute(pName, size, win, timeout, time, hard)
                    ) {
                        popUpTo(AppScreens.MainMenu.route)
                    }
                },
                onAbandonGame = { safePopBackStack() }
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
                playerName = playerName,
                gridSize = gridSize,
                didWin = didWin,
                isTimeOut = isTimeOut,
                timeSpent = timeSpent,
                isHardMode = isHardMode,
                onPlayAgain = {
                    runBlocking {
                        val pName = preferencesRepository.playerName.first()
                        val size = preferencesRepository.gridSize.first()
                        val timeEnabled = preferencesRepository.isTimeEnabled.first()
                        val limit = preferencesRepository.timeLimit.first()
                        val hard = preferencesRepository.isHardMode.first()
                        navController.navigate(AppScreens.Game.createRoute(pName, size, timeEnabled, limit, hard)) {
                            popUpTo(AppScreens.MainMenu.route)
                        }
                    }
                },
                onBackToMenu = { safePopBackStack() },
                onExit = { (context as? Activity)?.finish() }
            )
        }

        composable(route = AppScreens.Help.route) {
            HelpScreen(onBack = { safePopBackStack() })
        }
    }
}