package com.example.crashcourse.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.crashcourse.utils.Constants

// 1. KELAS (Departemen)
@Entity(tableName = Constants.COLL_OPT_CLASSES)
data class ClassOption(
    @PrimaryKey val id: Int,
    val name: String,
    val displayOrder: Int = 0  // ✅ WAS 'order' → matches UI expectation
)

// 2. SUB-KELAS (Unit Detail) - Firestore uses parentClassId
@Entity(tableName = Constants.COLL_OPT_SUBCLASSES)
data class SubClassOption(
    @PrimaryKey val id: Int,
    val name: String,
    val parentClassId: Int = 0,  // ✅ WAS 'parentId' → matches UI expectation
    val displayOrder: Int = 0    // ✅ WAS 'order'
)

// 3. JENJANG (Grade)
@Entity(tableName = Constants.COLL_OPT_GRADES)
data class GradeOption(
    @PrimaryKey val id: Int,
    val name: String,
    val displayOrder: Int = 0  // ✅ WAS 'order'
)

// 4. SUB-JENJANG (Periode/Shift) - Firestore uses parentGradeId
@Entity(tableName = Constants.COLL_OPT_SUBGRADES)
data class SubGradeOption(
    @PrimaryKey val id: Int,
    val name: String,
    val parentGradeId: Int = 0,  // ✅ WAS 'parentId' → matches UI expectation
    val displayOrder: Int = 0    // ✅ WAS 'order'
)

// 5. PROGRAM (Jurusan)
@Entity(tableName = Constants.COLL_OPT_PROGRAMS)
data class ProgramOption(
    @PrimaryKey val id: Int,
    val name: String,
    val displayOrder: Int = 0  // ✅ WAS 'order'
)

// 6. PERAN (Jabatan)
@Entity(tableName = Constants.COLL_OPT_ROLES)
data class RoleOption(
    @PrimaryKey val id: Int,
    val name: String,
    val displayOrder: Int = 0  // ✅ WAS 'order'
)