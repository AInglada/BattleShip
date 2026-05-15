package com.inglada.battleship.data

import kotlinx.coroutines.flow.Flow

/**
 * Repository module for handling game match data operations.
 * Acts as a clean API over the [GameMatchDao], hiding the data source implementation from ViewModels.
 *
 * @property gameMatchDao The DAO interface for database operations.
 */
class MatchRepository(private val gameMatchDao: GameMatchDao) {

    /**
     * A reactive stream (Flow) of all saved matches, ordered by most recent.
     */
    val allMatches: Flow<List<GameMatchEntity>> = gameMatchDao.getAllMatches()

    /**
     * Inserts a completed game match into the local database asynchronously.
     *
     * @param match The [GameMatchEntity] representing the finished game.
     */
    suspend fun insertMatch(match: GameMatchEntity) {
        gameMatchDao.insertMatch(match)
    }
}