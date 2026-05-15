package com.inglada.battleship.ui.screens

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inglada.battleship.R
import com.inglada.battleship.model.Cell
import com.inglada.battleship.model.CellState
import com.inglada.battleship.model.MoveLog
import com.inglada.battleship.viewmodel.GamePhase
import com.inglada.battleship.viewmodel.GameViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * Composable that represents the main gameplay screen.
 *
 * It manages the setup and playing phases of the Battleship match, observing the
 * [GameViewModel] for state updates and handling user interactions with the boards.
 * Includes exit confirmation dialogs to prevent accidental progress loss and a live
 * log for tablet layouts.
 *
 * @param playerName Alias of the player.
 * @param gridSize Dimensions of the game grid.
 * @param isTimeEnabled Whether turn-based time limits are active.
 * @param timeLimit Duration of the time limit in seconds.
 * @param isHardMode Whether the AI uses tactical targeting.
 * @param onNavigateToResults Callback to navigate to the results screen.
 * @param onAbandonGame Callback triggered when the user confirms they want to exit the match early.
 * @param viewModel The state management unit for the game session.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    playerName: String,
    gridSize: Int,
    isTimeEnabled: Boolean,
    timeLimit: Int,
    isHardMode: Boolean,
    onNavigateToResults: (String, Int, Boolean, Boolean, Int, Boolean, List<MoveLog>) -> Unit,
    onAbandonGame: () -> Unit,
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

    val moveLogs by viewModel.moveLogs.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    var showExitDialog by remember { mutableStateOf(false) }

    BackHandler {
        showExitDialog = true
    }

    LaunchedEffect(Unit) {
        viewModel.initializeBoard(playerName, gridSize, isTimeEnabled, timeLimit, isHardMode)
    }

    LaunchedEffect(isGameOver) {
        if (isGameOver) {
            onNavigateToResults(
                playerName,
                gridSize,
                playerWon,
                isTimeOut,
                totalTimeSpent,
                isHardMode,
                moveLogs
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(text = stringResource(id = R.string.game_abandon_title)) },
            text = { Text(text = stringResource(id = R.string.game_abandon_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        onAbandonGame()
                    }
                ) {
                    Text(
                        text = stringResource(id = R.string.game_abandon_confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text(text = stringResource(id = R.string.game_abandon_cancel))
                }
            }
        )
    }

    val headerContent = @Composable {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp, top = 8.dp),
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
                    color = MaterialTheme.colorScheme.error
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
                                        .background(
                                            if (isSunk) MaterialTheme.colorScheme.error 
                                            else MaterialTheme.colorScheme.outline
                                        )
                                )
                            }
                        }
                        Text(
                            text = stringResource(id = if (isSunk) R.string.game_hud_sunk else R.string.game_hud_alive),
                            color = if (isSunk) MaterialTheme.colorScheme.error else Color.Green, // Green is standard for "Alive"
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
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { showExitDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.cd_exit_game)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            if (isPortrait) {
                // PORTRAIT LAYOUT (SMARTPHONES)
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
                // LANDSCAPE / TABLET LAYOUT
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.4f)
                            .fillMaxHeight()
                    ) {
                        headerContent()
                        if (gamePhase == GamePhase.PLAYING) fleetStatusContent()

                        if (gamePhase == GamePhase.SETUP) {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                SetupControls(
                                    currentShipSize = currentShipSize,
                                    isHorizontal = isHorizontal,
                                    onRotateClick = { viewModel.toggleOrientation() },
                                    onResetClick = { viewModel.resetPlacement(gridSize) }
                                )
                            }
                        } else if (gamePhase == GamePhase.PLAYING) {
                            TurnIndicator(isPlayerTurn = isPlayerTurn)
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = stringResource(id = R.string.game_live_log),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LiveLogList(logs = moveLogs, modifier = Modifier.weight(1f))
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

/**
 * UI for the ship placement phase.
 *
 * @param gridSize The size of the board.
 * @param playerBoard The list of cells representing the player's board.
 * @param currentShipSize The size of the ship currently being placed.
 * @param isHorizontal The current orientation for ship placement.
 * @param onCellClick Callback for when a board cell is clicked.
 * @param onRotateClick Callback for rotating the ship orientation.
 * @param onResetClick Callback for resetting all ship placements.
 */
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

/**
 * Controls for the setup phase, including rotation and reset buttons.
 *
 * @param currentShipSize The size of the ship currently being placed.
 * @param isHorizontal The current orientation for ship placement.
 * @param onRotateClick Callback for rotating the ship orientation.
 * @param onResetClick Callback for resetting all ship placements.
 */
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

/**
 * UI for the active playing phase, showing both boards.
 *
 * @param gridSize The size of the board.
 * @param playerBoard The player's current board state.
 * @param enemyBoard The enemy's current board state.
 * @param isPlayerTurn Whether it is currently the player's turn.
 * @param onCellClick Callback for when a cell on the enemy board is clicked.
 */
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

/**
 * Displays whose turn it currently is.
 *
 * @param isPlayerTurn True if it's the player's turn, false otherwise.
 */
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
        color = if (isPlayerTurn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
    )
}

/**
 * Generic board grid component.
 *
 * @param size The dimensions of the grid.
 * @param board The data representing the cells.
 * @param onCellClick Callback for cell interaction.
 * @param showShips Whether to reveal hidden ships on this board.
 */
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
            Row(modifier = Modifier
                .weight(1f)
                .fillMaxWidth()) {
                for (col in 0 until size) {
                    val cell = board[row][col]
                    CellUI(
                        cell = cell,
                        onCellClick = onCellClick,
                        showShips = showShips,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
}

/**
 * Individual cell component.
 *
 * @param cell The cell data.
 * @param onCellClick Callback for clicking the cell.
 * @param showShips Whether ships are visible in this view.
 * @param modifier Modifier for styling.
 */
@Composable
fun CellUI(
    cell: Cell,
    onCellClick: (com.inglada.battleship.model.Position) -> Unit,
    showShips: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (cell.state) {
        CellState.HIT -> MaterialTheme.colorScheme.error
        CellState.MISS -> MaterialTheme.colorScheme.outline
        CellState.HIDDEN -> if (showShips && cell.hasShip) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
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
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

/**
 * Compact live log list designed for the tablet side panel.
 * Automatically scrolls to the bottom when a new move is logged.
 *
 * @param logs List of moves made in the game.
 * @param modifier Modifier for the container.
 */
@Composable
fun LiveLogList(logs: List<MoveLog>, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(logs) { log ->
                val actorPlayer = stringResource(id = R.string.history_log_actor_player)
                val actorAI = stringResource(id = R.string.history_log_actor_ai)
                val actor = if (log.isPlayer) actorPlayer else actorAI
                
                val hitText = stringResource(id = R.string.move_result_hit)
                val isHit = log.result == hitText
                val resultColor = if (isHit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$actor: (${log.row}, ${log.col})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = log.result.uppercase(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = resultColor
                    )
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}