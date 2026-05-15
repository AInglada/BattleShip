package com.inglada.battleship.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inglada.battleship.model.Board
import com.inglada.battleship.model.Cell
import com.inglada.battleship.model.CellState
import com.inglada.battleship.viewmodel.GamePhase
import com.inglada.battleship.viewmodel.GameViewModel
import com.inglada.battleship.R
import kotlinx.coroutines.flow.collectLatest

/**
 * Composable that represents the main gameplay screen.
 *
 * It manages the setup and playing phases of the Battleship match, observing the
 * [GameViewModel] for state updates and handling user interactions with the boards.
 *
 * @param playerName Alias of the player.
 * @param gridSize Dimensions of the game grid.
 * @param isTimeEnabled Whether turn-based time limits are active.
 * @param timeLimit Duration of the time limit in seconds.
 * @param isHardMode Whether the AI uses tactical targeting.
 * @param onNavigateToResults Callback to navigate to the results screen.
 * @param viewModel The state management unit for the game session.
 */
@Composable
fun GameScreen(
    playerName: String,
    gridSize: Int,
    isTimeEnabled: Boolean,
    timeLimit: Int,
    isHardMode: Boolean,
    onNavigateToResults: (String, Int, Boolean, Boolean, Int, Boolean) -> Unit,
    viewModel: GameViewModel = viewModel()
) {
    val playerBoardObj by viewModel.playerBoard.collectAsState()
    val enemyBoardObj by viewModel.enemyBoard.collectAsState()

    val dynamicTimeLeft by viewModel.timeLeft.collectAsState()
    val totalTimeSpent by viewModel.totalTimeSpent.collectAsState()
    val playerWon by viewModel.playerWon.collectAsState()
    val isTimeOut by viewModel.isTimeOut.collectAsState()
    val isGameOver by viewModel.isGameOver.collectAsState()
    val gamePhase by viewModel.gamePhase.collectAsState()

    val isPlayerTurn by viewModel.isPlayerTurn.collectAsState()
    val enemyFleetStatus by viewModel.enemyFleetStatus.collectAsState()

    val currentShipSize by viewModel.currentShipSizeToPlace.collectAsState()
    val isHorizontal by viewModel.isHorizontal.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.initializeBoard(gridSize, isTimeEnabled, timeLimit, isHardMode)
    }

    LaunchedEffect(isGameOver) {
        if (isGameOver) {
            onNavigateToResults(
                playerName,
                gridSize,
                playerWon,
                isTimeOut,
                totalTimeSpent,
                isHardMode
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

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
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }

    val fleetStatusContent = @Composable {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.game_hud_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                enemyFleetStatus.forEach { (shipSize, isSunk) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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

    val screenConfig = LocalConfiguration.current
    val isPortrait = screenConfig.orientation == Configuration.ORIENTATION_PORTRAIT

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (isPortrait) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    headerContent()
                    if (gamePhase == GamePhase.PLAYING) fleetStatusContent()

                    when (gamePhase) {
                        GamePhase.SETUP -> {
                            SetupPhaseUI(
                                gridSize = gridSize,
                                playerBoard = playerBoardObj.grid,
                                currentShipSize = currentShipSize,
                                isHorizontal = isHorizontal,
                                onCellClick = { viewModel.onCellClicked(it) },
                                onRotateClick = { viewModel.toggleOrientation() },
                                onResetClick = { viewModel.resetPlacement(gridSize) }
                            )
                        }
                        GamePhase.PLAYING -> {
                            PlayingPhaseUI(
                                gridSize = gridSize,
                                playerBoard = playerBoardObj.grid,
                                enemyBoard = enemyBoardObj.grid,
                                isPlayerTurn = isPlayerTurn,
                                onCellClick = { viewModel.onCellClicked(it) }
                            )
                        }
                        else -> {}
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.4f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        headerContent()
                        if (gamePhase == GamePhase.PLAYING) fleetStatusContent()

                        if (gamePhase == GamePhase.SETUP) {
                            SetupControls(
                                currentShipSize = currentShipSize,
                                isHorizontal = isHorizontal,
                                onRotateClick = { viewModel.toggleOrientation() },
                                onResetClick = { viewModel.resetPlacement(gridSize) }
                            )
                        } else if (gamePhase == GamePhase.PLAYING) {
                            TurnIndicator(isPlayerTurn = isPlayerTurn)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(0.6f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        when (gamePhase) {
                            GamePhase.SETUP -> {
                                GridBoard(
                                    size = gridSize,
                                    board = playerBoardObj.grid,
                                    onCellClick = { viewModel.onCellClicked(it) },
                                    showShips = true
                                )
                            }
                            GamePhase.PLAYING -> {
                                Column(
                                    modifier = Modifier.verticalScroll(rememberScrollState()),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(text = stringResource(id = R.string.game_board_enemy))
                                    GridBoard(
                                        size = gridSize,
                                        board = enemyBoardObj.grid,
                                        onCellClick = { viewModel.onCellClicked(it) },
                                        showShips = false
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(text = stringResource(id = R.string.game_board_yours))
                                    GridBoard(
                                        size = gridSize,
                                        board = playerBoardObj.grid,
                                        onCellClick = {},
                                        showShips = true
                                    )
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SetupPhaseUI(
    gridSize: Int,
    playerBoard: List<List<Cell>>,
    currentShipSize: Int,
    isHorizontal: Boolean,
    onCellClick: (com.inglada.battleship.model.Position) -> Unit,
    onRotateClick: () -> Unit,
    onResetClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SetupControls(currentShipSize, isHorizontal, onRotateClick, onResetClick)
        Spacer(modifier = Modifier.height(16.dp))
        GridBoard(size = gridSize, board = playerBoard, onCellClick = onCellClick, showShips = true)
    }
}

@Composable
fun SetupControls(
    currentShipSize: Int,
    isHorizontal: Boolean,
    onRotateClick: () -> Unit,
    onResetClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(id = R.string.game_phase_setup, currentShipSize),
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        val orientation = if (isHorizontal) {
            stringResource(id = R.string.game_orientation_h)
        } else {
            stringResource(id = R.string.game_orientation_v)
        }
        Button(onClick = onRotateClick) {
            Text(text = stringResource(id = R.string.game_btn_rotate, orientation))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onResetClick) {
            Text(text = stringResource(id = R.string.game_btn_reset))
        }
    }
}

@Composable
fun PlayingPhaseUI(
    gridSize: Int,
    playerBoard: List<List<Cell>>,
    enemyBoard: List<List<Cell>>,
    isPlayerTurn: Boolean,
    onCellClick: (com.inglada.battleship.model.Position) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TurnIndicator(isPlayerTurn)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = stringResource(id = R.string.game_board_enemy))
        GridBoard(size = gridSize, board = enemyBoard, onCellClick = onCellClick, showShips = false)
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = stringResource(id = R.string.game_board_yours))
        GridBoard(size = gridSize, board = playerBoard, onCellClick = {}, showShips = true)
    }
}

@Composable
fun TurnIndicator(isPlayerTurn: Boolean) {
    val turnText = if (isPlayerTurn) {
        stringResource(id = R.string.game_turn_player)
    } else {
        stringResource(id = R.string.game_turn_enemy)
    }
    Text(
        text = turnText,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = if (isPlayerTurn) MaterialTheme.colorScheme.primary else Color.Gray
    )
}

@Composable
fun GridBoard(
    size: Int,
    board: List<List<Cell>>,
    onCellClick: (com.inglada.battleship.model.Position) -> Unit,
    showShips: Boolean
) {
    if (board.isEmpty()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .border(2.dp, MaterialTheme.colorScheme.outline)
            .padding(4.dp)
    ) {
        for (row in 0 until size) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                for (col in 0 until size) {
                    val cell = board[row][col]
                    CellUI(
                        cell = cell,
                        onCellClick = onCellClick,
                        showShips = showShips,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
fun CellUI(
    cell: Cell,
    onCellClick: (com.inglada.battleship.model.Position) -> Unit,
    showShips: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (cell.state) {
        CellState.HIT -> Color.Red
        CellState.MISS -> Color.Gray
        CellState.HIDDEN -> if (showShips && cell.hasShip) Color.Blue else Color.LightGray
    }

    Box(
        modifier = modifier
            .padding(1.dp)
            .background(backgroundColor)
            .clickable { onCellClick(cell.position) },
        contentAlignment = Alignment.Center
    ) {
        if (showShips && cell.hasShip && cell.state == CellState.HIDDEN) {
            Text(
                text = stringResource(id = R.string.game_hud_cheatShipPlacement),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
        }
    }
}
