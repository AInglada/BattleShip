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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inglada.battleship.model.CellState
import com.inglada.battleship.viewmodel.GameViewModel

@Composable
fun GameScreen(
    playerName: String,
    gridSize: Int,
    isTimeEnabled: Boolean,
    timeLimit: Int,
    viewModel: GameViewModel = viewModel() // Instantiates the ViewModel automatically
) {
    // 1. Observe the board state. Any change here will update the UI
    val board by viewModel.boardState.collectAsState()

    val dynamicTimeLeft by viewModel.timeLeft.collectAsState()

    // 2. Initialize the board only once when the screen is first loaded
    LaunchedEffect(Unit) {
        viewModel.initializeBoard(gridSize, isTimeEnabled, timeLimit)
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
            Text(text = "Cmdr: $playerName", style = MaterialTheme.typography.titleLarge)

            // Check requirement: Red if time controlled, Blue if not
            if (isTimeEnabled) {
                // It shows the dynamic time counting down
                Text(text = "$dynamicTimeLeft s", style = MaterialTheme.typography.titleLarge, color = Color.Red)
            } else {
                Text(text = "∞", style = MaterialTheme.typography.titleLarge, color = Color.Blue)
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
                                    text = "S",
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
    }
}