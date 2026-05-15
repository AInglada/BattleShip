package com.inglada.battleship.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Main database configuration class for the application.
 * Uses the Room persistence library to manage the SQLite database.
 */
@Database(entities = [GameMatchEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class BattleshipDatabase : RoomDatabase() {

    /**
     * Provides access to the Data Access Object (DAO) for game matches.
     *
     * @return The [GameMatchDao] instance.
     */
    abstract fun gameMatchDao(): GameMatchDao

    companion object {
        @Volatile
        private var INSTANCE: BattleshipDatabase? = null

        /**
         * Retrieves the singleton instance of the database.
         * Creates it if it doesn't exist using a thread-safe implementation.
         *
         * @param context The application context.
         * @return The [BattleshipDatabase] singleton.
         */
        fun getDatabase(context: Context): BattleshipDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BattleshipDatabase::class.java,
                    "battleship_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}