package com.inglada.battleship.model

/**
 * Represents a 2D coordinate on the game grid.
 *
 * @property row The vertical index of the position.
 * @property col The horizontal index of the position.
 */
data class Position(val row: Int, val col: Int)

/**
 * Represents the current visual state of a cell from the player's perspective.
 */
enum class CellState {
    /** The user hasn't interacted with this cell yet. */
    HIDDEN,
    /** The user clicked, but it was water. */
    MISS,
    /** The user clicked and hit a ship part. */
    HIT
}

/**
 * Represents a single square on the game board.
 *
 * @property position The [Position] of the cell on the grid.
 * @property hasShip Whether this cell contains a part of a ship.
 * @property state The current [CellState] of the cell.
 */
data class Cell(
    val position: Position,
    val hasShip: Boolean = false,
    val state: CellState = CellState.HIDDEN
)

/**
 * Represents a ship placed on the game board.
 *
 * @property size The length of the ship in cells.
 * @property positions The list of [Position]s occupied by this ship.
 */
data class Ship(
    val size: Int,
    val positions: List<Position>
) {
    /**
     * Determines if the ship is completely sunk based on the current board state.
     *
     * @param board The current grid of cells representing the board.
     * @return True if all positions occupied by the ship are in the [CellState.HIT] state.
     */
    fun isSunk(board: List<List<Cell>>): Boolean {
        return positions.all { pos ->
            board[pos.row][pos.col].state == CellState.HIT
        }
    }
}