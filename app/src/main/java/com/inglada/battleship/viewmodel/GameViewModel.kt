package com.inglada.battleship.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.inglada.battleship.data.GameMatchEntity
import com.inglada.battleship.data.MatchRepository
import com.inglada.battleship.model.Board
import com.inglada.battleship.model.CellState
import com.inglada.battleship.model.MoveLog
import com.inglada.battleship.model.Position
import com.inglada.battleship.model.Ship
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import kotlin.random.Random

/**
 * Defines the distinct phases of the game lifecycle.
 */
enum class GamePhase {
    /** The player is placing their ships on the board. */
    SETUP,
    /** The game is in progress, with turns being taken. */
    PLAYING,
    /** The match has concluded. */
    GAME_OVER
}

/**
 * Represents the result of a single move.
 */
object MoveResults {
    /** Target was a ship. */
    const val HIT = "Hit"
    /** Target was empty water. */
    const val MISS = "Miss"
}

/**
 * Represents the final outcome of a match.
 */
object GameOutcomes {
    /** Player destroyed the entire enemy fleet. */
    const val VICTORY = "Victory"
    /** Player's fleet was destroyed by the AI. */
    const val DEFEAT_AI = "Defeat (AI)"
    /** Player ran out of time. */
    const val DEFEAT_TIMEOUT = "Defeat (Timeout)"
}

/**
 * ViewModel responsible for managing the game state, logic, and turn-based interactions.
 * It handles the state for both the player and enemy boards, timer logic, AI behavior, and move logging.
 *
 * @property repository The repository to persist match results.
 */
class GameViewModel(private val repository: MatchRepository) : ViewModel() {

    // --- State Properties ---

    private val _playerBoard = MutableStateFlow(Board())
    /** The current state of the player's board. */
    val playerBoard: StateFlow<Board> = _playerBoard.asStateFlow()

    private val _enemyBoard = MutableStateFlow(Board())
    /** The current state of the enemy's board (as seen by the player). */
    val enemyBoard: StateFlow<Board> = _enemyBoard.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 1)
    /** Flow of short-lived UI messages/events (e.g., error notifications). */
    val events = _events.asSharedFlow()

    private val _timeLeft = MutableStateFlow(0)
    /** Remaining time for the current turn in seconds. */
    val timeLeft: StateFlow<Int> = _timeLeft.asStateFlow()

    private val _totalTimeSpent = MutableStateFlow(0)
    /** Total accumulated time for the entire match. */
    val totalTimeSpent: StateFlow<Int> = _totalTimeSpent.asStateFlow()

    private val _gamePhase = MutableStateFlow(GamePhase.SETUP)
    /** Current phase of the game session. */
    val gamePhase: StateFlow<GamePhase> = _gamePhase.asStateFlow()

    private val _isGameOver = MutableStateFlow(false)
    /** Flag indicating if the game has ended. */
    val isGameOver: StateFlow<Boolean> = _isGameOver.asStateFlow()

    private val _isTimeOut = MutableStateFlow(false)
    /** Flag indicating if the game ended due to time running out. */
    val isTimeOut: StateFlow<Boolean> = _isTimeOut.asStateFlow()

    private val _playerWon = MutableStateFlow(false)
    /** Flag indicating if the player won the match. */
    val playerWon: StateFlow<Boolean> = _playerWon.asStateFlow()

    private val _isPlayerTurn = MutableStateFlow(true)
    /** Flag indicating if it is currently the player's turn to act. */
    val isPlayerTurn: StateFlow<Boolean> = _isPlayerTurn.asStateFlow()

    private val _enemyFleetStatus = MutableStateFlow<List<Pair<Int, Boolean>>>(emptyList())
    /** List of ship sizes and their sunk status for the enemy fleet. */
    val enemyFleetStatus: StateFlow<List<Pair<Int, Boolean>>> = _enemyFleetStatus.asStateFlow()

    private val _currentShipSizeToPlace = MutableStateFlow(5)
    /** Size of the ship currently being handled during the SETUP phase. */
    val currentShipSizeToPlace: StateFlow<Int> = _currentShipSizeToPlace.asStateFlow()

    private val _isHorizontal = MutableStateFlow(true)
    /** Orientation (horizontal/vertical) for ship placement. */
    val isHorizontal: StateFlow<Boolean> = _isHorizontal.asStateFlow()

    private val _moveLogs = MutableStateFlow<List<MoveLog>>(emptyList())
    /** The sequential log of all moves made during the match. */
    val moveLogs: StateFlow<List<MoveLog>> = _moveLogs.asStateFlow()

    // --- Internal Properties ---
    private val shipsToPlaceSizes = listOf(5, 4, 3, 3, 2)
    private val _currentShipIndex = MutableStateFlow(0)
    private val playerShips = mutableListOf<Ship>()
    private val enemyShips = mutableListOf<Ship>()
    private var baseTimeLimit = 0
    private var isInitialized = false
    private var timerJob: Job? = null
    private var timeWasEnabled = false
    private var isHardMode = false
    private var playerName = "Unknown"
    private var gridSize = 8
    private val aiTargetQueue = mutableListOf<Position>()

    // --- Public Methods ---

    /**
     * Initializes the game session with the specified rules.
     *
     * @param playerName Name of the player.
     * @param size Grid dimensions.
     * @param isTimeEnabled Whether turn limits are active.
     * @param timeLimit Seconds allowed per turn.
     * @param hardMode Whether the AI uses tactical targeting.
     */
    fun initializeBoard(playerName: String, size: Int, isTimeEnabled: Boolean, timeLimit: Int, hardMode: Boolean) {
        if (isInitialized) return
        this.playerName = playerName
        this.gridSize = size
        timeWasEnabled = isTimeEnabled
        isHardMode = hardMode
        baseTimeLimit = timeLimit
        if (isTimeEnabled) _timeLeft.value = timeLimit
        val emptyBoard = Board.createEmpty(size)
        _playerBoard.value = emptyBoard
        _enemyBoard.value = emptyBoard
        _gamePhase.value = GamePhase.SETUP
        _currentShipSizeToPlace.value = shipsToPlaceSizes[0]
        isInitialized = true
    }

    /**
     * Toggles ship orientation between horizontal and vertical.
     */
    fun toggleOrientation() {
        _isHorizontal.value = !_isHorizontal.value
    }

    /**
     * Clears all placed ships and restarts the SETUP phase.
     *
     * @param size Grid dimensions to reset.
     */
    fun resetPlacement(size: Int) {
        playerShips.clear()
        _currentShipIndex.value = 0
        _currentShipSizeToPlace.value = shipsToPlaceSizes[0]
        _isHorizontal.value = true
        _playerBoard.value = Board.createEmpty(size)
    }

    /**
     * Handles user interaction with a specific cell on the board.
     *
     * @param position The coordinates of the clicked cell.
     */
    fun onCellClicked(position: Position) {
        when (_gamePhase.value) {
            GamePhase.SETUP -> tryPlaceShipManually(position)
            GamePhase.PLAYING -> {
                if (_isPlayerTurn.value) performPlayerAttack(position)
            }
            GamePhase.GAME_OVER -> return
        }
    }

    // --- Private Helper Methods ---

    /**
     * Records a move into the session log.
     * 
     * @param isPlayer True if the player made the move, false if AI.
     * @param row Row coordinate.
     * @param col Column coordinate.
     * @param result Result string (Hit/Miss).
     */
    private fun logMove(isPlayer: Boolean, row: Int, col: Int, result: String) {
        val currentTimeLeft = _timeLeft.value.takeIf { timeWasEnabled }
        val newLog = MoveLog(
            isPlayer = isPlayer,
            row = row,
            col = col,
            result = result,
            timeRemaining = currentTimeLeft
        )
        _moveLogs.value = _moveLogs.value + newLog
    }

    /**
     * Starts the game timer and turn clock logic.
     */
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_gamePhase.value == GamePhase.PLAYING) {
                delay(1000L)
                _totalTimeSpent.value += 1
                if (timeWasEnabled && _isPlayerTurn.value) {
                    _timeLeft.value -= 1
                    if (_timeLeft.value <= 0) {
                        _isTimeOut.value = true
                        endGame(won = false, isTimeout = true)
                    }
                }
            }
        }
    }

    /**
     * Logic for placing a ship during the SETUP phase.
     */
    private fun tryPlaceShipManually(startPos: Position) {
        if (_currentShipIndex.value >= shipsToPlaceSizes.size) return
        val shipSize = shipsToPlaceSizes[_currentShipIndex.value]
        val horizontal = _isHorizontal.value
        val board = _playerBoard.value

        if (board.canPlaceShip(startPos, shipSize, horizontal)) {
            val updatedBoard = board.placeShip(startPos, shipSize, horizontal)
            val shipPositions = (0 until shipSize).map { i ->
                val row = if (horizontal) startPos.row else startPos.row + i
                val col = if (horizontal) startPos.col + i else startPos.col
                Position(row, col)
            }
            playerShips.add(Ship(shipSize, shipPositions))
            _playerBoard.value = updatedBoard

            val nextIndex = _currentShipIndex.value + 1
            if (nextIndex < shipsToPlaceSizes.size) {
                _currentShipIndex.value = nextIndex
                _currentShipSizeToPlace.value = shipsToPlaceSizes[nextIndex]
            } else {
                startGameplay()
            }
        } else {
            viewModelScope.launch {
                _events.emit("Invalid placement: Ships cannot overlap or touch diagonally")
            }
        }
    }

    /**
     * Transitions the game from SETUP to PLAYING phase.
     */
    private fun startGameplay() {
        _gamePhase.value = GamePhase.PLAYING
        placeEnemyShipsRandomly(_enemyBoard.value.size)
        updateEnemyFleetStatus(_enemyBoard.value)
        startTimer()
    }

    /**
     * Randomly places AI ships on the enemy board.
     */
    private fun placeEnemyShipsRandomly(boardSize: Int) {
        var tempBoard = Board.createEmpty(boardSize)
        for (shipSize in shipsToPlaceSizes) {
            var isPlaced = false
            var attempts = 0
            while (!isPlaced && attempts < 500) {
                attempts++
                val isHorizontal = Random.nextBoolean()
                val startRow = Random.nextInt(boardSize)
                val startCol = Random.nextInt(boardSize)
                val startPos = Position(startRow, startCol)

                if (tempBoard.canPlaceShip(startPos, shipSize, isHorizontal)) {
                    tempBoard = tempBoard.placeShip(startPos, shipSize, isHorizontal)
                    val shipPositions = (0 until shipSize).map { i ->
                        val row = if (isHorizontal) startRow else startRow + i
                        val col = if (isHorizontal) startCol + i else startCol
                        Position(row, col)
                    }
                    enemyShips.add(Ship(shipSize, shipPositions))
                    isPlaced = true
                }
            }
        }
        _enemyBoard.value = tempBoard
    }

    /**
     * Executes an attack by the player on the enemy board.
     */
    private fun performPlayerAttack(position: Position) {
        val currentBoard = _enemyBoard.value
        val clickedCell = currentBoard.getCell(position)

        if (clickedCell.state != CellState.HIDDEN) return

        val newState = if (clickedCell.hasShip) CellState.HIT else CellState.MISS
        val resultString = if (newState == CellState.HIT) MoveResults.HIT else MoveResults.MISS

        logMove(isPlayer = true, row = position.row, col = position.col, result = resultString)

        val updatedBoard = currentBoard.updateCell(position, newState)
        _enemyBoard.value = updatedBoard
        updateEnemyFleetStatus(updatedBoard)

        val playerWon = enemyShips.all { ship -> ship.isSunk(updatedBoard.grid) }
        if (playerWon) {
            endGame(won = true)
        } else {
            if (timeWasEnabled) _timeLeft.value = baseTimeLimit
            _isPlayerTurn.value = false
            viewModelScope.launch {
                delay(1000L)
                performAIAttack()
            }
        }
    }

    /**
     * Executes an attack by the AI on the player's board.
     */
    private fun performAIAttack() {
        if (_gamePhase.value != GamePhase.PLAYING || _isGameOver.value) return

        val board = _playerBoard.value
        val size = board.size
        var target: Position? = null

        if (isHardMode && aiTargetQueue.isNotEmpty()) {
            while (aiTargetQueue.isNotEmpty() && target == null) {
                val candidate = aiTargetQueue.removeAt(0)
                if (board.getCell(candidate).state == CellState.HIDDEN) {
                    target = candidate
                }
            }
        }

        if (target == null) {
            val hiddenCells = mutableListOf<Position>()
            board.grid.forEach { row ->
                row.forEach { cell ->
                    if (cell.state == CellState.HIDDEN) hiddenCells.add(cell.position)
                }
            }
            if (hiddenCells.isNotEmpty()) {
                target = hiddenCells.random()
            } else {
                return
            }
        }

        val clickedCell = board.getCell(target)
        val newState = if (clickedCell.hasShip) CellState.HIT else CellState.MISS
        val resultString = if (newState == CellState.HIT) MoveResults.HIT else MoveResults.MISS

        logMove(isPlayer = false, row = target.row, col = target.col, result = resultString)

        val updatedBoard = board.updateCell(target, newState)
        _playerBoard.value = updatedBoard

        if (newState == CellState.HIT && isHardMode) {
            val adj = listOf(
                Position(target.row - 1, target.col),
                Position(target.row + 1, target.col),
                Position(target.row, target.col - 1),
                Position(target.row, target.col + 1),
            ).filter { it.row in 0 until size && it.col in 0 until size }

            aiTargetQueue.addAll(adj)
        }

        val aiWon = playerShips.all { it.isSunk(updatedBoard.grid) }
        if (aiWon) {
            endGame(won = false)
        } else {
            _isPlayerTurn.value = true
        }
    }

    /**
     * Updates the status of the enemy ships based on the current board state.
     */
    private fun updateEnemyFleetStatus(board: Board) {
        _enemyFleetStatus.value = enemyShips.map { ship ->
            Pair(ship.size, ship.isSunk(board.grid))
        }.sortedByDescending { it.first }
    }

    /**
     * Ends the game, stops the timer, and persists the result to the database.
     *
     * @param won True if the player won, false if they lost.
     * @param isTimeout True if the loss was due to time running out.
     */
    private fun endGame(won: Boolean, isTimeout: Boolean = false) {
        _playerWon.value = won
        _gamePhase.value = GamePhase.GAME_OVER
        _isGameOver.value = true
        timerJob?.cancel()

        val finalOutcome = when {
            won -> GameOutcomes.VICTORY
            isTimeout -> GameOutcomes.DEFEAT_TIMEOUT
            else -> GameOutcomes.DEFEAT_AI
        }

        viewModelScope.launch {
            repository.insertMatch(
                GameMatchEntity(
                    playerName = playerName,
                    timestamp = Date().time,
                    gridSize = gridSize,
                    timeSpent = _totalTimeSpent.value,
                    outcome = finalOutcome,
                    moveLogs = _moveLogs.value
                )
            )
        }
    }
}

/**
 * Factory for creating [GameViewModel] with its dependencies.
 */
class GameViewModelFactory(private val repository: MatchRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GameViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}