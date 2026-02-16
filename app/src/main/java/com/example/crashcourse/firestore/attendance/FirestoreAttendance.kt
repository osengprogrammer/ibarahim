package com.example.crashcourse.firestore

import android.util.Log
import com.example.crashcourse.db.CheckInRecord
import com.example.crashcourse.firestore.core.FirestoreCore
import com.example.crashcourse.firestore.core.FirestorePaths
import com.example.crashcourse.utils.Constants
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

/**
 * 📅 FirestoreAttendance (V.20.1 - Cloud Verified)
 * Update: Menambahkan schoolId ke constructor CheckInRecord untuk sinkronisasi Room.
 */
object FirestoreAttendance {

    private const val TAG = "FirestoreAttendance"
    private val db = FirestoreCore.db

    // ==========================================
    // 🛡️ READ-ONLY OPERATIONS (Fetching Data)
    // ==========================================

    /**
     * Mengambil data sejarah absensi yang sudah diverifikasi oleh Cloud.
     */
    suspend fun fetchHistoricalData(
        sekolahId: String,
        startMillis: Long,
        endMillis: Long,
        className: String? = null
    ): List<CheckInRecord> {
        return try {
            val startTs = Timestamp(Date(startMillis))
            val endTs = Timestamp(Date(endMillis))

            var query: Query = db.collection("attendance_logs")
                .whereEqualTo(Constants.KEY_SEKOLAH_ID, sekolahId)
                .whereGreaterThanOrEqualTo(Constants.FIELD_TIMESTAMP, startTs)
                .whereLessThanOrEqualTo(Constants.FIELD_TIMESTAMP, endTs)

            if (!className.isNullOrBlank() && className != "Semua Kelas") {
                query = query.whereEqualTo(Constants.PILLAR_CLASS, className)
            }

            query.get()
                .await()
                .documents
                .mapNotNull { doc ->
                    val ts = doc.getTimestamp(Constants.FIELD_TIMESTAMP)?.toDate() ?: return@mapNotNull null
                    val time = LocalDateTime.ofInstant(ts.toInstant(), ZoneId.systemDefault())

                    // 🔥 FIX: Menambahkan schoolId agar tidak error saat compile
                    CheckInRecord(
                        id = 0,
                        studentId = doc.getString(Constants.FIELD_STUDENT_ID) ?: "",
                        name = doc.getString(Constants.KEY_NAME) ?: "Unknown",
                        schoolId = sekolahId, // Menggunakan parameter fungsi
                        timestamp = time,
                        status = doc.getString(Constants.FIELD_STATUS) ?: Constants.STATUS_PRESENT,
                        verified = true,
                        syncStatus = "SYNCED",
                        photoPath = doc.getString(Constants.FIELD_PHOTO_PATH) ?: "",
                        className = doc.getString(Constants.PILLAR_CLASS) ?: "",
                        gradeName = doc.getString(Constants.PILLAR_GRADE) ?: "",
                        role = doc.getString(Constants.FIELD_ROLE) ?: Constants.ROLE_USER,
                        firestoreId = doc.id
                    )
                }
        } catch (e: Exception) {
            Log.e(TAG, "❌ fetchHistoricalData failed", e)
            emptyList()
        }
    }

    /**
     * Realtime Listener untuk dashboard hari ini.
     */
    fun listenToTodayCheckIns(
        sekolahId: String,
        onUpdate: (List<CheckInRecord>) -> Unit
    ): ListenerRegistration {
        val startOfToday = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant())
        val startTs = Timestamp(startOfToday)

        return db.collection("attendance_logs")
            .whereEqualTo(Constants.KEY_SEKOLAH_ID, sekolahId)
            .whereGreaterThanOrEqualTo(Constants.FIELD_TIMESTAMP, startTs)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "❌ listenToTodayCheckIns failed", e)
                    return@addSnapshotListener
                }

                val records = snapshot?.documents?.mapNotNull { doc ->
                    val ts = doc.getTimestamp(Constants.FIELD_TIMESTAMP)?.toDate() ?: return@mapNotNull null
                    val time = LocalDateTime.ofInstant(ts.toInstant(), ZoneId.systemDefault())

                    // 🔥 FIX: Menambahkan schoolId agar tidak error saat compile
                    CheckInRecord(
                        id = 0,
                        studentId = doc.getString(Constants.FIELD_STUDENT_ID) ?: "",
                        name = doc.getString(Constants.KEY_NAME) ?: "Unknown",
                        schoolId = sekolahId, // Menggunakan parameter fungsi
                        timestamp = time,
                        status = doc.getString(Constants.FIELD_STATUS) ?: Constants.STATUS_PRESENT,
                        verified = true,
                        syncStatus = "SYNCED",
                        photoPath = doc.getString(Constants.FIELD_PHOTO_PATH) ?: "",
                        className = doc.getString(Constants.PILLAR_CLASS) ?: "",
                        gradeName = doc.getString(Constants.PILLAR_GRADE) ?: "",
                        role = doc.getString(Constants.FIELD_ROLE) ?: Constants.ROLE_USER,
                        firestoreId = doc.id
                    )
                } ?: emptyList()

                onUpdate(records)
            }
    }
}