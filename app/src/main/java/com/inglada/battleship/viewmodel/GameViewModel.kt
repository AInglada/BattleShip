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

enum class GamePhase {
    SETUP, PLAYING, GAME_OVER
}

class GameViewModel : ViewModel() {

    // The internal mutable state flow that holds the 2D grid of cells.
    private val _boardState = MutableStateFlow<List<List<Cell>>>(emptyList())

    // The public immutable state flow that the UI will observe.
    val boardState: StateFlow<List<List<Cell>>> = _boardState.asStateFlow()

    // StateFlow to hold the remaining time
    private val _timeLeft = MutableStateFlow(0)
    val timeLeft: StateFlow<Int> = _timeLeft.asStateFlow()

    // We track the specific phase
    private val _gamePhase = MutableStateFlow(GamePhase.SETUP)
    val gamePhase: StateFlow<GamePhase> = _gamePhase.asStateFlow()

    // StateFlow to hold the game status (isPlaying, won, lost)
    private val _isGameOver = MutableStateFlow(false)
    val isGameOver: StateFlow<Boolean> = _isGameOver.asStateFlow()

    // StateFlow to hold the status of the fleet.
    // Pair<Int, Boolean> represents <ShipSize, IsSunk>
    private val _fleetStatus = MutableStateFlow<List<Pair<Int, Boolean>>>(emptyList())
    val fleetStatus: StateFlow<List<Pair<Int, Boolean>>> = _fleetStatus.asStateFlow()

    // --- MANUAL PLACEMENT STATES ---
    private val shipsToPlaceSizes = listOf(5, 4, 3, 3, 2)

    private val _currentShipIndex = MutableStateFlow(0)

    // Tracks the size of the ship currently being placed
    private val _currentShipSizeToPlace = MutableStateFlow(shipsToPlaceSizes[0])
    val currentShipSizeToPlace: StateFlow<Int> = _currentShipSizeToPlace.asStateFlow()

    // Tracks if the user wants to place the ship horizontally or vertically
    private val _isHorizontal = MutableStateFlow(true)
    val isHorizontal: StateFlow<Boolean> = _isHorizontal.asStateFlow()

    // Internal list to keep track of all ships to check win conditions later
    private val placedShips = mutableListOf<Ship>()

    // Flag to check if the board has already been initialized to avoid resetting it when rotating the screen
    private var isInitialized = false

    private var timerJob: Job? = null // Holds the coroutine job for the timer

    // Used to remember if time was enabled so we can start it later
    private var timeWasEnabled = false

    /**
     * Initializes the board with water (HIDDEN state).
     * @param size The size of the grid (e.g., 8 for an 8x8 grid).
     */
    fun initializeBoard(size: Int, isTimeEnabled: Boolean, timeLimit: Int) {
        // We only want to create the board once per game
        if (isInitialized) return

        timeWasEnabled = isTimeEnabled
        // Set the initial time
        if (isTimeEnabled) {
            _timeLeft.value = timeLimit
        }

        val initialBoard = MutableList(size) { row ->
            MutableList(size) { col ->
                Cell(position = Position(row, col))
            }
        }

        _boardState.value = initialBoard
        _gamePhase.value = GamePhase.SETUP
        updateFleetStatus(initialBoard)
        isInitialized = true
    }

    /**
     * Toggles the orientation for the next ship placement.
     */
    fun toggleOrientation() {
        _isHorizontal.value = !_isHorizontal.value
    }

    /**
     * Resets the board during the SETUP phase if the user gets stuck.
     */
    fun resetPlacement(size: Int) {
        // Clear internal lists and reset index
        placedShips.clear()
        _currentShipIndex.value = 0
        _currentShipSizeToPlace.value = shipsToPlaceSizes[0]
        _isHorizontal.value = true

        // Generate a fresh empty board
        val emptyBoard = MutableList(size) { row ->
            MutableList(size) { col ->
                Cell(position = Position(row, col))
            }
        }

        _boardState.value = emptyBoard
    }

    /**
     * Starts the coroutine timer
     */
    private fun startTimer() {
        timerJob?.cancel() // Cancel any existing timer just in case
        timerJob = viewModelScope.launch {
            while (_timeLeft.value > 0 && _gamePhase.value == GamePhase.PLAYING) {
                delay(1000L) // Wait for 1 second
                _timeLeft.value -= 1 // Decrease time

                if (_timeLeft.value == 0) {
                    endGame(won = false) // Time's up
                }
            }
        }
    }

    /**
     * Handles a user clicking on a specific cell.
     * It delegates the action depending on the current Game Phase.
     */
    fun onCellClicked(position: Position) {
        when (_gamePhase.value) {
            GamePhase.SETUP -> tryPlaceShipManually(position)
            GamePhase.PLAYING -> performAttack(position)
            GamePhase.GAME_OVER -> return // Do nothing
        }
    }

    /**
     * Attempts to place the current ship at the clicked position.
     */
    private fun tryPlaceShipManually(startPos: Position) {
        if (_currentShipIndex.value >= shipsToPlaceSizes.size) return

        val shipSize = shipsToPlaceSizes[_currentShipIndex.value]
        val horizontal = _isHorizontal.value
        val board = _boardState.value
        val boardSize = board.size

        // 1. Check if it goes out of bounds
        if (horizontal && startPos.col + shipSize > boardSize) return
        if (!horizontal && startPos.row + shipSize > boardSize) return

        // 2. Check overlap and "no touching" rules
        var isValidPlacement = true
        val shipPositions = mutableListOf<Position>()

        for (i in 0 until shipSize) {
            val row = if (horizontal) startPos.row else startPos.row + i
            val col = if (horizontal) startPos.col + i else startPos.col

            // Check surrounding cells (including diagonals)
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

        // 3. If valid, place it
        if (isValidPlacement) {
            val newBoard = board.map { row ->
                row.map { cell ->
                    if (shipPositions.contains(cell.position)) cell.copy(hasShip = true) else cell
                }
            }

            placedShips.add(Ship(shipSize, shipPositions))
            _boardState.value = newBoard

            // 4. Move to the next ship, or start the game if all are placed
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
        updateFleetStatus(_boardState.value) // Populate the HUD

        // Now we start the timer
        if (timeWasEnabled) {
            startTimer()
        }
    }

    private fun performAttack(position: Position) {
        val currentBoard = _boardState.value
        val clickedCell = currentBoard[position.row][position.col]

        if (clickedCell.state != CellState.HIDDEN) return

        val newState = if (clickedCell.hasShip) CellState.HIT else CellState.MISS

        val newBoard = currentBoard.map { row ->
            row.map { cell ->
                if (cell.position == position) cell.copy(state = newState) else cell
            }
        }

        _boardState.value = newBoard
        updateFleetStatus(newBoard)
        checkWinCondition(newBoard)
    }

    /**
     * Checks if all placed ships are fully sunk.
     */
    private fun checkWinCondition(board: List<List<Cell>>) {
        val allSunk = placedShips.all { ship -> ship.isSunk(board) }
        if (allSunk) endGame(won = true)
    }

    private fun endGame(won: Boolean) {
        _gamePhase.value = GamePhase.GAME_OVER
        _isGameOver.value = true
        timerJob?.cancel()
    }

    /**
     * Updates the UI state of the fleet (which ships are alive or sunk)
     */
    private fun updateFleetStatus(board: List<List<Cell>>) {
        _fleetStatus.value = placedShips.map { ship ->
            Pair(ship.size, ship.isSunk(board))
        }.sortedByDescending { it.first } // Sort from largest to smallest ship
    }
}