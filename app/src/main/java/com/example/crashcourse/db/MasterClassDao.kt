package com.example.crashcourse.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.example.crashcourse.utils.Constants

/**
 * 🛰️ Azura Tech Master Class DAO (FIXED)
 * Menangani Unit Rakitan 6-Pilar dengan Join Table Otomatis.
 */
@Dao
interface MasterClassDao {
    
    /**
     * ✅ FIX UTAMA:
     * Mengembalikan 'Long' (row ID) agar ViewModel bisa mengambil ID yang baru digenerate.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClass(masterClass: MasterClassRoom): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(masterClasses: List<MasterClassRoom>)

    @Delete
    suspend fun deleteClass(masterClass: MasterClassRoom)
    
    // Membersihkan semua data master class (misal saat logout atau full sync)
    @Query("DELETE FROM ${Constants.COLL_MASTER_CLASSES}")
    suspend fun deleteAll()

    /**
     * 🚀 THE HEART OF THE 6-PILAR SYSTEM
     * Menggabungkan ID dari MasterClassRoom dengan Nama Asli dari tabel Options.
     */
    @Query("""
        SELECT 
            m.classId, 
            m.className, 
            m.${Constants.KEY_SEKOLAH_ID},
            g.${Constants.KEY_NAME} AS grade_name,
            c.${Constants.KEY_NAME} AS class_opt_name,
            p.${Constants.KEY_NAME} AS program_name,
            sc.${Constants.KEY_NAME} AS sub_class_name,
            sg.${Constants.KEY_NAME} AS sub_grade_name,
            r.${Constants.KEY_NAME} AS role_name
        FROM ${Constants.COLL_MASTER_CLASSES} m
        LEFT JOIN ${Constants.COLL_OPT_GRADES} g ON m.gradeId = g.${Constants.KEY_ID}
        LEFT JOIN ${Constants.COLL_OPT_CLASSES} c ON m.classOptionId = c.${Constants.KEY_ID}
        LEFT JOIN ${Constants.COLL_OPT_PROGRAMS} p ON m.programId = p.${Constants.KEY_ID}
        LEFT JOIN ${Constants.COLL_OPT_SUBCLASSES} sc ON m.subClassId = sc.${Constants.KEY_ID}
        LEFT JOIN ${Constants.COLL_OPT_SUBGRADES} sg ON m.subGradeId = sg.${Constants.KEY_ID}
        LEFT JOIN ${Constants.COLL_OPT_ROLES} r ON m.roleId = r.${Constants.KEY_ID}
        WHERE m.${Constants.KEY_SEKOLAH_ID} = :sekolahId
    """)
    fun getAllMasterClassesWithNamesFlow(sekolahId: String): Flow<List<MasterClassWithNames>>
}