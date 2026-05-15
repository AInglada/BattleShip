package com.inglada.battleship.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents a single move made during the game to be recorded in the match log.
 *
 * @property isPlayer True if the move was made by the human player, false if by the AI.
 * @property row The grid row targeted.
 * @property col The grid column targeted.
 * @property result The outcome of the move (e.g., "Miss", "Hit", "Invalid").
 * @property timeRemaining The seconds left on the clock when the move was made (null if infinite time).
 * @property timestamp The exact millisecond the move occurred.
 */
@Parcelize
data class MoveLog(
    val isPlayer: Boolean,
    val row: Int,
    val col: Int,
    val result: String,
    val timeRemaining: Int?,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable