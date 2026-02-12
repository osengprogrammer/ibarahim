package com.example.crashcourse.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * 📝 Azura Tech Check-In Record DAO
 * Mengelola akses data absensi dengan dukungan filter rentang waktu dan multi-rombel.
 */
@Dao
interface CheckInRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: CheckInRecord): Long 

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<CheckInRecord>)

    /**
     * 📱 Menampilkan semua riwayat absen di UI secara real-time.
     */
    @Query("SELECT * FROM attendance_records ORDER BY timestamp DESC")
    fun getAllRecordsFlow(): Flow<List<CheckInRecord>>

    /**
     * 🔍 Filter berdasarkan Mata Kuliah/Rombel yang spesifik.
     */
    @Query("SELECT * FROM attendance_records WHERE className = :className ORDER BY timestamp DESC")
    fun getRecordsByClass(className: String): Flow<List<CheckInRecord>>

    /**
     * 📅 FILTER RENTANG WAKTU (Penting untuk Laporan)
     * Mengambil data di antara dua waktu (misal: Awal Hari ini dan Akhir Hari ini).
     */
    @Query("SELECT * FROM attendance_records WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    suspend fun getRecordsBetween(start: LocalDateTime, end: LocalDateTime): List<CheckInRecord>

    /**
     * ⏱️ Anti-Spam: Mencari record terakhir mahasiswa di matkul tertentu.
     */
    @Query("SELECT * FROM attendance_records WHERE studentId = :studentId AND className = :className ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastRecordForClass(studentId: String, className: String): CheckInRecord?

    /**
     * 🔄 Sinkronisasi: Mencari data yang belum ter-upload ke Cloud.
     */
    @Query("SELECT * FROM attendance_records WHERE syncStatus = 'PENDING'")
    suspend fun getPendingRecords(): List<CheckInRecord>

    /**
     * ✅ Update status setelah berhasil sinkronisasi ke Firestore.
     */
    @Query("UPDATE attendance_records SET syncStatus = 'SYNCED', firestoreId = :firestoreId WHERE id = :id")
    suspend fun markAsSynced(id: Int, firestoreId: String)

    /**
     * Mengambil timestamp terakhir siswa (Global).
     */
    @Query("SELECT MAX(timestamp) FROM attendance_records WHERE studentId = :studentId")
    suspend fun getLastTimestampByStudentId(studentId: String): LocalDateTime?

    /**
     * Mengubah status kehadiran secara manual oleh Guru (Hadir/Sakit/Izin).
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