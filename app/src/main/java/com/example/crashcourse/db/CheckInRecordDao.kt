package com.example.crashcourse.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface CheckInRecordDao {

    // --- 📝 CRUD STANDAR ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: CheckInRecord): Long 

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<CheckInRecord>)

    @Update
    suspend fun update(record: CheckInRecord)

    @Delete
    suspend fun delete(record: CheckInRecord)

    // --- 🛡️ VALIDASI & LOGIC (Anti-Duplicate) ---

    @Query("SELECT COUNT(*) FROM check_in_records WHERE studentId = :studentId AND timestamp = :timestamp")
    suspend fun checkIfExist(studentId: String, timestamp: LocalDateTime): Int

    @Query("SELECT MAX(timestamp) FROM check_in_records WHERE studentId = :studentId")
    suspend fun getLastTimestampByStudentId(studentId: String): LocalDateTime?

    // --- 🚀 BATCH OPTIMIZATION (REQUIRED FOR VIEWMODEL) ---
    // Fungsi ini WAJIB ADA untuk ViewModel 'fetchHistoricalData' dan 'startSmartSync'
    // Gunanya untuk mengambil data dalam rentang waktu sekaligus (Batch)
    @Query("SELECT * FROM check_in_records WHERE timestamp BETWEEN :start AND :end")
    suspend fun getRecordsBetween(start: LocalDateTime, end: LocalDateTime): List<CheckInRecord>

    // --- 🔍 QUERY UTAMA ---

    @Query("SELECT * FROM check_in_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<CheckInRecord>>

    @Query("SELECT * FROM check_in_records WHERE status = :status")
    fun getRecordsByStatus(status: String): Flow<List<CheckInRecord>>

    // --- ⚙️ UTILITAS ---

    @Query("DELETE FROM check_in_records")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM check_in_records")
    suspend fun getCount(): Int
}