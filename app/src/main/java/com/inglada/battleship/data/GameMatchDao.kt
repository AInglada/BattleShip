package com.inglada.battleship.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for the [GameMatchEntity] table.
 * Provides the necessary database operations to save and retrieve match history.
 */
@Dao
interface GameMatchDao {

    /**
     * Inserts a new game match record into the database.
     *
     * @param match The [GameMatchEntity] to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: GameMatchEntity)

    /**
     * Retrieves all saved game matches, ordered by the most recent first.
     * Returns a [Flow] to automatically update the UI whenever the database changes.
     *
     * @return A stream of the match history list.
     */
    @Query("SELECT * FROM game_matches ORDER BY timestamp DESC")
    fun getAllMatches(): Flow<List<GameMatchEntity>>
}