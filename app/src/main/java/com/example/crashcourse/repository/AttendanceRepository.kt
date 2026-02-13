package com.example.crashcourse.repository

import android.app.Application
import com.example.crashcourse.db.AppDatabase
import com.example.crashcourse.db.CheckInRecord
import com.example.crashcourse.firestore.FirestoreAttendance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 📂 AttendanceRepository
 * Pusat kendali transaksi absensi, sinkronisasi cloud, dan manajemen history.
 */
class AttendanceRepository(application: Application) {
    private val db = AppDatabase.getInstance(application)
    private val checkInDao = db.checkInRecordDao()

    companion object {
        private const val CHECK_IN_COOLDOWN_SEC = 30L
    }

    // 1. Flow Data Absensi (Live Data untuk UI History/Laporan)
    fun getAllRecordsFlow(): Flow<List<CheckInRecord>> = checkInDao.getAllRecordsFlow()

    // 🔥 2. BRIDGE UNTUK RECOGNITION VIEWMODEL (SOLUSI ERROR BUILD)
    // Fungsi ini wajib ada agar RecognitionViewModel bisa mengecek cooldown sebelum simpan
    suspend fun getLastRecordForClass(studentId: String, className: String): CheckInRecord? = withContext(Dispatchers.IO) {
        checkInDao.getLastRecordForClass(studentId, className)
    }

    // 3. Logika Simpan Absensi (Scanner AI & Input Manual)
    suspend fun saveAttendance(record: CheckInRecord, sekolahId: String): String? = withContext(Dispatchers.IO) {
        try {
            // Cek Cooldown (Double Check di level Repo)
            val lastRecord = checkInDao.getLastRecordForClass(record.studentId, record.className)
            if (lastRecord != null && lastRecord.timestamp.isAfter(LocalDateTime.now().minusSeconds(CHECK_IN_COOLDOWN_SEC))) {
                return@withContext "COOLDOWN"
            }

            // Simpan Lokal
            val rowId = checkInDao.insert(record)
            val saved = record.copy(id = rowId.toInt())

            // Simpan ke Cloud Firestore
            FirestoreAttendance.saveCheckIn(saved, sekolahId)
            return@withContext "SUCCESS"
        } catch (e: Exception) {
            return@withContext e.message
        }
    }

    // 4. 🛡️ SMART SYNC (Cloud -> Lokal)
    suspend fun syncAttendance(cloudRecords: List<CheckInRecord>) = withContext(Dispatchers.IO) {
        if (cloudRecords.isEmpty()) return@withContext
        
        val todayStart = LocalDate.now().atStartOfDay()
        val todayEnd = LocalDate.now().atTime(LocalTime.MAX)

        val localRecordsToday = checkInDao.getRecordsBetween(todayStart, todayEnd)
        val localKeys = localRecordsToday.map { "${it.studentId}_${it.timestamp}" }.toSet()

        val newRecords = cloudRecords.filter { cloud ->
            val uniqueKey = "${cloud.studentId}_${cloud.timestamp}"
            !localKeys.contains(uniqueKey)
        }

        if (newRecords.isNotEmpty()) {
            checkInDao.insertAll(newRecords)
        }
    }

    // 5. 📅 FETCH HISTORY
    suspend fun fetchHistory(sekolahId: String, startMillis: Long, endMillis: Long): List<CheckInRecord> = withContext(Dispatchers.IO) {
        FirestoreAttendance.fetchHistoryRecords(sekolahId, startMillis, endMillis)
    }

    // 6. Logika Update Status (PRESENT/SAKIT/IZIN)
    suspend fun updateStatus(record: CheckInRecord, newStatus: String) = withContext(Dispatchers.IO) {
        // Update Room menggunakan ID (Lebih aman sesuai DAO terbaru)
        checkInDao.updateStatusById(record.id, newStatus)
        
        // Update Firestore
        val timeKey = record.timestamp.toString().replace(Regex("[^0-9]"), "").take(12)
        val docId = "${record.studentId}_$timeKey"
        FirestoreAttendance.updateAttendanceStatus(docId, newStatus)
    }

    // 7. Logika Delete Log
    suspend fun deleteRecord(record: CheckInRecord) = withContext(Dispatchers.IO) {
        val timeKey = record.timestamp.toString().replace(Regex("[^0-9]"), "").take(12)
        val docId = "${record.studentId}_$timeKey"
        
        FirestoreAttendance.deleteAttendanceLog(docId)
        checkInDao.delete(record)
    }
}