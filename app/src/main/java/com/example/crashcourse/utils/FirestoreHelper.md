package com.example.crashcourse.utils

import android.util.Log
import com.example.crashcourse.db.CheckInRecord
import com.example.crashcourse.db.FaceEntity
import com.example.crashcourse.db.MasterClassRoom
import com.example.crashcourse.utils.Constants.COLL_ATTENDANCE
import com.example.crashcourse.utils.Constants.COLL_MASTER_CLASSES
import com.example.crashcourse.utils.Constants.COLL_STUDENTS
import com.example.crashcourse.utils.Constants.COLL_USERS
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

/**
 * 🛰️ Azura Tech Firestore Helper (FINAL)
 * Single contract layer between Firestore ↔ Room ↔ ViewModel.
 */
object FirestoreHelper {

    private const val TAG = "FirestoreHelper"
    private val db by lazy { FirebaseFirestore.getInstance() }

    // ==========================================
    // 0. AUTH & USER
    // ==========================================
    suspend fun getUserProfile(uid: String): Map<String, Any?>? {
        return try {
            db.collection(COLL_USERS).document(uid).get().await().data
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateDeviceBinding(uid: String, deviceId: String) {
        try {
            db.collection(COLL_USERS)
                .document(uid)
                .update("deviceId", deviceId)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Device binding failed", e)
        }
    }

    // ==========================================
    // 1. STUDENT / FACE SMART SYNC
    // ==========================================
    suspend fun fetchSmartSyncStudents(
        sekolahId: String,
        assignedClasses: List<String>,
        role: String,
        lastSync: Long
    ): List<FaceEntity> {
        return try {
            var query: Query = db.collection(COLL_STUDENTS)
                .whereEqualTo(Constants.KEY_SEKOLAH_ID, sekolahId)
                .whereGreaterThan(Constants.KEY_TIMESTAMP, lastSync)

            if (role != Constants.ROLE_ADMIN && assignedClasses.isNotEmpty()) {
                query = query.whereIn(Constants.PILLAR_CLASS, assignedClasses)
            }

            query.get().await().documents.mapNotNull { doc ->
                val embedding = (doc.get(Constants.FIELD_EMBEDDING) as? List<*>)
                    ?.mapNotNull { (it as? Number)?.toFloat() }
                    ?.toFloatArray()

                embedding?.let {
                    FaceEntity(
                        studentId = doc.getString(Constants.FIELD_STUDENT_ID) ?: "",
                        sekolahId = doc.getString(Constants.KEY_SEKOLAH_ID) ?: sekolahId,
                        name = doc.getString(Constants.KEY_NAME) ?: "Unknown",
                        photoUrl = doc.getString(Constants.FIELD_PHOTO_URL),
                        embedding = it,
                        className = doc.getString(Constants.PILLAR_CLASS) ?: "",
                        role = doc.getString(Constants.FIELD_ROLE) ?: Constants.ROLE_USER,
                        grade = doc.getString(Constants.PILLAR_GRADE) ?: "",
                        subGrade = doc.getString(Constants.PILLAR_SUB_GRADE) ?: "",
                        program = doc.getString(Constants.PILLAR_PROGRAM) ?: "",
                        subClass = doc.getString(Constants.PILLAR_SUB_CLASS) ?: "",
                        timestamp = doc.getLong(Constants.KEY_TIMESTAMP) ?: System.currentTimeMillis()
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Student sync error", e)
            emptyList()
        }
    }

    suspend fun syncStudentToFirestore(face: FaceEntity, sekolahId: String): Boolean {
        return try {
            val data: Map<String, Any?> = mapOf(
                Constants.FIELD_STUDENT_ID to face.studentId,
                Constants.KEY_SEKOLAH_ID to sekolahId,
                Constants.KEY_NAME to face.name,
                Constants.FIELD_EMBEDDING to face.embedding.map { it.toDouble() },
                Constants.KEY_TIMESTAMP to System.currentTimeMillis(),
                Constants.FIELD_PHOTO_URL to face.photoUrl,
                Constants.PILLAR_CLASS to face.className,
                Constants.FIELD_ROLE to face.role,
                Constants.PILLAR_GRADE to face.grade,
                Constants.PILLAR_SUB_GRADE to face.subGrade,
                Constants.PILLAR_PROGRAM to face.program,
                Constants.PILLAR_SUB_CLASS to face.subClass
            )

            db.collection(COLL_STUDENTS)
                .document(face.studentId)
                .set(data, SetOptions.merge())
                .await()

            true
        } catch (e: Exception) {
            false
        }
    }

    // ==========================================
    // 2. MASTER CLASS
    // ==========================================
    suspend fun fetchMasterClasses(sekolahId: String): List<MasterClassRoom> {
        return try {
            db.collection(COLL_MASTER_CLASSES)
                .whereEqualTo(Constants.KEY_SEKOLAH_ID, sekolahId)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    MasterClassRoom(
                        classId = doc.getLong(Constants.KEY_ID)?.toInt() ?: 0,
                        sekolahId = sekolahId,
                        className = doc.getString(Constants.PILLAR_CLASS) ?: "",
                        gradeId = doc.getLong("gradeId")?.toInt() ?: 0,
                        classOptionId = doc.getLong("classOptionId")?.toInt() ?: 0,
                        programId = doc.getLong("programId")?.toInt() ?: 0,
                        subClassId = doc.getLong("subClassId")?.toInt() ?: 0,
                        subGradeId = doc.getLong("subGradeId")?.toInt() ?: 0,
                        roleId = doc.getLong("roleId")?.toInt() ?: 0
                    )
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ==========================================
    // 3. ATTENDANCE (WRITE)
    // ==========================================
    suspend fun syncAttendanceLog(
        record: CheckInRecord,
        sekolahId: String
    ): String? {
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
                Constants.FIELD_DATE to record.timestamp.toLocalDate().toString()
            )

            db.collection(COLL_ATTENDANCE)
                .document(docId)
                .set(data, SetOptions.merge())
                .await()

            docId
        } catch (e: Exception) {
            null
        }
    }

    fun updateAttendanceStatus(docId: String, newStatus: String) {
        db.collection(COLL_ATTENDANCE)
            .document(docId)
            .update(Constants.FIELD_STATUS, newStatus)
    }

    fun deleteAttendanceLog(firestoreId: String) {
        db.collection(COLL_ATTENDANCE)
            .document(firestoreId)
            .delete()
    }

    // ==========================================
    // 3b. ATTENDANCE (READ – HISTORY)
    // ==========================================
    suspend fun fetchHistoryRecords(
        sekolahId: String,
        startMillis: Long,
        endMillis: Long
    ): List<CheckInRecord> {
        return try {
            val startTs = Timestamp(Date(startMillis))
            val endTs = Timestamp(Date(endMillis))

            db.collection(COLL_ATTENDANCE)
                .whereEqualTo(Constants.KEY_SEKOLAH_ID, sekolahId)
                .whereGreaterThanOrEqualTo(Constants.FIELD_TIMESTAMP, startTs)
                .whereLessThanOrEqualTo(Constants.FIELD_TIMESTAMP, endTs)
                .get()
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
                        className = doc.getString(Constants.PILLAR_CLASS),
                        gradeName = doc.getString(Constants.PILLAR_GRADE),
                        role = doc.getString(Constants.FIELD_ROLE)
                    )
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ==========================================
    // 4. OPTIONS
    // ==========================================
    suspend fun saveOptionToFirestore(
        collection: String,
        data: Map<String, Any>
    ) {
        db.collection(collection).add(data).await()
    }

    suspend fun deleteOptionFromFirestore(
        collection: String,
        docId: String
    ) {
        db.collection(collection).document(docId).delete().await()
    }

    fun listenToOptions(
        collection: String,
        onUpdate: (List<Map<String, Any>>) -> Unit
    ): ListenerRegistration {
        return db.collection(collection)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    onUpdate(it.documents.mapNotNull { d -> d.data })
                }
            }
    }

    // ==========================================
    // 5. REALTIME ATTENDANCE
    // ==========================================
    fun listenToTodayCheckIns(
        sekolahId: String,
        onUpdate: (List<CheckInRecord>) -> Unit
    ): ListenerRegistration {
        val today = LocalDate.now().toString()

        return db.collection(COLL_ATTENDANCE)
            .whereEqualTo(Constants.KEY_SEKOLAH_ID, sekolahId)
            .whereEqualTo(Constants.FIELD_DATE, today)
            .addSnapshotListener { snapshot, _ ->
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

    suspend fun deleteStudentFromFirestore(studentId: String) {
        db.collection(COLL_STUDENTS)
            .document(studentId)
            .delete()
            .await()
    }



    // ==========================================
// 4a. FETCH OPTIONS (ONE-SHOT)
// ==========================================
suspend fun fetchOptionsOnce(
    collection: String
): List<Map<String, Any>> {
    return try {
        db.collection(collection)
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                val data = doc.data?.toMutableMap() ?: return@mapNotNull null
                // ensure ID always exists
                data["id"] = data["id"] ?: doc.id.toIntOrNull() ?: 0
                data
            }
    } catch (e: Exception) {
        emptyList()
    }
}
// ==========================================
// 4b. SAVE OPTION (WITH CUSTOM ID)
// ==========================================
suspend fun saveOptionToFirestore(
    collection: String,
    id: Int,
    data: Map<String, Any>
) {
    db.collection(collection)
        .document(id.toString())
        .set(data, SetOptions.merge())
        .await()
}
// ==========================================
// 4c. DELETE OPTION (INT ID)
// ==========================================
suspend fun deleteOptionFromFirestore(
    collection: String,
    id: Int
) {
    db.collection(collection)
        .document(id.toString())
        .delete()
        .await()
}

}
