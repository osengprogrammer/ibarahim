package com.example.crashcourse.repository

import android.app.Application
import android.util.Log
import com.example.crashcourse.db.AppDatabase
import com.example.crashcourse.db.CheckInRecord
import com.example.crashcourse.firestore.FirestoreAttendance
import com.example.crashcourse.util.NativeKeyStore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/**
 * 📊 AttendanceRepository (V.20.3 - Stainless Steel Edition)
 * Jantung manajemen data AzuraTech.
 * Update: Menambahkan schoolId ke record & Verbose Debugging.
 */
class AttendanceRepository(application: Application) {
    private val db = AppDatabase.getInstance(application)
    private val checkInDao = db.checkInRecordDao()
    private val functions: FirebaseFunctions = Firebase.functions("us-central1")

    companion object {
        private const val TAG = "AzuraRepo"
        private const val CHECK_IN_COOLDOWN_SEC = 20L
    }

    // ==========================================
    // ✍️ WRITE & SECURE CLOUD SYNC
    // ==========================================

    /**
     * saveAttendance: Menyimpan ke Cloud via Firebase Functions.
     * Jika gagal (offline), akan otomatis tersimpan ke Room dengan status PENDING.
     */
    suspend fun saveAttendance(record: CheckInRecord, schoolId: String, rawDistance: Float): String = withContext(Dispatchers.IO) {
        try {
            // 1. Cek Cooldown Lokal (Cegah double scan dalam waktu singkat)
            val lastRecord = checkInDao.getLastRecordForClass(record.studentId, record.className)
            if (lastRecord != null && lastRecord.timestamp.isAfter(LocalDateTime.now().minusSeconds(CHECK_IN_COOLDOWN_SEC))) {
                Log.w(TAG, "⏳ Cooldown active for ${record.name}")
                return@withContext "COOLDOWN"
            }

            // 2. Ambil Kunci Security dari JNI/C++
            val isoKey = NativeKeyStore.getIsoKey()

            // 3. Persiapkan Payload (Verbose Logging untuk Debugging)
            val data = hashMapOf(
                "studentId" to record.studentId,
                "name" to record.name,
                "distance" to rawDistance, 
                "isoKey" to isoKey,       
                "className" to record.className,
                "grade" to record.gradeName,
                "schoolId" to schoolId
            )
            
            Log.d(TAG, "🚀 Menembak Cloud Function: $data")

            // 4. Panggil Cloud Function 'secureCheckIn'
            val result = functions
                .getHttpsCallable("secureCheckIn")
                .call(data)
                .await()

            // 5. Validasi Response Server
            val responseData = result.data as? Map<*, *>
            val status = responseData?.get("status") as? String
            
            return@withContext if (status == "SUCCESS") {
                Log.i(TAG, "✅ Cloud verified: ${record.name}")
                checkInDao.insert(record.copy(syncStatus = "SYNCED", schoolId = schoolId))
                "SUCCESS"
            } else {
                Log.e(TAG, "❌ Server rejected record: $status")
                "SERVER_REJECTED"
            }

        } catch (e: Exception) {
            // 6. Jalur Offline (Stainless Steel Path)
            Log.e(TAG, "🚨 Cloud Shield Error (Mode Offline aktif): ${e.message}")
            return@withContext try {
                // Tetap simpan ke Lokal meskipun internet mati
                checkInDao.insert(record.copy(syncStatus = "PENDING", schoolId = schoolId))
                "SAVED_OFFLINE"
            } catch (localError: Exception) {
                Log.e(TAG, "❌ Database Lokal Error: ${localError.message}")
                "DATABASE_ERROR"
            }
        }
    }

    // ==========================================
    // 🔍 READ & MAINTENANCE
    // ==========================================

    /**
     * Membaca riwayat berdasarkan schoolId secara reaktif.
     */
    fun getRecordsBySchoolFlow(schoolId: String): Flow<List<CheckInRecord>> =
        checkInDao.getRecordsBySchoolDirect(schoolId)

    suspend fun fetchHistoricalData(schoolId: String, startM: Long, endM: Long): List<CheckInRecord> {
        return try {
            FirestoreAttendance.fetchHistoricalData(schoolId, startM, endM)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Gagal tarik riwayat cloud", e)
            emptyList()
        }
    }

    suspend fun updateStatus(record: CheckInRecord, newStatus: String) = withContext(Dispatchers.IO) {
        checkInDao.updateStatusById(record.id, newStatus)
    }

    suspend fun deleteRecord(record: CheckInRecord) = withContext(Dispatchers.IO) {
        checkInDao.delete(record)
    }

    suspend fun syncAttendance(records: List<CheckInRecord>) = withContext(Dispatchers.IO) {
        // Pastikan schoolId ikut tersimpan saat sinkronisasi massal
        checkInDao.insertAll(records)
    }
}