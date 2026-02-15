package com.example.crashcourse.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 🏗️ MasterClassRoom (V.10.15 - Schema Refactored)
 * Tabel utama untuk menyimpan konfigurasi Unit/Rombel.
 */
@Entity(tableName = "master_classes")
data class MasterClassRoom(
    @PrimaryKey
    val classId: Int, // Diambil dari Firestore ID
    
    val schoolId: String, // ✅ FIXED: Ganti dari sekolahId
    val className: String, // Nama Display, misal: "XII RPL 1"

    // Foreign Keys (Menunjuk ke ID di tabel Options)
    val gradeId: Int,
    val classOptionId: Int, 
    val programId: Int,
    val subClassId: Int,
    val subGradeId: Int,
    val roleId: Int
)

/**
 * 📋 MasterClassWithNames
 * Data class perantara (POJO) untuk menampung hasil JOIN antara 
 * tabel MasterClassRoom dengan tabel-tabel Options.
 */
data class MasterClassWithNames(
    val classId: Int,
    val schoolId: String, // ✅ FIXED: Ganti dari sekolahId
    val className: String,
    
    // 🚀 MAPPING SQL: Harus sama persis dengan alias "AS" di MasterClassDao
    @ColumnInfo(name = "grade_name") val gradeName: String?,
    @ColumnInfo(name = "class_opt_name") val classOptionName: String?,
    @ColumnInfo(name = "program_name") val programName: String?,
    @ColumnInfo(name = "sub_class_name") val subClassName: String?,
    @ColumnInfo(name = "sub_grade_name") val subGradeName: String?,
    @ColumnInfo(name = "role_name") val roleName: String?
)