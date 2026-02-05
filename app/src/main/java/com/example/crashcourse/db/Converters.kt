package com.example.crashcourse.utils

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class Converters {
    // --- Waktu (LocalDateTime <-> Long) ---
    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDateTime? {
        return value?.let {
            LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault())
        }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDateTime?): Long? {
        return date?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    }

    // --- Data Wajah (FloatArray <-> String JSON) ---
    @TypeConverter
    fun fromString(value: String?): FloatArray? {
        return if (value == null) null else Gson().fromJson(value, FloatArray::class.java)
    }

    @TypeConverter
    fun fromFloatArray(list: FloatArray?): String? {
        return if (list == null) null else Gson().toJson(list)
    }

    // --- List String (Opsional) ---
    @TypeConverter
    fun fromStringList(value: String?): List<String>? {
        if (value == null) return null
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromListString(list: List<String>?): String? {
        return Gson().toJson(list)
    }
}