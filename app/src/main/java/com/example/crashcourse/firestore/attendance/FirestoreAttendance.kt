package com.example.crashcourse.firestore

import android.util.Log
import com.example.crashcourse.db.CheckInRecord
import com.example.crashcourse.firestore.core.FirestoreCore
import com.example.crashcourse.firestore.core.FirestorePaths
import com.example.crashcourse.utils.Constants
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

/**
 * 📅 FirestoreAttendance (FINAL & OPTIMIZED)
 * Pusat kendali data absensi di Cloud.
 */
object FirestoreAttendance {

    private const val TAG = "FirestoreAttendance"
    private val db = FirestoreCore.db

    // ==========================================
    // 1️⃣ SAVE CHECK-IN
    // ==========================================
    suspend fun saveCheckIn(record: CheckInRecord, sekolahId: String): String? {
        return try {
            val timeKey = record.timestamp.toString().replace(Regex("[^0-9]"), "").take(12)
            val docId = "${record.studentId}_$timeKey"

            val data: Map<String, Any?> = mapOf(
                Constants.FIELD_FIRESTORE_ID to docId,
                Constants.KEY_SEKOLAH_ID to sekolahId,
                Constants.FIELD_STUDENT_ID to record.studentId,
                Constants.KEY_NAME to record.name,
                Constants.FIELD_STATUS to record.status,
                Constants.PILLAR_CLASS to record.className,
                Constants.PILLAR_GRADE to record.gradeName,
                Constants.FIELD_ROLE to record.role,
                Constants.FIELD_TIMESTAMP to Timestamp(
                    Date.from(record.timestamp.atZone(ZoneId.systemDefault()).toInstant())
                ),
                Constants.FIELD_DATE to record.timestamp.toLocalDate().toString(),
                Constants.FIELD_VERIFIED to record.verified,
                Constants.FIELD_PHOTO_PATH to record.photoPath
            )

            db.collection(FirestorePaths.ATTENDANCE)
                .document(docId)
                .set(data, SetOptions.merge())
                .await()

            docId
        } catch (e: Exception) {
            Log.e(TAG, "❌ saveCheckIn failed", e)
            null
        }
    }

    // ==========================================
    // 2️⃣ FETCH HISTORY (Optimized with Server-side Filter)
    // ==========================================
    suspend fun fetchHistoryRecords(
        sekolahId: String,
        startMillis: Long,
        endMillis: Long,
        className: String? = null // 🚀 Tambahkan parameter opsional
    ): List<CheckInRecord> {
        return try {
            val startTs = Timestamp(Date(startMillis))
            val endTs = Timestamp(Date(endMillis))

            // Inisialisasi Query Dasar
            var query: Query = db.collection(FirestorePaths.ATTENDANCE)
                .whereEqualTo(Constants.KEY_SEKOLAH_ID, sekolahId)
                .whereGreaterThanOrEqualTo(Constants.FIELD_TIMESTAMP, startTs)
                .whereLessThanOrEqualTo(Constants.FIELD_TIMESTAMP, endTs)

            // 🚀 SERVER-SIDE FILTER: Jika admin pilih kelas tertentu, filter di Cloud
            if (!className.isNullOrBlank() && className != "Semua Kelas") {
                query = query.whereEqualTo(Constants.PILLAR_CLASS, className)
            }

            query.get()
                .await()
                .documents
                .mapNotNull { doc ->
                    val ts = doc.getTimestamp(Constants.FIELD_TIMESTAMP)?.toDate() ?: return@mapNotNull null
                    val time = LocalDateTime.ofInstant(ts.toInstant(), ZoneId.systemDefault())

                    CheckInRecord(
                        id = 0,
                        studentId = doc.getString(Constants.FIELD_STUDENT_ID) ?: "",
                        name = doc.getString(Constants.KEY_NAME) ?: "Unknown",
                        timestamp = time,
                        status = doc.getString(Constants.FIELD_STATUS) ?: Constants.STATUS_PRESENT,
                        verified = doc.getBoolean(Constants.FIELD_VERIFIED) ?: true,
                        syncStatus = "SYNCED",
                        photoPath = doc.getString(Constants.FIELD_PHOTO_PATH) ?: "",
                        className = doc.getString(Constants.PILLAR_CLASS) ?: "",
                        gradeName = doc.getString(Constants.PILLAR_GRADE) ?: "",
                        role = doc.getString(Constants.FIELD_ROLE) ?: Constants.ROLE_USER
                    )
                }
        } catch (e: Exception) {
            Log.e(TAG, "❌ fetchHistoryRecords failed", e)
            emptyList()
        }
    }

    // ==========================================
    // 3️⃣ UPDATE STATUS
    // ==========================================
    suspend fun updateAttendanceStatus(docId: String, newStatus: String) {
        try {
            db.collection(FirestorePaths.ATTENDANCE)
                .document(docId)
                .update(Constants.FIELD_STATUS, newStatus)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "❌ updateAttendanceStatus failed", e)
        }
    }

    // ==========================================
    // 4️⃣ DELETE LOG
    // ==========================================
    suspend fun deleteAttendanceLog(firestoreId: String) {
        try {
            db.collection(FirestorePaths.ATTENDANCE)
                .document(firestoreId)
                .delete()
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "❌ deleteAttendanceLog failed", e)
        }
    }

    // ==========================================
    // 5️⃣ REALTIME TODAY
    // ==========================================
    fun listenToTodayCheckIns(
        sekolahId: String,
        onUpdate: (List<CheckInRecord>) -> Unit
    ): ListenerRegistration {
        val today = LocalDate.now().toString()

        return db.collection(FirestorePaths.ATTENDANCE)
            .whereEqualTo(Constants.KEY_SEKOLAH_ID, sekolahId)
            .whereEqualTo(Constants.FIELD_DATE, today)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "❌ listenToTodayCheckIns failed", e)
                    return@addSnapshotListener
                }

                val records = snapshot?.documents?.mapNotNull { doc ->
                    val ts = doc.getTimestamp(Constants.FIELD_TIMESTAMP)?.toDate() ?: return@mapNotNull null
                    val time = LocalDateTime.ofInstant(ts.toInstant(), ZoneId.systemDefault())

                    CheckInRecord(
                        id = 0,
                        studentId = doc.getString(Constants.FIELD_STUDENT_ID) ?: "",
                        name = doc.getString(Constants.KEY_NAME) ?: "Unknown",
                        timestamp = time,
                        status = doc.getString(Constants.FIELD_STATUS) ?: Constants.STATUS_PRESENT,
                        verified = doc.getBoolean(Constants.FIELD_VERIFIED) ?: true,
                        syncStatus = "SYNCED",
                        photoPath = doc.getString(Constants.FIELD_PHOTO_PATH) ?: "",
                        className = doc.getString(Constants.PILLAR_CLASS) ?: "",
                        gradeName = doc.getString(Constants.PILLAR_GRADE) ?: "",
                        role = doc.getString(Constants.FIELD_ROLE) ?: Constants.ROLE_USER
                    )
                } ?: emptyList()

                onUpdate(records)
            }
    }
}