package com.example.crashcourse.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(
    tableName = "attendance_records", // 🚀 SINKRON: Gunakan nama ini
    indices = [Index(value = ["studentId"]), Index(value = ["syncStatus"])]
)
data class CheckInRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val studentId: String,
    val name: String,
    val timestamp: LocalDateTime,
    val status: String, // PRESENT, LATE, ABSENT
    val className: String, // Konteks Matkul saat scan (Socio/Translation)
    val gradeName: String? = null,
    val role: String? = null,
    val firestoreId: String? = null,
    val faceId: Int? = null,
    val verified: Boolean = true,
    val syncStatus: String = "PENDING", 
    val photoPath: String? = ""
)