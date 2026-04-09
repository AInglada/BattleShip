package com.inglada.battleship.model

// Represents a 2D coordinate on the game grid
data class Position(val row: Int, val col: Int)

// Represents the current visual state of a cell from the player's perspective
enum class CellState {
    HIDDEN, // The user hasn't clicked here yet
    MISS,   // The user clicked, but it was just water
    HIT     // The user clicked, and hit a piece of a ship
}

// Represents a single square on the board.
data class Cell(
    val position: Position,
    val hasShip: Boolean = false,
    val state: CellState = CellState.HIDDEN
)

// Represents a ship placed on the board
data class Ship(
    val size: Int,
    val positions: List<Position>
) {
    // Helper function to check if this specific ship is completely sunk
    // We pass the current board cells to check if all ship positions are in 'HIT' state
    fun isSunk(board: List<List<Cell>>): Boolean {
        return positions.all { pos ->
            board[pos.row][pos.col].state == CellState.HIT
        }
    }
}