package com.example.crashcourse.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * 📊 CheckInRecord (V.20.5 - Aligned)
 */
@Entity(
    tableName = "attendance_records",
    indices = [
        Index(value = ["studentId"]), 
        Index(value = ["schoolId"]), 
        Index(value = ["syncStatus"])
    ]
)
data class CheckInRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val studentId: String,
    val name: String,
    val schoolId: String, // ✅ Pastikan namanya 'schoolId'
    val timestamp: LocalDateTime,
    val status: String, 
    val className: String,
    val gradeName: String? = null,
    val role: String? = null,
    val firestoreId: String? = null,
    val faceId: Int? = null,
    val verified: Boolean = true,
    val syncStatus: String = "PENDING", 
    val photoPath: String? = ""
)