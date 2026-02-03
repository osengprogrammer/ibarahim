package com.example.crashcourse.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface CheckInRecordDao {

    // --- 1. AMBIL SEMUA DATA (Sederhana untuk Debug) ---
    @Query("SELECT * FROM check_in_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<CheckInRecord>>

    // --- 2. QUERY FILTER SAKTI (Optimized for Null Safety) ---
    /**
     * Zohar menggunakan logika (column = :param OR :param IS NULL) 
     * karena ini jauh lebih stabil di SQLite saat menangani TypeConverters Long.
     */
    @Query("""
        SELECT * FROM check_in_records 
        WHERE 
            (name LIKE '%' || :nameFilter || '%' OR :nameFilter = '')
            AND (timestamp >= :startDate OR :startDate IS NULL)
            AND (timestamp <= :endDate OR :endDate IS NULL)
            AND (classId = :classId OR :classId IS NULL)
            AND (subClassId = :subClassId OR :subClassId IS NULL)
            AND (gradeId = :gradeId OR :gradeId IS NULL)
            AND (subGradeId = :subGradeId OR :subGradeId IS NULL)
            AND (programId = :programId OR :programId IS NULL)
            AND (roleId = :roleId OR :roleId IS NULL)
        ORDER BY timestamp DESC
    """)
    fun getFilteredRecords(
        nameFilter: String,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        classId: Int?,
        subClassId: Int?,
        gradeId: Int?,
        subGradeId: Int?,
        programId: Int?,
        roleId: Int?
    ): Flow<List<CheckInRecord>>

    // --- 3. OPERASI CRUD ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: CheckInRecord)

    @Update
    suspend fun update(record: CheckInRecord)

    @Delete
    suspend fun delete(record: CheckInRecord)

    // Helper untuk membersihkan riwayat jika diperlukan
    @Query("DELETE FROM check_in_records")
    suspend fun deleteAllRecords()
}