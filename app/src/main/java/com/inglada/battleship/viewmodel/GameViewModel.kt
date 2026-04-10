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

class GameViewModel : ViewModel() {

    // The internal mutable state flow that holds the 2D grid of cells.
    private val _boardState = MutableStateFlow<List<List<Cell>>>(emptyList())

    // The public immutable state flow that the UI will observe.
    val boardState: StateFlow<List<List<Cell>>> = _boardState.asStateFlow()

    // StateFlow to hold the remaining time
    private val _timeLeft = MutableStateFlow(0)
    val timeLeft: StateFlow<Int> = _timeLeft.asStateFlow()

    // StateFlow to hold the game status (isPlaying, won, lost)
    private val _isGameOver = MutableStateFlow(false)
    val isGameOver: StateFlow<Boolean> = _isGameOver.asStateFlow()

    // Internal list to keep track of all ships to check win conditions later
    private val placedShips = mutableListOf<Ship>()

    // Flag to check if the board has already been initialized to avoid resetting it when rotating the screen
    private var isInitialized = false

    private var timerJob: Job? = null // Holds the coroutine job for the timer

    /**
     * Initializes the board with water (HIDDEN state).
     * @param size The size of the grid (e.g., 8 for an 8x8 grid).
     */
    fun initializeBoard(size: Int, isTimeEnabled: Boolean, timeLimit: Int) {
        // We only want to create the board once per game
        if (isInitialized) return

        // Set the initial time
        if (isTimeEnabled) {
            _timeLeft.value = timeLimit
            startTimer() // Start the countdown
        }

        // 1. Create a mutable 2D list for easy modification during setup
        val initialBoard = MutableList(size) { row ->
            MutableList(size) { col ->
                Cell(position = Position(row, col))
            }
        }

        // 2. Define the fleet (Ship sizes: Carrier=5, Battleship=4, Cruiser=3, Submarine=3, Destroyer=2)
        // We ensure the grid is at least 6x6 in our ConfigScreen, so these will fit.
        val fleetSizes = listOf(5, 4, 3, 3, 2)

        // 3. Place each ship randomly
        for (shipSize in fleetSizes) {
            placeShipRandomly(shipSize, size, initialBoard)
        }

        // 4. Expose the immutable board to the UI
        _boardState.value = initialBoard
        isInitialized = true
    }

    /**
     * Tries to place a single ship on the board at random coordinates.
     */
    private fun placeShipRandomly(shipSize: Int, boardSize: Int, board: MutableList<MutableList<Cell>>) {
        var isPlaced = false
        val maxAttempts = 100 // Prevent infinite loops if the board is too crowded
        var attempts = 0

        while (!isPlaced && attempts < maxAttempts) {
            attempts++
            val isHorizontal = Random.nextBoolean()
            val startRow = Random.nextInt(boardSize)
            val startCol = Random.nextInt(boardSize)

            // Check boundaries
            if (isHorizontal && startCol + shipSize > boardSize) continue
            if (!isHorizontal && startRow + shipSize > boardSize) continue

            var isValidPlacement = true
            val shipPositions = mutableListOf<Position>()

            for (i in 0 until shipSize) {
                val row = if (isHorizontal) startRow else startRow + i
                val col = if (isHorizontal) startCol + i else startCol

                // Check surrounding cells (including diagonals)
                // We check from row-1 to row+1, and col-1 to col+1
                for (r in (row - 1)..(row + 1)) {
                    for (c in (col - 1)..(col + 1)) {
                        // Ensure we don't go out of bounds while checking surroundings
                        if (r in 0 until boardSize && c in 0 until boardSize) {
                            if (board[r][c].hasShip) {
                                isValidPlacement = false
                            }
                        }
                    }
                }

                if (!isValidPlacement) break // Break early if we found a conflict
                shipPositions.add(Position(row, col))
            }

            // If it fits and has water all around it, place it
            if (isValidPlacement) {
                shipPositions.forEach { pos ->
                    board[pos.row][pos.col] = board[pos.row][pos.col].copy(hasShip = true)
                }
                placedShips.add(Ship(shipSize, shipPositions))
                isPlaced = true
            }
        }
    }

    /**
     * Starts the coroutine timer
     */
    private fun startTimer() {
        timerJob?.cancel() // Cancel any existing timer just in case
        timerJob = viewModelScope.launch {
            while (_timeLeft.value > 0 && !_isGameOver.value) {
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
     */
    fun onCellClicked(position: Position) {
        val currentBoard = _boardState.value
        val clickedCell = currentBoard[position.row][position.col]

        // If the cell is already revealed, do nothing
        if (clickedCell.state != CellState.HIDDEN) return

        // Determine if it's a hit or a miss
        val newState = if (clickedCell.hasShip) CellState.HIT else CellState.MISS

        // Create a completely new board to respect Jetpack Compose immutability rules.
        val newBoard = currentBoard.map { row ->
            row.map { cell ->
                if (cell.position == position) {
                    cell.copy(state = newState)
                } else {
                    cell
                }
            }
        }

        // Emit the new board state
        _boardState.value = newBoard

        // Check if this move won the game
        checkWinCondition(newBoard)
    }

    /**
     * Checks if all placed ships are fully sunk.
     */
    private fun checkWinCondition(board: List<List<Cell>>) {
        val allSunk = placedShips.all { ship ->
            ship.isSunk(board)
        }

        if (allSunk) {
            endGame(won = true)
        }
    }

    private fun endGame(won: Boolean) {
        _isGameOver.value = true
        timerJob?.cancel() // Stop the timer

        if (won) {
            println("GAME OVER - YOU WIN!")
        } else {
            println("GAME OVER - TIME OUT!")
        }
    }
}