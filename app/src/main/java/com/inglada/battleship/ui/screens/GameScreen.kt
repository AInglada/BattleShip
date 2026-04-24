package com.inglada.battleship.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.inglada.battleship.model.Cell
import com.inglada.battleship.model.CellState
import com.inglada.battleship.ui.navigation.AppScreens
import com.inglada.battleship.viewmodel.GamePhase
import com.inglada.battleship.viewmodel.GameViewModel
import com.inglada.battleship.R

@Composable
fun GameScreen(
    navController: NavController,
    playerName: String,
    gridSize: Int,
    isTimeEnabled: Boolean,
    timeLimit: Int,
    isHardMode: Boolean,
    viewModel: GameViewModel = viewModel()
) {
    // 1. Observe all the states from our updated ViewModel
    val playerBoard by viewModel.playerBoardState.collectAsState()
    val enemyBoard by viewModel.enemyBoardState.collectAsState()

    val dynamicTimeLeft by viewModel.timeLeft.collectAsState()
    val totalTimeSpent by viewModel.totalTimeSpent.collectAsState()
    val playerWon by viewModel.playerWon.collectAsState()
    val isGameOver by viewModel.isGameOver.collectAsState()
    val gamePhase by viewModel.gamePhase.collectAsState()

    val isPlayerTurn by viewModel.isPlayerTurn.collectAsState()
    val enemyFleetStatus by viewModel.enemyFleetStatus.collectAsState()

    val currentShipSize by viewModel.currentShipSizeToPlace.collectAsState()
    val isHorizontal by viewModel.isHorizontal.collectAsState()

    // 2. Initialize passing the new hardMode variable
    LaunchedEffect(Unit) {
        viewModel.initializeBoard(gridSize, isTimeEnabled, timeLimit, isHardMode)
    }

    // 3. Navigation on Game Over
    LaunchedEffect(isGameOver) {
        if (isGameOver) {
            navController.navigate(AppScreens.Results.createRoute(
                playerName = playerName,
                gridSize = gridSize,
                didWin = playerWon,
                timeSpent = totalTimeSpent,
                isHardMode = isHardMode
            )) {
                popUpTo(AppScreens.Game.route) { inclusive = true }
            }
        }
    }

    // --- REUSABLE UI COMPONENTS ---

    val headerContent = @Composable {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp, top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(id = R.string.game_cmdr, playerName),
                style = MaterialTheme.typography.titleLarge
            )
            if (isTimeEnabled) {
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
    }

    // A reusable board drawer that we can use for both the Player and the Enemy
    val drawBoard = @Composable { board: List<List<Cell>>, isEnemy: Boolean, title: String ->
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                board.forEach { row ->
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        row.forEach { cell ->
                            // Determine the visual color depending on whose board it is
                            val cellColor = when (cell.state) {
                                CellState.HIDDEN -> {
                                    // The player can see their own ships, but not the enemy's
                                    if (!isEnemy && cell.hasShip) Color.DarkGray else Color.LightGray
                                }
                                CellState.MISS -> Color.Cyan
                                CellState.HIT -> Color.Red
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(2.dp)
                                    .border(1.dp, Color.DarkGray)
                                    .background(cellColor)
                                    .clickable {
                                        // Restrict clicks: Setup only on Player board, Playing only on Enemy board
                                        if (gamePhase == GamePhase.SETUP && !isEnemy) {
                                            viewModel.onCellClicked(cell.position)
                                        } else if (gamePhase == GamePhase.PLAYING && isEnemy && isPlayerTurn) {
                                            viewModel.onCellClicked(cell.position)
                                        }
                                    }
                            )
                        }
                    }
                }
            }
        }
    }

    val setupControls = @Composable {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(id = R.string.game_phase_setup, currentShipSize),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = { viewModel.toggleOrientation() }) {
                    val orientationStr = stringResource(id = if (isHorizontal) R.string.game_orientation_h else R.string.game_orientation_v)
                    Text(text = stringResource(id = R.string.game_btn_rotate, orientationStr))
                }
                Button(onClick = { viewModel.resetPlacement(gridSize) }) {
                    Text(text = stringResource(id = R.string.game_btn_reset))
                }
            }
        }
    }

    val hudAndTurnContent = @Composable {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            // Turn Indicator
            val turnText = if (isPlayerTurn) R.string.game_turn_player else R.string.game_turn_enemy
            val turnColor = if (isPlayerTurn) Color.Green else Color.Red

            Text(
                text = stringResource(id = turnText),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = turnColor,
                modifier = Modifier.padding(bottom = 16.dp, top = 8.dp)
            )

            // Fleet HUD
            Text(
                text = stringResource(id = R.string.game_hud_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                enemyFleetStatus.forEach { (shipSize, isSunk) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(modifier = Modifier.padding(bottom = 4.dp)) {
                            repeat(shipSize) {
                                Box(
                                    modifier = Modifier.size(12.dp).padding(1.dp).background(if (isSunk) Color.Red else Color.DarkGray)
                                )
                            }
                        }
                        Text(
                            text = stringResource(id = if (isSunk) R.string.game_hud_sunk else R.string.game_hud_alive),
                            color = if (isSunk) Color.Red else Color.Green,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }

    // --- MAIN LAYOUT ---

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // We use a scrollable surface so small screens don't cut the double board layout
    Box(modifier = Modifier.fillMaxSize()) {
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Side: The active board
                Box(modifier = Modifier.weight(1f)) {
                    if (gamePhase == GamePhase.SETUP) {
                        drawBoard(playerBoard, false, stringResource(id = R.string.game_board_yours))
                    } else {
                        drawBoard(enemyBoard, true, stringResource(id = R.string.game_board_enemy))
                    }
                }

                // Right Side: Info and inactive board
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(start = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    headerContent()
                    if (gamePhase == GamePhase.SETUP) {
                        setupControls()
                    } else {
                        hudAndTurnContent()
                        drawBoard(playerBoard, false, stringResource(id = R.string.game_board_yours))
                    }
                }
            }
        } else {
            // Portrait Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                headerContent()

                if (gamePhase == GamePhase.SETUP) {
                    drawBoard(playerBoard, false, stringResource(id = R.string.game_board_yours))
                    setupControls()
                } else {
                    hudAndTurnContent()
                    drawBoard(enemyBoard, true, stringResource(id = R.string.game_board_enemy))
                    drawBoard(playerBoard, false, stringResource(id = R.string.game_board_yours))
                }
            }
        }
    }
}