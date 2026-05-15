package com.inglada.battleship.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.inglada.battleship.model.MoveLog
import kotlinx.parcelize.Parcelize

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