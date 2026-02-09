package com.example.crashcourse.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "check_in_records")
data class CheckInRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0, // ID Lokal (Room)

    // 🚀 FIELD BARU (PENTING): Menyimpan ID Dokumen dari Firestore
    // Diisi setelah data sukses ter-upload ke Cloud.
    // Digunakan untuk Update/Delete data di Cloud nantinya.
    val firestoreId: String? = null,

    val studentId: String, // ✅ WAJIB: NIK/NIS Siswa
    val name: String,
    val timestamp: LocalDateTime,
    
    // Relasi & Metadata
    val faceId: Int? = null,
    val status: String = "PRESENT",
    
    // Foreign Keys untuk Filter & Grouping
    val classId: Int? = null,
    val subClassId: Int? = null,
    val gradeId: Int? = null,
    val subGradeId: Int? = null,
    val programId: Int? = null,
    val roleId: Int? = null,
    
    // Data String (Denormalized) untuk display cepat
    val note: String? = null,
    val className: String? = null,
    val gradeName: String? = null
)