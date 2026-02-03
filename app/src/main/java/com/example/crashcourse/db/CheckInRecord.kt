package com.example.crashcourse.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "check_in_records")
data class CheckInRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val timestamp: LocalDateTime,
    val faceId: Int? = null,
    val status: String = "PRESENT",
    val classId: Int? = null,
    val subClassId: Int? = null,
    val gradeId: Int? = null,
    val subGradeId: Int? = null,
    val programId: Int? = null,
    val roleId: Int? = null,
    val note: String? = null,
    // ✅ Tambahkan kembali dua field ini agar Export & ViewModel tidak error
    val className: String? = null,
    val gradeName: String? = null
)