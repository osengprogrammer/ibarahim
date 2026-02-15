package com.example.crashcourse.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.example.crashcourse.utils.Constants

/**
 * 🛰️ Azura Tech Master Class DAO (V.10.17 - Refactored)
 * Menangani Unit Rakitan 6-Pilar dengan Join Table Otomatis.
 */
@Dao
interface MasterClassDao {
    
    /**
     * Menyimpan satu Unit/Rombel. 
     * Mengembalikan 'Long' (row ID) untuk validasi di ViewModel.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClass(masterClass: MasterClassRoom): Long

    /**
     * Menyimpan banyak Unit sekaligus (Batch Insert saat Sync).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(masterClasses: List<MasterClassRoom>)

    @Delete
    suspend fun deleteClass(masterClass: MasterClassRoom)
    
    /**
     * Membersihkan semua data master class (saat logout).
     */
    @Query("DELETE FROM master_classes")
    suspend fun deleteAll()

    /**
     * 🚀 THE HEART OF THE 6-PILAR SYSTEM
     * Menggabungkan ID dari MasterClassRoom dengan Nama Asli dari tabel Options.
     * 🔥 FIXED: Query disesuaikan menggunakan 'schoolId'.
     */
    @Query("""
        SELECT 
            m.classId, 
            m.className, 
            m.schoolId,
            g.${Constants.KEY_NAME} AS grade_name,
            c.${Constants.KEY_NAME} AS class_opt_name,
            p.${Constants.KEY_NAME} AS program_name,
            sc.${Constants.KEY_NAME} AS sub_class_name,
            sg.${Constants.KEY_NAME} AS sub_grade_name,
            r.${Constants.KEY_NAME} AS role_name
        FROM master_classes m
        LEFT JOIN ${Constants.COLL_OPT_GRADES} g ON m.gradeId = g.${Constants.KEY_ID}
        LEFT JOIN ${Constants.COLL_OPT_CLASSES} c ON m.classOptionId = c.${Constants.KEY_ID}
        LEFT JOIN ${Constants.COLL_OPT_PROGRAMS} p ON m.programId = p.${Constants.KEY_ID}
        LEFT JOIN ${Constants.COLL_OPT_SUBCLASSES} sc ON m.subClassId = sc.${Constants.KEY_ID}
        LEFT JOIN ${Constants.COLL_OPT_SUBGRADES} sg ON m.subGradeId = sg.${Constants.KEY_ID}
        LEFT JOIN ${Constants.COLL_OPT_ROLES} r ON m.roleId = r.${Constants.KEY_ID}
        WHERE m.schoolId = :schoolId
    """)
    fun getAllMasterClassesWithNamesFlow(schoolId: String): Flow<List<MasterClassWithNames>>
}