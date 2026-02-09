package com.example.crashcourse.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "faces")
data class FaceEntity(
    // 🚀 IDENTITAS UNIK SEBAGAI PRIMARY KEY
    // Menjadikan studentId sebagai PK memastikan OnConflictStrategy.REPLACE bekerja 
    // dengan cara menimpa data lama jika ID yang sama masuk dari Cloud.
    @PrimaryKey 
    val studentId: String, 

    // 🚀 Identitas Cloud (Penting untuk CRUD Firestore)
    val firestoreId: String? = null,

    val name: String,
    val photoUrl: String? = null,
    val embedding: FloatArray,

    // 🆕 Relasi ID (Foreign Keys semu untuk mapping ke tabel Master Data)
    val classId: Int? = null,
    val subClassId: Int? = null,
    val gradeId: Int? = null,
    val subGradeId: Int? = null,
    val programId: Int? = null,
    val roleId: Int? = null,

    // 📄 Display Strings (Denormalisasi agar UI kencang tanpa Join)
    val className: String = "",
    val subClass: String = "",
    val grade: String = "",
    val subGrade: String = "",
    val program: String = "",
    val role: String = "",

    val timestamp: Long = System.currentTimeMillis()
) {
    // 🛠 Perbaikan Equals: Memastikan UI terupdate saat ada perubahan field sekecil apapun
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FaceEntity) return false

        if (studentId != other.studentId) return false
        if (firestoreId != other.firestoreId) return false
        if (name != other.name) return false
        if (photoUrl != other.photoUrl) return false
        if (!embedding.contentEquals(other.embedding)) return false
        
        // Cek ID Relasi
        if (classId != other.classId) return false
        if (subClassId != other.subClassId) return false
        if (gradeId != other.gradeId) return false
        if (subGradeId != other.subGradeId) return false
        if (programId != other.programId) return false
        if (roleId != other.roleId) return false
        
        // Cek Display Strings
        if (className != other.className) return false
        if (subClass != other.subClass) return false
        if (grade != other.grade) return false
        if (subGrade != other.subGrade) return false
        if (program != other.program) return false
        if (role != other.role) return false

        return timestamp == other.timestamp
    }

    // 🛠 Perbaikan HashCode: Menghindari bug pada koleksi List/Set/Map
    override fun hashCode(): Int {
        var result = studentId.hashCode()
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