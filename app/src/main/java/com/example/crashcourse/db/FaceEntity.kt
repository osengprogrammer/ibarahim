package com.example.crashcourse.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(
    tableName = "students",
    indices = [
        // ⚡ Index untuk filter "Global Load" (Wajib ada!)
        Index(value = ["schoolId"]),
        
        // 🔍 Index nama buat fitur pencarian manual (Search Bar)
        Index(value = ["name"]) 
        
        // ❌ HAPUS Index studentId (Karena PrimaryKey sudah otomatis Index)
    ]
)
// TypeConverters dipanggil di sini agar FloatArray & List<String> bisa disimpan
@TypeConverters(Converters::class)
data class FaceEntity(
    
    /**
     * 🆔 Primary Key
     * Gunakan NISN atau ID unik dari server.
     */
    @PrimaryKey 
    @ColumnInfo(name = "studentId")
    val studentId: String, 

    /**
     * 🏫 Tenant ID
     * Kunci utama untuk pemisahan data antar sekolah/kantor.
     * (Renamed from sekolahId for consistency)
     */
    @ColumnInfo(name = "schoolId")
    val schoolId: String,
    
    /**
     * 🧠 AI Biometric Vector (192 floats)
     * Converter akan mengubah ini menjadi BLOB (ByteArray) agar hemat memori & cepat.
     */
    @ColumnInfo(name = "embedding")
    val embedding: FloatArray, 

    /**
     * 📚 Enrolled Classes / Divisions
     * Menggunakan List<String> agar mudah dicek di Logic Kotlin.
     * Contoh: ["X-RPL-1", "Math-Club", "Basket"]
     * Disimpan di DB sebagai JSON: "[\"X-RPL-1\", \"Math-Club\"]"
     */
    @ColumnInfo(name = "enrolledClasses")
    val enrolledClasses: List<String> = emptyList(), 
    
    val name: String,
    
    val photoUrl: String? = null,
    
    // Metadata Tambahan (Opsional)
    val subClass: String = "", // Misal: "Shift Pagi"
    val grade: String = "",    // Misal: "Staff" atau "12"
    val timestamp: Long = System.currentTimeMillis()
) {
    // 🛡️ WAJIB: Override equals untuk membandingkan isi Array
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FaceEntity

        if (studentId != other.studentId) return false
        // Konten vector harus sama persis
        if (!embedding.contentEquals(other.embedding)) return false 

        return true
    }

    // 🛡️ WAJIB: Override hashCode agar entity ini bisa masuk ke Set/Map dengan benar
    override fun hashCode(): Int {
        var result = studentId.hashCode()
        result = 31 * result + embedding.contentHashCode()
        return result
    }
}