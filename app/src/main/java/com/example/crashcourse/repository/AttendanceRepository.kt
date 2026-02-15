package com.example.crashcourse.repository

import android.app.Application
import android.util.Log
import com.example.crashcourse.db.AppDatabase
import com.example.crashcourse.db.CheckInRecord
import com.example.crashcourse.firestore.FirestoreAttendance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/**
 * 📊 AttendanceRepository (V.10.20 - Build Success Ready)
 * Jantung manajemen data absensi AzuraTech.
 * Menghubungkan Room (Lokal) dan Firestore (Cloud).
 */
class AttendanceRepository(application: Application) {
    private val db = AppDatabase.getInstance(application)
    private val checkInDao = db.checkInRecordDao()

    companion object {
        private const val TAG = "AttendanceRepo"
        private const val CHECK_IN_COOLDOWN_SEC = 30L
    }

    // ==========================================
    // 🔍 READ OPERATIONS
    // ==========================================

    fun getRecordsBySchoolFlow(schoolId: String): Flow<List<CheckInRecord>> =
        checkInDao.getAllRecordsBySchoolFlow(schoolId)

    suspend fun getLastRecordForClass(studentId: String, className: String): CheckInRecord? = 
        withContext(Dispatchers.IO) { checkInDao.getLastRecordForClass(studentId, className) }

    // ==========================================
    // ✍️ WRITE & SYNC OPERATIONS
    // ==========================================

    suspend fun saveAttendance(record: CheckInRecord, schoolId: String): String = withContext(Dispatchers.IO) {
        try {
            val lastRecord = checkInDao.getLastRecordForClass(record.studentId, record.className)
            if (lastRecord != null && lastRecord.timestamp.isAfter(LocalDateTime.now().minusSeconds(CHECK_IN_COOLDOWN_SEC))) {
                return@withContext "COOLDOWN"
            }

            // 1. Simpan Lokal
            val localId = checkInDao.insert(record)
            
            // 2. Upload Cloud (Panggil Firestore)
            val firestoreId = FirestoreAttendance.saveCheckIn(record, schoolId)
            
            if (firestoreId != null) {
                checkInDao.markAsSynced(localId.toInt(), firestoreId)
                return@withContext "SUCCESS"
            } else {
                return@withContext "SAVED_OFFLINE"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving attendance", e)
            return@withContext e.message ?: "UNKNOWN_ERROR"
        }
    }

    suspend fun syncAttendance(records: List<CheckInRecord>) = withContext(Dispatchers.IO) {
        try {
            checkInDao.insertAll(records)
        } catch (e: Exception) { 
            Log.e(TAG, "❌ syncAttendance failed", e) 
        }
    }

    suspend fun fetchHistoricalData(schoolId: String, startM: Long, endM: Long): List<CheckInRecord> {
        return try {
            // 🔥 Error line 80: Resolve ini dengan update FirestoreAttendance.kt
            FirestoreAttendance.fetchHistoricalData(schoolId, startM, endM)
        } catch (e: Exception) {
            Log.e(TAG, "❌ fetchHistoricalData failed", e)
            emptyList()
        }
    }

    // ==========================================
    // 🛠️ CRUD HANDLERS (Update & Delete)
    // ==========================================

    suspend fun updateStatus(record: CheckInRecord, newStatus: String) = withContext(Dispatchers.IO) {
        try {
            // 1. Update Room (Lokal)
            checkInDao.updateStatusById(record.id, newStatus)
            
            // 2. Update Cloud (Panggil Firestore)
            record.firestoreId?.let { fsId ->
                // 🔥 Error line 98: Resolve ini dengan update FirestoreAttendance.kt
                FirestoreAttendance.updateStatus(fsId, newStatus)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ updateStatus failed", e)
        }
    }

    suspend fun deleteRecord(record: CheckInRecord) = withContext(Dispatchers.IO) {
        try {
            // 1. Hapus Lokal
            checkInDao.delete(record)
            
            // 2. Hapus Cloud (Panggil Firestore)
            record.firestoreId?.let { fsId ->
                // 🔥 Error line 112: Resolve ini dengan update FirestoreAttendance.kt
                FirestoreAttendance.deleteRecord(fsId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ deleteRecord failed", e)
        }
    }
}