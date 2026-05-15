package com.inglada.battleship.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inglada.battleship.R
import com.inglada.battleship.data.UserPreferencesRepository

/**
 * Composable that represents the main menu screen of the application.
 *
 * Provides a Scaffold layout with a TopAppBar for configuration access, and central
 * action buttons to start a new game, view history, read instructions, or exit.
 *
 * @param repository The repository to read the current user preferences before starting a game.
 * @param onNavigateToSettings Callback to open the configuration screen.
 * @param onStartGame Callback to initiate the game session with the loaded parameters.
 * @param onViewHistory Callback to navigate to the match history screen.
 * @param onHelp Callback to navigate to the help screen.
 * @param onExit Callback to close the application.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenuScreen(
    repository: UserPreferencesRepository,
    onNavigateToSettings: () -> Unit,
    onStartGame: (playerName: String, gridSize: Int, isTimeEnabled: Boolean, timeLimit: Int, isHardMode: Boolean) -> Unit,
    onViewHistory: () -> Unit,
    onHelp: () -> Unit,
    onExit: () -> Unit
) {
    // Read the current state of preferences to inject into the game when "Start" is clicked
    val playerName by repository.playerName.collectAsState(initial = stringResource(id = R.string.config_default_player))
    val gridSize by repository.gridSize.collectAsState(initial = 8)
    val isTimeEnabled by repository.isTimeEnabled.collectAsState(initial = false)
    val timeLimit by repository.timeLimit.collectAsState(initial = 60)
    val isHardMode by repository.isHardMode.collectAsState(initial = false)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.menu_title_small)) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(id = R.string.cd_settings)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .systemBarsPadding(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.menu_title),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 48.dp)
            )

            Button(
                onClick = {
                    onStartGame(playerName, gridSize, isTimeEnabled, timeLimit, isHardMode)
                },
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .padding(bottom = 16.dp)
            ) {
                Text(text = stringResource(id = R.string.menu_btn_start))
            }

            Button(
                onClick = onViewHistory,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .padding(bottom = 16.dp)
            ) {
                Text(text = stringResource(id = R.string.menu_btn_history))
            }

            Button(
                onClick = onHelp,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .padding(bottom = 16.dp)
            ) {
                Text(text = stringResource(id = R.string.menu_btn_help))
            }

            Button(
                onClick = onExit,
                modifier = Modifier.fillMaxWidth(0.6f)
            ) {
                Text(text = stringResource(id = R.string.menu_btn_exit))
            }
        }
    }
}