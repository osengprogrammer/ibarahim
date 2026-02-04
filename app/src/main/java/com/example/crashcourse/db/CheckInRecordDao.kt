package com.example.crashcourse.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface CheckInRecordDao {

    @Query("SELECT * FROM check_in_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<CheckInRecord>>

    @Query("""
        SELECT * FROM check_in_records 
        WHERE 
            (name LIKE '%' || :nameFilter || '%' OR :nameFilter = '')
            AND (timestamp >= :startDate OR :startDate IS NULL)
            AND (timestamp <= :endDate OR :endDate IS NULL)
            -- Filter ID (Untuk akurasi data master)
            AND (classId = :classId OR :classId IS NULL)
            -- 🔥 TAMBAHAN: Filter Nama (Penyelamat jika ID kosong)
            AND (className = :className OR :className IS NULL OR :className = '')
            
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
        className: String?, // ✅ Tambahkan parameter ini
        subClassId: Int?,
        gradeId: Int?,
        subGradeId: Int?,
        programId: Int?,
        roleId: Int?
    ): Flow<List<CheckInRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: CheckInRecord)

    @Update
    suspend fun update(record: CheckInRecord)

    @Delete
    suspend fun delete(record: CheckInRecord)
}