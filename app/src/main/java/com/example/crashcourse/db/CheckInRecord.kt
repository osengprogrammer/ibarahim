package com.example.crashcourse.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.crashcourse.utils.Constants
import java.time.LocalDateTime

@Entity(tableName = Constants.COLL_ATTENDANCE) // 🚀 Must use the Constant
data class CheckInRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val studentId: String,
    val name: String,
    val timestamp: LocalDateTime,
    val status: String,
    val className: String?,
    val gradeName: String?,
    val role: String?,
    val firestoreId: String? = null,
    val faceId: Int? = null,
    val verified: Boolean = false,
    val syncStatus: String = "PENDING",
    val photoPath: String? = ""
)