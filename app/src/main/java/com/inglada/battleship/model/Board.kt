package com.inglada.battleship.model

/**
 * Represents the game board, wrapping a 2D grid of cells and providing
 * logic for ship placement and validation.
 *
 * @property grid The 2D list of [Cell] objects representing the board.
 */
data class Board(
    val grid: List<List<Cell>> = emptyList()
) {
    /** The dimensions of the square grid. */
    val size: Int = grid.size

    /**
     * Checks if a ship can be placed at the specified position and orientation.
     * Validates boundaries and ensures no overlap or diagonal/adjacent contact with other ships.
     *
     * @param startPos The starting [Position] of the ship.
     * @param shipSize The length of the ship.
     * @param horizontal Whether the ship is oriented horizontally.
     * @return True if the placement is valid, false otherwise.
     */
    fun canPlaceShip(startPos: Position, shipSize: Int, horizontal: Boolean): Boolean {
        if (horizontal && startPos.col + shipSize > size) return false
        if (!horizontal && startPos.row + shipSize > size) return false

        for (i in 0 until shipSize) {
            val row = if (horizontal) startPos.row else startPos.row + i
            val col = if (horizontal) startPos.col + i else startPos.col

            // Check the 3x3 area around each cell for existing ships
            for (r in (row - 1)..(row + 1)) {
                for (c in (col - 1)..(col + 1)) {
                    if (r in 0 until size && c in 0 until size) {
                        if (grid[r][c].hasShip) return false
                    }
                }
            }
        }
        return true
    }

    /**
     * Places a ship on the board and returns a new [Board] instance with the updated grid.
     *
     * @param startPos The starting [Position] of the ship.
     * @param shipSize The length of the ship.
     * @param horizontal Whether the ship is oriented horizontally.
     * @return A new [Board] containing the placed ship.
     * @throws IllegalArgumentException if the placement is invalid.
     */
    fun placeShip(startPos: Position, shipSize: Int, horizontal: Boolean): Board {
        require(canPlaceShip(startPos, shipSize, horizontal)) { "Invalid ship placement" }

        val shipPositions = (0 until shipSize).map { i ->
            val row = if (horizontal) startPos.row else startPos.row + i
            val col = if (horizontal) startPos.col + i else startPos.col
            Position(row, col)
        }

        val newGrid = grid.map { row ->
            row.map { cell ->
                if (shipPositions.contains(cell.position)) cell.copy(hasShip = true) else cell
            }
        }
        return copy(grid = newGrid)
    }

    /**
     * Updates the state of a cell at the specified position.
     *
     * @param position The [Position] of the cell to update.
     * @param newState The new [CellState] for the cell.
     * @return A new [Board] instance with the updated cell.
     */
    fun updateCell(position: Position, newState: CellState): Board {
        val newGrid = grid.map { row ->
            row.map { cell ->
                if (cell.position == position) cell.copy(state = newState) else cell
            }
        }
        return copy(grid = newGrid)
    }

    /**
     * Retrieves the cell at the given position.
     *
     * @param position The position to query.
     * @return The [Cell] at that position.
     */
    fun getCell(position: Position): Cell = grid[position.row][position.col]

    companion object {
        /**
         * Creates an empty square board of the specified size.
         *
         * @param size The dimensions of the board.
         * @return A new [Board] instance with empty cells.
         */
        fun createEmpty(size: Int): Board {
            val grid = List(size) { row ->
                List(size) { col -> Cell(Position(row, col)) }
            }
            return Board(grid)
        }
    }
}
