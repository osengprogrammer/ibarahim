package com.example.crashcourse.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * 📝 Azura Tech Check-In Record DAO (V.7.0 - Build Success Version)
 * Mengelola akses data absensi dengan dukungan filter rentang waktu dan multi-rombel.
 */
@Dao
interface CheckInRecordDao {

    // --- 📥 INSERT OPERATIONS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: CheckInRecord): Long 

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<CheckInRecord>)

    // --- 🔍 QUERY OPERATIONS (Flow for UI) ---
    @Query("SELECT * FROM attendance_records ORDER BY timestamp DESC")
    fun getAllRecordsFlow(): Flow<List<CheckInRecord>>

    @Query("SELECT * FROM attendance_records WHERE className = :className ORDER BY timestamp DESC")
    fun getRecordsByClass(className: String): Flow<List<CheckInRecord>>

    // --- 📅 FILTER & HISTORY ---
    // Fungsi ini wajib untuk Smart Sync di Repository
    @Query("SELECT * FROM attendance_records WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    suspend fun getRecordsBetween(start: LocalDateTime, end: LocalDateTime): List<CheckInRecord>

    // ⏱️ Anti-Spam: Digunakan oleh RecognitionViewModel & AttendanceRepository
    @Query("SELECT * FROM attendance_records WHERE studentId = :studentId AND className = :className ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastRecordForClass(studentId: String, className: String): CheckInRecord?

    // --- 🔄 SYNC OPERATIONS ---
    @Query("SELECT * FROM attendance_records WHERE syncStatus = 'PENDING'")
    suspend fun getPendingRecords(): List<CheckInRecord>

    @Query("UPDATE attendance_records SET syncStatus = 'SYNCED', firestoreId = :firestoreId WHERE id = :id")
    suspend fun markAsSynced(id: Int, firestoreId: String)

    // --- ✍️ UPDATE & DELETE ---
    @Query("UPDATE attendance_records SET status = :newStatus WHERE id = :id")
    suspend fun updateStatusById(id: Int, newStatus: String)

    @Update
    suspend fun update(record: CheckInRecord)

    @Delete
    suspend fun delete(record: CheckInRecord)

    @Query("DELETE FROM attendance_records")
    suspend fun deleteAll()
}