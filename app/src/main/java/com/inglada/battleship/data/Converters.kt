package com.inglada.battleship.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.inglada.battleship.model.MoveLog

/**
 * Type converters for the Room database to handle custom data types.
 * Converts complex objects (like Lists) into JSON Strings for storage, and vice versa.
 */
class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromMoveLogList(value: List<MoveLog>?): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toMoveLogList(value: String): List<MoveLog> {
        val listType = object : TypeToken<List<MoveLog>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }
}