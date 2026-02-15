package com.example.crashcourse.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.LocalDateTime

/**
 * 🧪 Converters V.16.0 - Precision Edition
 * Mengganti JSON String dengan ByteArray (BLOB) untuk menjaga integritas vektor biometrik.
 */
class Converters {

    private val gson = Gson()

    // --- 📅 LOCAL DATETIME ---
    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(it) }
    }

    @TypeConverter
    fun fromLocalDateTime(date: LocalDateTime?): String? {
        return date?.toString()
    }

    // --- 🧬 FLOAT ARRAY (BIOMETRIC VECTOR) ---
    // ✅ Menggunakan ByteBuffer agar presisi desimal terjaga 100%
    @TypeConverter
    fun fromFloatArray(bytes: ByteArray?): FloatArray? {
        if (bytes == null) return null
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder())
        val array = FloatArray(bytes.size / 4) // 1 Float = 4 Bytes
        for (i in array.indices) {
            array[i] = buffer.float
        }
        return array
    }

    @TypeConverter
    fun toFloatArray(array: FloatArray?): ByteArray? {
        if (array == null) return null
        val buffer = ByteBuffer.allocate(array.size * 4).order(ByteOrder.nativeOrder())
        for (value in array) {
            buffer.putFloat(value)
        }
        return buffer.array()
    }

    // --- 📝 LIST STRING ---
    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        if (value == null) return emptyList()
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun toStringList(list: List<String>?): String {
        return gson.toJson(list ?: emptyList<String>())
    }
}