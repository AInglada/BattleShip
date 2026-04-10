package com.inglada.battleship.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.inglada.battleship.model.CellState
import com.inglada.battleship.ui.navigation.AppScreens
import com.inglada.battleship.viewmodel.GameViewModel
import com.inglada.battleship.R

@Composable
fun GameScreen(
    navController: NavController,
    playerName: String,
    gridSize: Int,
    isTimeEnabled: Boolean,
    timeLimit: Int,
    viewModel: GameViewModel = viewModel() // Instantiates the ViewModel automatically
) {
    // 1. Observe the board state. Any change here will update the UI
    val board by viewModel.boardState.collectAsState()

    val dynamicTimeLeft by viewModel.timeLeft.collectAsState()

    val isGameOver by viewModel.isGameOver.collectAsState()

    val fleetStatus by viewModel.fleetStatus.collectAsState()

    // 2. Initialize the board only once when the screen is first loaded
    LaunchedEffect(Unit) {
        viewModel.initializeBoard(gridSize, isTimeEnabled, timeLimit)
    }

    // 3. Listen for game over state to navigate to the Results screen
    LaunchedEffect(isGameOver) {
        if (isGameOver) {
            // For now, we assume if time is not 0, the player won.
            val didWin = dynamicTimeLeft > 0 || !isTimeEnabled
            val timeSpent = if (isTimeEnabled) timeLimit - dynamicTimeLeft else 0

            navController.navigate(AppScreens.Results.createRoute(playerName, gridSize, didWin, timeSpent)) {
                // Ensure we can't go back to the finished game by pressing the physical back button
                popUpTo(AppScreens.Game.route) { inclusive = true }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header (Player info and Time control as per requirements)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp, top = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(id = R.string.game_cmdr, playerName),
                style = MaterialTheme.typography.titleLarge
            )

            // Check requirement: Red if time controlled, Blue if not
            if (isTimeEnabled) {
                // It shows the dynamic time counting down
                Text(
                    text = stringResource(id = R.string.game_time_seconds, dynamicTimeLeft),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.Red
                )
            } else {
                Text(
                    text = stringResource(id = R.string.game_time_infinite),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.Blue
                )
            }
        }

        // The Game Grid
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f) // Ensures the grid is always a perfect square
        ) {
            board.forEach { row ->
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    row.forEach { cell ->
                        Box(
                            modifier = Modifier
                                .weight(1f) // Distributes width equally
                                .fillMaxHeight() // Distributes height equally
                                .padding(2.dp)
                                .border(1.dp, Color.DarkGray)
                                .background(
                                    when (cell.state) {
                                        CellState.HIDDEN -> Color.LightGray
                                        CellState.MISS -> Color.Cyan
                                        CellState.HIT -> Color.Red
                                    }
                                )
                                .clickable {
                                    // Let the ViewModel handle the click!
                                    viewModel.onCellClicked(cell.position)
                                }
                        ) {
                            // --- DEVELOPER CHEAT MODE ---
                            // Uncomment the code below to see where the ships are generated
                            /*
                            if (cell.hasShip) {
                                Text(
                                    text = stringResource(id = R.string.game_hud_cheatShipPlacement),
                                    modifier = Modifier.align(Alignment.Center),
                                    color = Color.Black
                                )
                            }
                            */
                        }
                    }
                }
            }
        }
        // --- Fleet Status HUD ---
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(id = R.string.game_hud_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Draw the ships
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            fleetStatus.forEach { (shipSize, isSunk) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Draw mini squares for each part of the ship
                    Row(modifier = Modifier.padding(bottom = 4.dp)) {
                        repeat(shipSize) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .padding(1.dp)
                                    .background(if (isSunk) Color.Red else Color.DarkGray)
                            )
                        }
                    }
                    // Status text
                    Text(
                        text = if (isSunk) stringResource(id = R.string.game_hud_sunk) else stringResource(id = R.string.game_hud_alive),
                        color = if (isSunk) Color.Red else Color.Green,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}