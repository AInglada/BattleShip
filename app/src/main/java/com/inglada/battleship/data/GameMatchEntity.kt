package com.inglada.battleship.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a single game match record in the local SQLite database.
 * * @property id The unique auto-generated identifier for the match.
 * @property playerName The alias of the player.
 * @property timestamp The exact date and time the match finished (in milliseconds).
 * @property gridSize The dimension of the board played on.
 * @property timeSpent The duration of the match in seconds.
 * @property outcome A string indicating the result (e.g., "Victory", "Defeat (AI)", "Defeat (Timeout)").
 */
@Entity(tableName = "game_matches")
data class GameMatchEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val playerName: String,
    val timestamp: Long,
    val gridSize: Int,
    val timeSpent: Int,
    val outcome: String
)