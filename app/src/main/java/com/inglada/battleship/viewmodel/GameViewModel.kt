package com.inglada.battleship.viewmodel

import androidx.lifecycle.ViewModel
import com.inglada.battleship.model.Cell
import com.inglada.battleship.model.Position
import com.inglada.battleship.model.Ship
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

class GameViewModel : ViewModel() {

    // The internal mutable state flow that holds the 2D grid of cells.
    private val _boardState = MutableStateFlow<List<List<Cell>>>(emptyList())

    // The public immutable state flow that the UI will observe.
    val boardState: StateFlow<List<List<Cell>>> = _boardState.asStateFlow()

    // Internal list to keep track of all ships to check win conditions later
    private val placedShips = mutableListOf<Ship>()

    // Flag to check if the board has already been initialized to avoid resetting it when rotating the screen
    private var isInitialized = false

    /**
     * Initializes the board with water (HIDDEN state).
     * @param size The size of the grid (e.g., 8 for an 8x8 grid).
     */
    fun initializeBoard(size: Int) {
        // We only want to create the board once per game
        if (isInitialized) return

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

        while (!isPlaced) {
            val isHorizontal = Random.nextBoolean()
            val startRow = Random.nextInt(boardSize)
            val startCol = Random.nextInt(boardSize)

            // Check if the ship fits within the board boundaries
            if (isHorizontal && startCol + shipSize > boardSize) continue
            if (!isHorizontal && startRow + shipSize > boardSize) continue

            // Check for overlaps with already placed ships
            var hasOverlap = false
            val shipPositions = mutableListOf<Position>()

            for (i in 0 until shipSize) {
                val row = if (isHorizontal) startRow else startRow + i
                val col = if (isHorizontal) startCol + i else startCol

                if (board[row][col].hasShip) {
                    hasOverlap = true
                    break
                }
                shipPositions.add(Position(row, col))
            }

            // If it fits and there's no overlap, place the ship
            if (!hasOverlap) {
                shipPositions.forEach { pos ->
                    // Copy the cell with 'hasShip = true'
                    board[pos.row][pos.col] = board[pos.row][pos.col].copy(hasShip = true)
                }

                // Save the ship to our internal list
                placedShips.add(Ship(shipSize, shipPositions))
                isPlaced = true
            }
        }
    }

    /**
     * Handles a user clicking on a specific cell.
     */
    fun onCellClicked(position: Position) {
        // TODO: Implement the logic to change cell state (HIT or MISS)
    }
}