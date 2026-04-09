package com.inglada.battleship.viewmodel

import androidx.lifecycle.ViewModel
import com.inglada.battleship.model.Cell
import com.inglada.battleship.model.Position
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameViewModel : ViewModel() {

    // The internal mutable state flow that holds the 2D grid of cells.
    private val _boardState = MutableStateFlow<List<List<Cell>>>(emptyList())

    // The public immutable state flow that the UI will observe.
    val boardState: StateFlow<List<List<Cell>>> = _boardState.asStateFlow()

    // Flag to check if the board has already been initialized to avoid resetting it when rotating the screen
    private var isInitialized = false

    /**
     * Initializes the board with water (HIDDEN state).
     * @param size The size of the grid (e.g., 8 for an 8x8 grid).
     */
    fun initializeBoard(size: Int) {
        // We only want to create the board once per game
        if (isInitialized) return

        // Create a 2D list (matrix) representing the grid
        val initialBoard = List(size) { row ->
            List(size) { col ->
                // Create an empty cell at each coordinate
                Cell(position = Position(row, col))
            }
        }

        // Update the state flow so the UI knows the board is ready
        _boardState.value = initialBoard
        isInitialized = true

        // TODO: Call a function here to place the ships randomly
    }

    /**
     * Handles a user clicking on a specific cell.
     */
    fun onCellClicked(position: Position) {
        // TODO: Implement the logic to change cell state (HIT or MISS)
    }
}