package com.example.crashcourse.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 🏛️ Entity untuk pilihan Kelas (Contoh: IPA 1, IPS 2)
 */
@Entity(tableName = "class_options")
data class ClassOption(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // ✅ ID Otomatis
    val name: String,
    val displayOrder: Int = 0
)

/**
 * 🏛️ Entity untuk Sub-Kelas (Contoh: Kelompok A, Kelompok B)
 * Memiliki relasi ke ClassOption via parentClassId
 */
@Entity(tableName = "subclass_options")
data class SubClassOption(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // ✅ ID Otomatis
    val name: String,
    val parentClassId: Int, // Referensi ke ClassOption.id
    val displayOrder: Int = 0
)

/**
 * 🏛️ Entity untuk Jenjang/Grade (Contoh: Grade 10, Grade 11)
 */
@Entity(tableName = "grade_options")
data class GradeOption(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // ✅ ID Otomatis
    val name: String,
    val displayOrder: Int = 0
)

/**
 * 🏛️ Entity untuk Sub-Jenjang (Contoh: Level Foundation, Level Advanced)
 * Memiliki relasi ke GradeOption via parentGradeId
 */
@Entity(tableName = "subgrade_options")
data class SubGradeOption(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // ✅ ID Otomatis
    val name: String,
    val parentGradeId: Int, // Referensi ke GradeOption.id
    val displayOrder: Int = 0
)

/**
 * 🏛️ Entity untuk Pilihan Program (Contoh: Reguler, Boarding, Tahfidz)
 */
@Entity(tableName = "program_options")
data class ProgramOption(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // ✅ ID Otomatis
    val name: String,
    val displayOrder: Int = 0
)

/**
 * 🏛️ Entity untuk Peran/Role (Contoh: Pengajar, Murid, Admin)
 */
@Entity(tableName = "role_options")
data class RoleOption(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // ✅ ID Otomatis
    val name: String,
    val displayOrder: Int = 0
)