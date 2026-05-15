package com.inglada.battleship.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlin.random.Random

/**
 * Defines the distinct phases of the game lifecycle.
 */
enum class GamePhase {
    SETUP,
    PLAYING,
    GAME_OVER
}

/**
 * ViewModel responsible for managing the game state, logic, and turn-based interactions.
 * It handles the state for both the player and enemy boards, timer logic, AI behavior, and move logging.
 */
class GameViewModel : ViewModel() {

    // --- State Properties ---

    private val _playerBoard = MutableStateFlow(Board())
    val playerBoard: StateFlow<Board> = _playerBoard.asStateFlow()

    private val _enemyBoard = MutableStateFlow(Board())
    val enemyBoard: StateFlow<Board> = _enemyBoard.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    private val _timeLeft = MutableStateFlow(0)
    val timeLeft: StateFlow<Int> = _timeLeft.asStateFlow()

    private val _totalTimeSpent = MutableStateFlow(0)
    val totalTimeSpent: StateFlow<Int> = _totalTimeSpent.asStateFlow()

    private val _gamePhase = MutableStateFlow(GamePhase.SETUP)
    val gamePhase: StateFlow<GamePhase> = _gamePhase.asStateFlow()

    private val _isGameOver = MutableStateFlow(false)
    val isGameOver: StateFlow<Boolean> = _isGameOver.asStateFlow()

    private val _isTimeOut = MutableStateFlow(false)
    val isTimeOut: StateFlow<Boolean> = _isTimeOut.asStateFlow()

    private val _playerWon = MutableStateFlow(false)
    val playerWon: StateFlow<Boolean> = _playerWon.asStateFlow()

    private val _isPlayerTurn = MutableStateFlow(true)
    val isPlayerTurn: StateFlow<Boolean> = _isPlayerTurn.asStateFlow()

    private val _enemyFleetStatus = MutableStateFlow<List<Pair<Int, Boolean>>>(emptyList())
    val enemyFleetStatus: StateFlow<List<Pair<Int, Boolean>>> = _enemyFleetStatus.asStateFlow()

    private val _currentShipSizeToPlace = MutableStateFlow(5)
    val currentShipSizeToPlace: StateFlow<Int> = _currentShipSizeToPlace.asStateFlow()

    private val _isHorizontal = MutableStateFlow(true)
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
    private val aiTargetQueue = mutableListOf<Position>()

    // --- Public Methods ---

    fun initializeBoard(size: Int, isTimeEnabled: Boolean, timeLimit: Int, hardMode: Boolean) {
        if (isInitialized) return
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

    fun toggleOrientation() {
        _isHorizontal.value = !_isHorizontal.value
    }

    fun resetPlacement(size: Int) {
        playerShips.clear()
        _currentShipIndex.value = 0
        _currentShipSizeToPlace.value = shipsToPlaceSizes[0]
        _isHorizontal.value = true
        _playerBoard.value = Board.createEmpty(size)
    }

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
                        endGame(won = false)
                    }
                }
            }
        }
    }

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

    private fun startGameplay() {
        _gamePhase.value = GamePhase.PLAYING
        placeEnemyShipsRandomly(_enemyBoard.value.size)
        updateEnemyFleetStatus(_enemyBoard.value)
        startTimer()
    }

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

    private fun performPlayerAttack(position: Position) {
        val currentBoard = _enemyBoard.value
        val clickedCell = currentBoard.getCell(position)

        if (clickedCell.state != CellState.HIDDEN) return

        val newState = if (clickedCell.hasShip) CellState.HIT else CellState.MISS

        logMove(isPlayer = true, row = position.row, col = position.col, result = if (newState == CellState.HIT) "Hit" else "Miss")

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

        logMove(isPlayer = false, row = target.row, col = target.col, result = if (newState == CellState.HIT) "Hit" else "Miss")

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

    private fun updateEnemyFleetStatus(board: Board) {
        _enemyFleetStatus.value = enemyShips.map { ship ->
            Pair(ship.size, ship.isSunk(board.grid))
        }.sortedByDescending { it.first }
    }

    private fun endGame(won: Boolean) {
        _playerWon.value = won
        _gamePhase.value = GamePhase.GAME_OVER
        _isGameOver.value = true
        timerJob?.cancel()
    }
}