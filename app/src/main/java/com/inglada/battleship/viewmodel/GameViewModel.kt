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

    // --- GAME BOARDS ---

    // The internal mutable state flow that holds the Player's 2D grid of cells.
    private val _playerBoardState = MutableStateFlow<List<List<Cell>>>(emptyList())
    // The public immutable state flow that the UI will observe for the Player.
    val playerBoardState: StateFlow<List<List<Cell>>> = _playerBoardState.asStateFlow()

    // The internal mutable state flow that holds the Enemy (AI) 2D grid of cells.
    private val _enemyBoardState = MutableStateFlow<List<List<Cell>>>(emptyList())
    // The public immutable state flow that the UI will observe for the Enemy.
    val enemyBoardState: StateFlow<List<List<Cell>>> = _enemyBoardState.asStateFlow()

    // --- GAME STATES ---

    // StateFlow to hold the remaining time
    private val _timeLeft = MutableStateFlow(0)
    val timeLeft: StateFlow<Int> = _timeLeft.asStateFlow()

    private val _totalTimeSpent = MutableStateFlow(0)
    val totalTimeSpent: StateFlow<Int> = _totalTimeSpent.asStateFlow()

    // We track the specific phase (SETUP, PLAYING, GAME_OVER)
    private val _gamePhase = MutableStateFlow(GamePhase.SETUP)
    val gamePhase: StateFlow<GamePhase> = _gamePhase.asStateFlow()

    // StateFlow to hold the game status (isPlaying, won, lost)
    private val _isGameOver = MutableStateFlow(false)
    val isGameOver: StateFlow<Boolean> = _isGameOver.asStateFlow()

    private val _playerWon = MutableStateFlow(false)
    val playerWon: StateFlow<Boolean> = _playerWon.asStateFlow()

    private var baseTimeLimit = 0

    // StateFlow to manage the turn system between Player and AI
    private val _isPlayerTurn = MutableStateFlow(true)
    val isPlayerTurn: StateFlow<Boolean> = _isPlayerTurn.asStateFlow()

    // StateFlow to hold the status of the enemy fleet for the HUD.
    // Pair<Int, Boolean> represents <ShipSize, IsSunk>
    private val _enemyFleetStatus = MutableStateFlow<List<Pair<Int, Boolean>>>(emptyList())
    val enemyFleetStatus: StateFlow<List<Pair<Int, Boolean>>> = _enemyFleetStatus.asStateFlow()

    // --- MANUAL PLACEMENT & FLEET STATES ---

    private val shipsToPlaceSizes = listOf(5, 4, 3, 3, 2)
    private val _currentShipIndex = MutableStateFlow(0)

    // Tracks the size of the ship currently being placed by the player
    private val _currentShipSizeToPlace = MutableStateFlow(shipsToPlaceSizes[0])
    val currentShipSizeToPlace: StateFlow<Int> = _currentShipSizeToPlace.asStateFlow()

    // Tracks if the user wants to place the ship horizontally or vertically
    private val _isHorizontal = MutableStateFlow(true)
    val isHorizontal: StateFlow<Boolean> = _isHorizontal.asStateFlow()

    // Internal lists to keep track of all ships to check win conditions later
    private val playerShips = mutableListOf<Ship>()
    private val enemyShips = mutableListOf<Ship>()

    // --- INTERNAL CONTROLS & AI SETTINGS ---

    // Flag to check if the board has already been initialized to avoid resetting it when rotating the screen
    private var isInitialized = false
    private var timerJob: Job? = null // Holds the coroutine job for the timer
    private var timeWasEnabled = false // Used to remember if time was enabled so we can start it later

    // AI Difficulty Flag
    private var isHardMode = false
    // Queue to hold adjacent target positions when the AI hits a ship (Hard Mode)
    private val aiTargetQueue = mutableListOf<Position>()

    /**
     * Initializes both boards with water (HIDDEN state).
     * @param size The size of the grid (e.g., 8 for an 8x8 grid).
     * @param isTimeEnabled Whether the time limit is active.
     * @param timeLimit The maximum time in seconds.
     * @param hardMode Whether the AI will use tactical targeting.
     */
    fun initializeBoard(size: Int, isTimeEnabled: Boolean, timeLimit: Int, hardMode: Boolean) {
        // We only want to create the board once per game
        if (isInitialized) return

        timeWasEnabled = isTimeEnabled
        isHardMode = hardMode

        // Set the initial time
        baseTimeLimit = timeLimit
        if (isTimeEnabled) {
            _timeLeft.value = timeLimit
        }

        // Initialize both boards with hidden water
        val emptyBoard = List(size) { row ->
            List(size) { col -> Cell(position = Position(row, col)) }
        }

        _playerBoardState.value = emptyBoard
        _enemyBoardState.value = emptyBoard
        _gamePhase.value = GamePhase.SETUP

        isInitialized = true
    }

    /**
     * Toggles the orientation for the next ship placement.
     */
    fun toggleOrientation() {
        _isHorizontal.value = !_isHorizontal.value
    }

    /**
     * Resets the player's board during the SETUP phase if the user gets stuck.
     */
    fun resetPlacement(size: Int) {
        // Clear internal lists and reset index
        playerShips.clear()
        _currentShipIndex.value = 0
        _currentShipSizeToPlace.value = shipsToPlaceSizes[0]
        _isHorizontal.value = true

        // Generate a fresh empty board for the player
        val emptyBoard = List(size) { row ->
            List(size) { col -> Cell(position = Position(row, col)) }
        }

        _playerBoardState.value = emptyBoard
    }

    /**
     * Starts the coroutine timer for the gameplay phase.
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
                        endGame(won = false)
                    }
                }
            }
        }
    }

    /**
     * Handles a user clicking on a specific cell.
     * It delegates the action depending on the current Game Phase and Turn.
     */
    fun onCellClicked(position: Position) {
        when (_gamePhase.value) {
            GamePhase.SETUP -> tryPlaceShipManually(position)
            GamePhase.PLAYING -> {
                // The player can only attack during their turn
                if (_isPlayerTurn.value) performPlayerAttack(position)
            }
            GamePhase.GAME_OVER -> return // Do nothing
        }
    }

    /**
     * Attempts to place the current ship at the clicked position on the Player's board.
     */
    private fun tryPlaceShipManually(startPos: Position) {
        if (_currentShipIndex.value >= shipsToPlaceSizes.size) return

        val shipSize = shipsToPlaceSizes[_currentShipIndex.value]
        val horizontal = _isHorizontal.value
        val board = _playerBoardState.value
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

            playerShips.add(Ship(shipSize, shipPositions))
            _playerBoardState.value = newBoard

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

    /**
     * Transitions from SETUP to PLAYING phase.
     * Generates the AI board, populates the HUD, and starts the timer.
     */
    private fun startGameplay() {
        _gamePhase.value = GamePhase.PLAYING
        placeEnemyShipsRandomly(_enemyBoardState.value.size)
        updateEnemyFleetStatus(_enemyBoardState.value) // Populate the HUD
        startTimer()
    }

    /**
     * Generates random valid positions for the AI fleet applying the same strict rules.
     */
    private fun placeEnemyShipsRandomly(boardSize: Int) {
        val tempBoard = _enemyBoardState.value.map { it.toMutableList() }.toMutableList()

        for (shipSize in shipsToPlaceSizes) {
            var isPlaced = false
            var attempts = 0

            // Try to place the ship randomly up to 500 times to avoid infinite loops
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

                    // Check surrounding cells
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

    /**
     * Handles the logic when the player shoots at the enemy board.
     */
    private fun performPlayerAttack(position: Position) {
        val currentBoard = _enemyBoardState.value
        val clickedCell = currentBoard[position.row][position.col]

        // Only allow clicking hidden water cells
        if (clickedCell.state != CellState.HIDDEN) return

        val newState = if (clickedCell.hasShip) CellState.HIT else CellState.MISS
        val newBoard = currentBoard.map { row ->
            row.map { cell ->
                if (cell.position == position) cell.copy(state = newState) else cell
            }
        }

        _enemyBoardState.value = newBoard
        updateEnemyFleetStatus(newBoard)

        // Check if Player Won
        val playerWon = enemyShips.all { ship -> ship.isSunk(newBoard) }
        if (playerWon) {
            endGame(won = true)
        } else {
            if (timeWasEnabled) _timeLeft.value = baseTimeLimit
            // Pass turn to AI and simulate thinking time
            _isPlayerTurn.value = false
            viewModelScope.launch {
                delay(1000L) // AI "thinks" for 1 second
                performAIAttack()
            }
        }
    }

    /**
     * Calculates and executes the AI's move against the Player's board.
     */
    private fun performAIAttack() {
        if (_gamePhase.value != GamePhase.PLAYING || _isGameOver.value) return

        val board = _playerBoardState.value
        val size = board.size
        var target: Position? = null

        // HARD MODE: Tactical targeting (hit adjacent cells if a ship was previously found)
        if (isHardMode && aiTargetQueue.isNotEmpty()) {
            while (aiTargetQueue.isNotEmpty() && target == null) {
                val candidate = aiTargetQueue.removeAt(0)
                if (board[candidate.row][candidate.col].state == CellState.HIDDEN) {
                    target = candidate
                }
            }
        }

        // EASY MODE or FALLBACK: Random targeting from available hidden cells
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

        // Execute Attack
        val clickedCell = board[target.row][target.col]
        val newState = if (clickedCell.hasShip) CellState.HIT else CellState.MISS

        val newBoard = board.map { row ->
            row.map { cell ->
                if (cell.position == target) cell.copy(state = newState) else cell
            }
        }
        _playerBoardState.value = newBoard

        // If Hard mode is on and the AI hit a ship, add neighbors to the hunting queue
        if (newState == CellState.HIT && isHardMode) {
            val adj = listOf(
                Position(target!!.row - 1, target.col),
                Position(target.row + 1, target.col),
                Position(target.row, target.col - 1),
                Position(target.row, target.col + 1)
            ).filter { it.row in 0 until size && it.col in 0 until size } // Ensure they are inside bounds

            aiTargetQueue.addAll(adj)
        }

        // Check if AI Won
        val aiWon = playerShips.all { it.isSunk(newBoard) }
        if (aiWon) {
            endGame(won = false)
        } else {
            _isPlayerTurn.value = true // Pass turn back to player
        }
    }

    /**
     * Updates the UI state of the enemy fleet (which ships are alive or sunk).
     */
    private fun updateEnemyFleetStatus(board: List<List<Cell>>) {
        _enemyFleetStatus.value = enemyShips.map { ship ->
            Pair(ship.size, ship.isSunk(board))
        }.sortedByDescending { it.first } // Sort from largest to smallest ship
    }

    /**
     * Ends the game and stops the timer.
     */
    private fun endGame(won: Boolean) {
        _playerWon.value = won
        _gamePhase.value = GamePhase.GAME_OVER
        _isGameOver.value = true
        timerJob?.cancel()
    }
}