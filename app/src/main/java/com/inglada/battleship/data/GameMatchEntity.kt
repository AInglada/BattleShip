package com.inglada.battleship.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.inglada.battleship.model.MoveLog
import kotlinx.parcelize.Parcelize

/**
 * Database entity representing a completed game match.
 *
 * This class is stored in the Room database to maintain a history of all games played.
 * It is marked as [Parcelize] to allow passing it through navigation components if needed.
 *
 * @property id Unique identifier for the match (auto-generated).
 * @property playerName Alias of the player who played the match.
 * @property timestamp Epoch time when the match was recorded.
 * @property gridSize The size of the square grid (e.g., 8 for an 8x8 grid).
 * @property timeSpent Total duration of the match in seconds.
 * @property outcome Descriptive string of the result (e.g., "Victory", "Defeat (AI)").
 * @property moveLogs List of all moves made during the match, serialized as JSON via TypeConverters.
 */
@Parcelize
@Entity(tableName = "game_matches")
data class GameMatchEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val playerName: String,
    val timestamp: Long,
    val gridSize: Int,
    val timeSpent: Int,
    val outcome: String,
    val moveLogs: List<MoveLog> = emptyList()
) : Parcelable