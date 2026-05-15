package com.inglada.battleship.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Extension property to instantiate the DataStore singleton at the Context level.
 */
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

/**
 * Repository responsible for managing user configuration data using Preferences DataStore.
 * Provides asynchronous streams (Flows) to observe preferences and suspend functions to update them.
 *
 * @property dataStore The internal [DataStore] instance used for persistence.
 */
class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {

    private companion object {
        val PLAYER_NAME = stringPreferencesKey("player_name")
        val GRID_SIZE = intPreferencesKey("grid_size")
        val IS_TIME_ENABLED = booleanPreferencesKey("is_time_enabled")
        val TIME_LIMIT = intPreferencesKey("time_limit")
        val IS_HARD_MODE = booleanPreferencesKey("is_hard_mode")
    }

    /** Emits the saved player alias, defaulting to "Commander" if not set. */
    val playerName: Flow<String> = dataStore.data.map { it[PLAYER_NAME] ?: "Commander" }

    /** Emits the saved grid dimension, defaulting to 8 if not set. */
    val gridSize: Flow<Int> = dataStore.data.map { it[GRID_SIZE] ?: 8 }

    /** Emits whether the turn time limit is enabled, defaulting to false. */
    val isTimeEnabled: Flow<Boolean> = dataStore.data.map { it[IS_TIME_ENABLED] ?: false }

    /** Emits the time limit duration in seconds, defaulting to 60. */
    val timeLimit: Flow<Int> = dataStore.data.map { it[TIME_LIMIT] ?: 60 }

    /** Emits whether the AI tactical mode is enabled, defaulting to false. */
    val isHardMode: Flow<Boolean> = dataStore.data.map { it[IS_HARD_MODE] ?: false }

    /**
     * Persists the user's game configuration to the DataStore.
     *
     * @param name The player's alias.
     * @param size The grid dimension (e.g., 8 for an 8x8 board).
     * @param timeEnabled True if the turn timer should be active.
     * @param limit The duration of the timer in seconds.
     * @param hardMode True if the AI should use tactical targeting.
     */
    suspend fun savePreferences(
        name: String,
        size: Int,
        timeEnabled: Boolean,
        limit: Int,
        hardMode: Boolean
    ) {
        dataStore.edit { prefs ->
            prefs[PLAYER_NAME] = name
            prefs[GRID_SIZE] = size
            prefs[IS_TIME_ENABLED] = timeEnabled
            prefs[TIME_LIMIT] = limit
            prefs[IS_HARD_MODE] = hardMode
        }
    }
}