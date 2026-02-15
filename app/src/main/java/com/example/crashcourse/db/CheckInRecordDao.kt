package com.example.crashcourse.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * 📝 CheckInRecordDao (V.10.19 - Repository Aligned)
 * Mengelola data pada tabel 'attendance_records'.
 * Seluruh nama fungsi di sini disesuaikan dengan AttendanceRepository V.10.18.
 */
@Dao
interface CheckInRecordDao {

    // ==========================================
    // 📥 INSERT OPERATIONS
    // ==========================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: CheckInRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<CheckInRecord>)

    // ==========================================
    // 🔍 READ OPERATIONS (Filtered by Context)
    // ==========================================

    /**
     * Mengambil data absensi berdasarkan Sekolah yang aktif.
     * Menggunakan subquery ke tabel 'students' (FaceEntity) untuk validasi schoolId.
     */
    @Query("""
        SELECT * FROM attendance_records 
        WHERE studentId IN (SELECT studentId FROM students WHERE schoolId = :schoolId) 
        ORDER BY timestamp DESC
    """)
    fun getAllRecordsBySchoolFlow(schoolId: String): Flow<List<CheckInRecord>>

    /**
     * Mengambil log terakhir siswa di kelas tertentu (Anti-Spam/Cooldown Logic).
     */
    @Query("""
        SELECT * FROM attendance_records 
        WHERE studentId = :studentId AND className = :className 
        ORDER BY timestamp DESC LIMIT 1
    """)
    suspend fun getLastRecordForClass(studentId: String, className: String): CheckInRecord?

    // ==========================================
    // 🔄 SYNC & MAINTENANCE (Update/Delete)
    // ==========================================

    /**
     * Menandai data sukses terupload ke Firestore.
     */
    @Query("UPDATE attendance_records SET syncStatus = 'SYNCED', firestoreId = :firestoreId WHERE id = :id")
    suspend fun markAsSynced(id: Int, firestoreId: String)

    /**
     * 🔥 FIXED: Nama fungsi harus 'updateStatusById' sesuai panggilan di Repository.
     */
    @Query("UPDATE attendance_records SET status = :newStatus WHERE id = :id")
    suspend fun updateStatusById(id: Int, newStatus: String)

    /**
     * 🔥 FIXED: Nama fungsi harus 'delete' sesuai standar @Delete atau panggilan Repository.
     */
    @Delete
    suspend fun delete(record: CheckInRecord)

    @Query("DELETE FROM attendance_records")
    suspend fun deleteAll()
}