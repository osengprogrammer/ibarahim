package com.example.crashcourse.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.crashcourse.utils.Constants

/**
 * 👤 Azura Tech Face Entity
 * * Arsitektur ini mendukung skenario Many-to-Many:
 * Kolom [className] akan menyimpan string CSV (contoh: "Socio Linguistic, Translation") 
 * yang diproses oleh ViewModel sebelum disimpan ke Room.
 */
@Entity(
    tableName = "students",
    indices = [
        Index(value = ["sekolahId"]),
        Index(value = ["className"]),
        Index(value = ["studentId"], unique = true)
    ]
)
data class FaceEntity(
    @PrimaryKey 
    @ColumnInfo(name = "studentId")
    val studentId: String, 

    @ColumnInfo(name = "sekolahId")
    val sekolahId: String,
    
    val firestoreId: String? = null,

    val name: String,
    val photoUrl: String? = null,
    
    /**
     * 🧠 AI Embedding
     * Memerlukan Converters.kt untuk mengubah FloatArray menjadi format yang didukung Room.
     */
    val embedding: FloatArray, 

    // --- Master Data Mapping (ID) ---
    val classId: Int? = null,
    val subClassId: Int? = null,
    val gradeId: Int? = null,
    val subGradeId: Int? = null,
    val programId: Int? = null,
    val roleId: Int? = null,

    // --- UI Denormalized Fields (Names) ---
    /**
     * Menyimpan gabungan nama Rombel/Mata Kuliah. 
     * Contoh: "Socio Linguistic, Translation"
     */
    val className: String = "",
    val subClass: String = "",
    val grade: String = "",
    val subGrade: String = "",
    val program: String = "",
    val role: String = "",

    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * ⚠️ Penting: Override equals dan hashCode karena menggunakan FloatArray.
     * Tanpa ini, perbandingan objek biometrik tidak akan akurat.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FaceEntity) return false

        if (studentId != other.studentId) return false
        if (sekolahId != other.sekolahId) return false
        if (firestoreId != other.firestoreId) return false
        if (name != other.name) return false
        if (photoUrl != other.photoUrl) return false
        if (!embedding.contentEquals(other.embedding)) return false
        if (classId != other.classId) return false
        if (subClassId != other.subClassId) return false
        if (gradeId != other.gradeId) return false
        if (subGradeId != other.subGradeId) return false
        if (programId != other.programId) return false
        if (roleId != other.roleId) return false
        if (className != other.className) return false
        if (subClass != other.subClass) return false
        if (grade != other.grade) return false
        if (subGrade != other.subGrade) return false
        if (program != other.program) return false
        if (role != other.role) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = studentId.hashCode()
        result = 31 * result + sekolahId.hashCode()
        result = 31 * result + (firestoreId?.hashCode() ?: 0)
        result = 31 * result + name.hashCode()
        result = 31 * result + (photoUrl?.hashCode() ?: 0)
        result = 31 * result + embedding.contentHashCode()
        result = 31 * result + (classId ?: 0)
        result = 31 * result + (subClassId ?: 0)
        result = 31 * result + (gradeId ?: 0)
        result = 31 * result + (subGradeId ?: 0)
        result = 31 * result + (programId ?: 0)
        result = 31 * result + (roleId ?: 0)
        result = 31 * result + className.hashCode()
        result = 31 * result + subClass.hashCode()
        result = 31 * result + grade.hashCode()
        result = 31 * result + subGrade.hashCode()
        result = 31 * result + program.hashCode()
        result = 31 * result + role.hashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}