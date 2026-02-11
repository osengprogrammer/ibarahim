package com.example.crashcourse.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface CheckInRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: CheckInRecord): Long 

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<CheckInRecord>)

    /**
     * Mengambil semua log absensi.
     */
    @Query("SELECT * FROM attendance_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<CheckInRecord>>

    /**
     * Digunakan untuk validasi apakah siswa sudah absen di waktu yang sama.
     */
    @Query("SELECT COUNT(*) FROM attendance_records WHERE studentId = :studentId AND timestamp = :timestamp")
    suspend fun checkIfExist(studentId: String, timestamp: LocalDateTime): Int

    /**
     * Mencari data dalam rentang waktu tertentu (Filter harian/mingguan).
     */
    @Query("SELECT * FROM attendance_records WHERE timestamp BETWEEN :start AND :end")
    suspend fun getRecordsBetween(start: LocalDateTime, end: LocalDateTime): List<CheckInRecord>

    /**
     * Mengambil waktu absen terakhir siswa.
     */
    @Query("SELECT MAX(timestamp) FROM attendance_records WHERE studentId = :studentId")
    suspend fun getLastTimestampByStudentId(studentId: String): LocalDateTime?

    /**
     * ✅ FIX: Menambahkan fungsi updateStatus yang dipanggil oleh ViewModel.
     * Digunakan untuk mengubah status (Hadir/Sakit/Izin) secara spesifik.
     */
    @Query("UPDATE attendance_records SET status = :newStatus WHERE studentId = :studentId AND timestamp = :timestamp")
    suspend fun updateStatus(studentId: String, timestamp: LocalDateTime, newStatus: String)

    @Query("DELETE FROM attendance_records")
    suspend fun deleteAll()

    @Update
    suspend fun update(record: CheckInRecord)

    @Delete
    suspend fun delete(record: CheckInRecord)
}