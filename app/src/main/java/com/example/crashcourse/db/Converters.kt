package com.example.crashcourse.utils

import androidx.room.TypeConverter
import com.google.gson.Gson
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class Converters {
    // --- 🕒 Waktu (LocalDateTime <-> Long) ---
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

    // --- 👤 Data Wajah (FloatArray <-> String) ---
    // Kita gunakan cara paling standar agar presisi Float tidak hilang
    @TypeConverter
    fun fromString(value: String?): FloatArray? {
        if (value.isNullOrEmpty()) return null
        return try {
            // Gunakan instance Gson baru di sini agar tidak ada state yang tertinggal
            Gson().fromJson(value, FloatArray::class.java)
        } catch (e: Exception) {
            null
        }
    }

    @TypeConverter
    fun fromFloatArray(array: FloatArray?): String? {
        if (array == null) return null
        return try {
            Gson().toJson(array)
        } catch (e: Exception) {
            null
        }
    }
}