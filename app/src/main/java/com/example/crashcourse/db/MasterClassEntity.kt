package com.example.crashcourse.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// ==========================================
// 1. TABEL UTAMA (Menyimpan ID)
// ==========================================
@Entity(tableName = "master_classes")
data class MasterClassRoom(
    @PrimaryKey(autoGenerate = true)
    val classId: Int = 0,
    
    val sekolahId: String,
    val className: String, // Nama Display, misal: "XII RPL 1"

    // Foreign Keys (Menunjuk ke ID di tabel Options)
    val gradeId: Int,
    val classOptionId: Int, // Departemen / Class
    val programId: Int,
    val subClassId: Int,
    val subGradeId: Int,
    val roleId: Int
)

// ==========================================
// 2. HELPER CLASS (Menyimpan Nama Asli)
// ==========================================
// ⚠️ Class ini WAJIB ada di package 'db' dan pakai @ColumnInfo
// agar sinkron dengan Query "AS grade_name" di MasterClassDao.
data class MasterClassWithNames(
    val classId: Int,
    val sekolahId: String,
    val className: String,
    
    // 🚀 MAPPING SQL (Wajib sama dengan Query di DAO)
    @ColumnInfo(name = "grade_name") val gradeName: String?,
    @ColumnInfo(name = "class_opt_name") val classOptionName: String?,
    @ColumnInfo(name = "program_name") val programName: String?,
    @ColumnInfo(name = "sub_class_name") val subClassName: String?,
    @ColumnInfo(name = "sub_grade_name") val subGradeName: String?,
    @ColumnInfo(name = "role_name") val roleName: String?
)