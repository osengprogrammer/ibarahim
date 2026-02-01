package com.example.crashcourse.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "faces")
data class FaceEntity(
    // We add a numeric ID to serve as 'faceId' in check-in records
    @PrimaryKey(autoGenerate = true) val id: Int = 0, 
    val studentId: String, 
    val name: String,
    val photoUrl: String? = null,
    val embedding: FloatArray,

    // 🆕 Numeric IDs (Required for your CheckInRecord logic)
    val classId: Int? = null,
    val subClassId: Int? = null,
    val gradeId: Int? = null,
    val subGradeId: Int? = null,
    val programId: Int? = null,
    val roleId: Int? = null,

    // Display Strings
    val className: String = "",
    val subClass: String = "",
    val grade: String = "",
    val subGrade: String = "",
    val program: String = "",
    val role: String = "",

    val timestamp: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FaceEntity) return false

        return id == other.id &&
                studentId == other.studentId &&
                name == other.name &&
                embedding.contentEquals(other.embedding) &&
                classId == other.classId &&
                roleId == other.roleId
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + studentId.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + embedding.contentHashCode()
        result = 31 * result + (classId ?: 0)
        result = 31 * result + (roleId ?: 0)
        return result
    }
}