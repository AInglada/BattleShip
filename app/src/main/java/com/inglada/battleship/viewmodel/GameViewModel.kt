package com.inglada.battleship.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inglada.battleship.model.Cell
import com.inglada.battleship.model.CellState
import com.inglada.battleship.model.Position
import com.inglada.battleship.model.Ship
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Defines the distinct phases of the game lifecycle.
 */
enum class GamePhase {
    /** The player is placing their ships on the board. */
    SETUP,
    /** The player and AI are taking turns attacking each other. */
    PLAYING,
    /** The game has concluded. */
    GAME_OVER
}

/**
 * ViewModel responsible for managing the game state, logic, and turn-based interactions.
 * It handles the state for both the player and enemy boards, timer logic, and AI behavior.
 */
class GameViewModel : ViewModel() {

    // --- State Properties ---

    private val _playerBoardState = MutableStateFlow<List<List<Cell>>>(emptyList())
    /** The current state of the player's board. */
    val playerBoardState: StateFlow<List<List<Cell>>> = _playerBoardState.asStateFlow()

    private val _enemyBoardState = MutableStateFlow<List<List<Cell>>>(emptyList())
    /** The current state of the enemy's board from the player's perspective. */
    val enemyBoardState: StateFlow<List<List<Cell>>> = _enemyBoardState.asStateFlow()

    private val _timeLeft = MutableStateFlow(0)
    /** Remaining time for the current turn in seconds. */
    val timeLeft: StateFlow<Int> = _timeLeft.asStateFlow()

    private val _totalTimeSpent = MutableStateFlow(0)
    /** Total duration of the match in seconds. */
    val totalTimeSpent: StateFlow<Int> = _totalTimeSpent.asStateFlow()

    private val _gamePhase = MutableStateFlow(GamePhase.SETUP)
    /** Current phase of the game. */
    val gamePhase: StateFlow<GamePhase> = _gamePhase.asStateFlow()

    private val _isGameOver = MutableStateFlow(false)
    /** Whether the game has finished. */
    val isGameOver: StateFlow<Boolean> = _isGameOver.asStateFlow()

    private val _isTimeOut = MutableStateFlow(false)
    /** Whether the game ended due to a time limit expiration. */
    val isTimeOut: StateFlow<Boolean> = _isTimeOut.asStateFlow()

    private val _playerWon = MutableStateFlow(false)
    /** Whether the player emerged victorious. */
    val playerWon: StateFlow<Boolean> = _playerWon.asStateFlow()

    private val _isPlayerTurn = MutableStateFlow(true)
    /** Whether it is currently the player's turn to act. */
    val isPlayerTurn: StateFlow<Boolean> = _isPlayerTurn.asStateFlow()

    private val _enemyFleetStatus = MutableStateFlow<List<Pair<Int, Boolean>>>(emptyList())
    /** Status of the enemy fleet, represented as pairs of ship size and sunk status. */
    val enemyFleetStatus: StateFlow<List<Pair<Int, Boolean>>> = _enemyFleetStatus.asStateFlow()

    private val _currentShipSizeToPlace = MutableStateFlow(5)
    /** Size of the ship currently being placed during the setup phase. */
    val currentShipSizeToPlace: StateFlow<Int> = _currentShipSizeToPlace.asStateFlow()

    private val _isHorizontal = MutableStateFlow(true)
    /** Current orientation for ship placement. */
    val isHorizontal: StateFlow<Boolean> = _isHorizontal.asStateFlow()

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

    /**
     * Initializes the game boards and parameters.
     *
     * @param size The dimensions of the square grid.
     * @param isTimeEnabled Whether a time limit should be enforced.
     * @param timeLimit The duration of the time limit in seconds.
     * @param hardMode Whether the AI should use tactical targeting logic.
     */
    fun initializeBoard(size: Int, isTimeEnabled: Boolean, timeLimit: Int, hardMode: Boolean) {
        if (isInitialized) return

        timeWasEnabled = isTimeEnabled
        isHardMode = hardMode
        baseTimeLimit = timeLimit

        if (isTimeEnabled) {
            _timeLeft.value = timeLimit
        }

        val emptyBoard = List(size) { row ->
            List(size) { col -> Cell(position = Position(row, col)) }
        }

        _playerBoardState.value = emptyBoard
        _enemyBoardState.value = emptyBoard
        _gamePhase.value = GamePhase.SETUP
        _currentShipSizeToPlace.value = shipsToPlaceSizes[0]

        isInitialized = true
    }

    /**
     * Toggles the orientation for ship placement between horizontal and vertical.
     */
    fun toggleOrientation() {
        _isHorizontal.value = !_isHorizontal.value
    }

    /**
     * Resets the player's ship placements and restarts the setup phase.
     *
     * @param size The dimensions of the square grid.
     */
    fun resetPlacement(size: Int) {
        playerShips.clear()
        _currentShipIndex.value = 0
        _currentShipSizeToPlace.value = shipsToPlaceSizes[0]
        _isHorizontal.value = true

        val emptyBoard = List(size) { row ->
            List(size) { col -> Cell(position = Position(row, col)) }
        }

        _playerBoardState.value = emptyBoard
    }

    /**
     * Processes a click event on a specific board cell.
     *
     * @param position The position of the clicked cell.
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
        val board = _playerBoardState.value
        val boardSize = board.size

        if ((horizontal && startPos.col + shipSize > boardSize) || (!horizontal && startPos.row + shipSize > boardSize)) return

        var isValidPlacement = true
        val shipPositions = mutableListOf<Position>()

        for (i in 0 until shipSize) {
            val row = if (horizontal) startPos.row else startPos.row + i
            val col = if (horizontal) startPos.col + i else startPos.col

            for (r in (row - 1)..(row + 1)) {
                for (c in (col - 1)..(col + 1)) {
                    if (r in 0 until boardSize && c in 0 until boardSize) {
                        if (board[r][c].hasShip) {
                            isValidPlacement = false
                        }
                    }
                }
            }

            if (!isValidPlacement) break
            shipPositions.add(Position(row, col))
        }

        if (isValidPlacement) {
            val newBoard = board.map { row ->
                row.map { cell ->
                    if (shipPositions.contains(cell.position)) cell.copy(hasShip = true) else cell
                }
            }

            playerShips.add(Ship(shipSize, shipPositions))
            _playerBoardState.value = newBoard

            val nextIndex = _currentShipIndex.value + 1
            if (nextIndex < shipsToPlaceSizes.size) {
                _currentShipIndex.value = nextIndex
                _currentShipSizeToPlace.value = shipsToPlaceSizes[nextIndex]
            } else {
                startGameplay()
            }
        }
    }

    private fun startGameplay() {
        _gamePhase.value = GamePhase.PLAYING
        placeEnemyShipsRandomly(_enemyBoardState.value.size)
        updateEnemyFleetStatus(_enemyBoardState.value)
        startTimer()
    }

    private fun placeEnemyShipsRandomly(boardSize: Int) {
        val tempBoard = _enemyBoardState.value.map { it.toMutableList() }.toMutableList()

        for (shipSize in shipsToPlaceSizes) {
            var isPlaced = false
            var attempts = 0

            while (!isPlaced && attempts < 500) {
                attempts++
                val isHorizontal = Random.nextBoolean()
                val startRow = Random.nextInt(boardSize)
                val startCol = Random.nextInt(boardSize)

                if (isHorizontal && startCol + shipSize > boardSize) continue
                if (!isHorizontal && startRow + shipSize > boardSize) continue

                var isValid = true
                val shipPositions = mutableListOf<Position>()

                for (i in 0 until shipSize) {
                    val row = if (isHorizontal) startRow else startRow + i
                    val col = if (isHorizontal) startCol + i else startCol

                    for (r in (row - 1)..(row + 1)) {
                        for (c in (col - 1)..(col + 1)) {
                            if (r in 0 until boardSize && c in 0 until boardSize) {
                                if (tempBoard[r][c].hasShip) isValid = false
                            }
                        }
                    }
                    if (!isValid) break
                    shipPositions.add(Position(row, col))
                }

                if (isValid) {
                    shipPositions.forEach { pos ->
                        tempBoard[pos.row][pos.col] = tempBoard[pos.row][pos.col].copy(hasShip = true)
                    }
                    enemyShips.add(Ship(shipSize, shipPositions))
                    isPlaced = true
                }
            }
        }
        _enemyBoardState.value = tempBoard
    }

    private fun performPlayerAttack(position: Position) {
        val currentBoard = _enemyBoardState.value
        val clickedCell = currentBoard[position.row][position.col]

        if (clickedCell.state != CellState.HIDDEN) return

        val newState = if (clickedCell.hasShip) CellState.HIT else CellState.MISS
        val newBoard = currentBoard.map { row ->
            row.map { cell ->
                if (cell.position == position) cell.copy(state = newState) else cell
            }
        }

        _enemyBoardState.value = newBoard
        updateEnemyFleetStatus(newBoard)

        val playerWon = enemyShips.all { ship -> ship.isSunk(newBoard) }
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

        val board = _playerBoardState.value
        val size = board.size
        var target: Position? = null

        if (isHardMode && aiTargetQueue.isNotEmpty()) {
            while (aiTargetQueue.isNotEmpty() && target == null) {
                val candidate = aiTargetQueue.removeAt(0)
                if (board[candidate.row][candidate.col].state == CellState.HIDDEN) {
                    target = candidate
                }
            }
        }

        if (target == null) {
            val hiddenCells = mutableListOf<Position>()
            board.forEach { row ->
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

        val clickedCell = board[target.row][target.col]
        val newState = if (clickedCell.hasShip) CellState.HIT else CellState.MISS

        val newBoard = board.map { row ->
            row.map { cell ->
                if (cell.position == target) cell.copy(state = newState) else cell
            }
        }
        _playerBoardState.value = newBoard

        if (newState == CellState.HIT && isHardMode) {
            val adj = listOf(
                Position(target.row - 1, target.col),
                Position(target.row + 1, target.col),
                Position(target.row, target.col - 1),
                Position(target.row, target.col + 1),
            ).filter { it.row in 0 until size && it.col in 0 until size }

            aiTargetQueue.addAll(adj)
        }

        val aiWon = playerShips.all { it.isSunk(newBoard) }
        if (aiWon) {
            endGame(won = false)
        } else {
            _isPlayerTurn.value = true
        }
    }

    private fun updateEnemyFleetStatus(board: List<List<Cell>>) {
        _enemyFleetStatus.value = enemyShips.map { ship ->
            Pair(ship.size, ship.isSunk(board))
        }.sortedByDescending { it.first }
    }

    private fun endGame(won: Boolean) {
        _playerWon.value = won
        _gamePhase.value = GamePhase.GAME_OVER
        _isGameOver.value = true
        timerJob?.cancel()
    }
}