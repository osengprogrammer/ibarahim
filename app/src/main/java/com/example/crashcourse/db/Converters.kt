package com.example.crashcourse.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 🔄 Azura Tech Type Converters
 * Bertugas mengubah tipe data kompleks menjadi format yang dipahami SQLite (String/Long).
 */
class Converters {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private val gson = Gson()

    // --- 🕒 LocalDateTime Converters (Untuk Log Absensi) ---
    // Mengubah String dari DB kembali menjadi objek waktu Kotlin
    @TypeConverter
    fun fromTimestamp(value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(it, formatter) }
    }

    // Mengubah objek waktu Kotlin menjadi String agar bisa disimpan di DB
    @TypeConverter
    fun dateToTimestamp(date: LocalDateTime?): String? {
        return date?.format(formatter)
    }

    // --- 🧬 FloatArray Converters (Untuk Biometric Embedding 128-d) ---
    // Sangat krusial agar data wajah AI bisa disimpan dalam satu kolom
    @TypeConverter
    fun fromFloatArray(value: FloatArray?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toFloatArray(value: String?): FloatArray? {
        val type = object : TypeToken<FloatArray>() {}.type
        return gson.fromJson(value, type)
    }

    // --- 📦 List<String> Converters (Untuk Assigned Classes / Hak Akses) ---
    // Digunakan pada UserEntity untuk menyimpan daftar kelas yang boleh diakses Admin
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type)
    }
}