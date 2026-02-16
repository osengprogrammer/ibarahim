package com.example.crashcourse.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 📝 CheckInRecordDao (V.20.3 - Synchronized)
 * Fix: Menyesuaikan nama fungsi dengan AttendanceRepository.
 */
@Dao
interface CheckInRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: CheckInRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<CheckInRecord>)

    // 🔥 FIX: Nama fungsi disesuaikan agar Repository tidak Error lagi
    @Query("SELECT * FROM attendance_records WHERE schoolId = :schoolId ORDER BY timestamp DESC")
    fun getRecordsBySchoolDirect(schoolId: String): Flow<List<CheckInRecord>>

    @Query("""
        SELECT * FROM attendance_records 
        WHERE studentId = :studentId AND className = :className 
        ORDER BY timestamp DESC LIMIT 1
    """)
    suspend fun getLastRecordForClass(studentId: String, className: String): CheckInRecord?

    @Query("UPDATE attendance_records SET status = :newStatus WHERE id = :id")
    suspend fun updateStatusById(id: Int, newStatus: String)

    @Delete
    suspend fun delete(record: CheckInRecord)

    @Query("DELETE FROM attendance_records")
    suspend fun deleteAll()
}