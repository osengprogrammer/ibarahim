package com.example.crashcourse.utils

import android.util.Log
import com.example.crashcourse.db.CheckInRecord
import com.example.crashcourse.db.FaceEntity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date

// 🚀 IMPORT CONSTANTS
import com.example.crashcourse.utils.Constants.COLL_ATTENDANCE
import com.example.crashcourse.utils.Constants.COLL_STUDENTS
import com.example.crashcourse.utils.Constants.COLL_USERS
import com.example.crashcourse.utils.Constants.FIELD_CLASS_NAME
import com.example.crashcourse.utils.Constants.FIELD_LAST_UPDATED
import com.example.crashcourse.utils.Constants.FIELD_STUDENT_ID
import com.example.crashcourse.utils.Constants.FIELD_TIMESTAMP

object FirestoreHelper {
    private const val TAG = "FirestoreHelper"
    
    private val db by lazy { FirebaseFirestore.getInstance() }

    // ==========================================
    // 1. DATA SISWA (Sync Profil & Wajah)
    // ==========================================

    suspend fun getScopedStudentsFromFirestore(uid: String): List<FaceEntity> {
        return try {
            val userDoc = db.collection(COLL_USERS).document(uid).get().await()
            val role = userDoc.getString("role") ?: "USER"
            
            @Suppress("UNCHECKED_CAST")
            val assignedClasses = userDoc.get("assigned_classes") as? List<String> ?: emptyList()

            val query: Query = if (role == "ADMIN" || assignedClasses.isEmpty()) {
                db.collection(COLL_STUDENTS)
            } else {
                db.collection(COLL_STUDENTS).whereIn(FIELD_CLASS_NAME, assignedClasses)
            }

            val snapshot = query.get().await()
            snapshot.documents.mapNotNull { doc ->
                val embeddingList = doc.get("embedding") as? List<Double>
                val embeddingFloatArray = embeddingList?.map { it.toFloat() }?.toFloatArray()
                
                if (embeddingFloatArray != null) {
                    mapDocToFaceEntity(doc, embeddingFloatArray)
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error Download Students: ${e.message}")
            emptyList()
        }
    }

    suspend fun syncStudentToFirestore(face: FaceEntity): Boolean {
        if (face.studentId.isBlank()) return false
        return try {
            val embeddingList = face.embedding.map { it.toDouble() }
            val studentData = mapFaceEntityToMap(face, embeddingList)

            db.collection(COLL_STUDENTS) 
                .document(face.studentId)
                .set(studentData, SetOptions.merge())
                .await() 
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateStudentInFirestore(face: FaceEntity) {
        try {
            val updateData = mutableMapOf<String, Any?>(
                "name" to face.name,
                FIELD_CLASS_NAME to face.className,
                "classId" to face.classId,
                "subClass" to face.subClass,
                "subClassId" to face.subClassId,
                "grade" to face.grade,
                "gradeId" to face.gradeId,
                "subGrade" to face.subGrade,
                "subGradeId" to face.subGradeId,
                "program" to face.program,
                "programId" to face.programId,
                "role" to face.role,
                "roleId" to face.roleId,
                FIELD_LAST_UPDATED to System.currentTimeMillis()
            )
            db.collection(COLL_STUDENTS).document(face.studentId).update(updateData).await()
            Log.d(TAG, "✅ Profil Cloud Updated: ${face.name}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update student: ${e.message}")
        }
    }

    suspend fun deleteStudentFromFirestore(studentId: String) {
        try {
            db.collection(COLL_STUDENTS).document(studentId).delete().await()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to delete student: ${e.message}")
        }
    }

    // ==========================================
    // 2. LOG ABSENSI (CRUD & Real-time)
    // ==========================================

    suspend fun syncAttendanceLog(record: CheckInRecord): String? {
        return try {
            val timeKey = record.timestamp.toString().replace(Regex("[^0-9]"), "")
            val customId = "${record.studentId}_$timeKey"
            
            val data = hashMapOf(
                "firestoreId" to customId,
                FIELD_STUDENT_ID to record.studentId,
                "name" to record.name,
                "status" to record.status,
                "note" to record.note,
                FIELD_CLASS_NAME to record.className,
                "gradeName" to record.gradeName,
                FIELD_TIMESTAMP to Timestamp(Date.from(record.timestamp.atZone(ZoneId.systemDefault()).toInstant()))
            )

            db.collection(COLL_ATTENDANCE)
                .document(customId) 
                .set(data, SetOptions.merge())
                .await()
                
            customId
        } catch (e: Exception) {
            Log.e(TAG, "❌ Sync Attendance Error: ${e.message}")
            null
        }
    }

    /**
     * 🚀 UPDATE STATUS (Untuk CRUD Riwayat)
     */
    suspend fun updateAttendanceStatus(docId: String, newStatus: String) {
        try {
            db.collection(COLL_ATTENDANCE)
                .document(docId)
                .update("status", newStatus)
                .await()
            Log.d(TAG, "✅ Cloud Status Updated: $docId -> $newStatus")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Gagal update status di cloud: ${e.message}")
            throw e
        }
    }

    suspend fun deleteAttendanceLog(firestoreId: String) {
        try {
            db.collection(COLL_ATTENDANCE).document(firestoreId).delete().await()
            Log.d(TAG, "🗑️ Cloud Record Deleted: $firestoreId")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Gagal Hapus Cloud: ${e.message}")
        }
    }

    fun listenToTodayCheckIns(onDataReceived: (List<CheckInRecord>) -> Unit): ListenerRegistration {
        val today = LocalDate.now()
        val start = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant())
        val end = Date.from(today.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant())

        return db.collection(COLL_ATTENDANCE)
            .whereGreaterThanOrEqualTo(FIELD_TIMESTAMP, Timestamp(start))
            .whereLessThanOrEqualTo(FIELD_TIMESTAMP, Timestamp(end))
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val records = snapshot.documents.mapNotNull { mapDocToCheckInRecord(it) }
                onDataReceived(records)
            }
    }

    suspend fun fetchHistoryRecords(startDate: LocalDate, endDate: LocalDate, className: String?): List<CheckInRecord> {
        return try {
            val start = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
            val end = Date.from(endDate.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant())

            var query: Query = db.collection(COLL_ATTENDANCE)
                .whereGreaterThanOrEqualTo(FIELD_TIMESTAMP, Timestamp(start))
                .whereLessThanOrEqualTo(FIELD_TIMESTAMP, Timestamp(end))

            if (!className.isNullOrBlank() && className != "Semua Kelas") {
                query = query.whereEqualTo(FIELD_CLASS_NAME, className)
            }

            val snapshot = query.get().await()
            snapshot.documents.mapNotNull { mapDocToCheckInRecord(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ==========================================
    // 3. MASTER DATA OPTIONS
    // ==========================================

    suspend fun fetchOptionsOnce(collectionName: String): List<Map<String, Any>> {
        return try {
            val snapshot = db.collection(collectionName).get().await()
            snapshot.documents.map { doc ->
                val data = doc.data?.toMutableMap() ?: mutableMapOf()
                data["id"] = doc.id.toIntOrNull() ?: 0
                data
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun listenToOptions(collectionName: String, onDataChange: (List<Map<String, Any>>) -> Unit): ListenerRegistration {
        return db.collection(collectionName).addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) return@addSnapshotListener
            val dataList = snapshot.documents.map { doc ->
                val data = doc.data?.toMutableMap() ?: mutableMapOf()
                data["id"] = doc.id.toIntOrNull() ?: 0
                data
            }
            onDataChange(dataList)
        }
    }

    suspend fun saveOptionToFirestore(collectionName: String, id: Int, data: Map<String, Any>) {
        db.collection(collectionName).document(id.toString()).set(data, SetOptions.merge()).await()
    }

    suspend fun deleteOptionFromFirestore(collectionName: String, id: Int) {
        db.collection(collectionName).document(id.toString()).delete().await()
    }

    // ==========================================
    // 🛠️ INTERNAL MAPPING HELPERS
    // ==========================================

    private fun mapDocToFaceEntity(doc: com.google.firebase.firestore.DocumentSnapshot, embedding: FloatArray): FaceEntity {
        return FaceEntity(
            studentId = doc.getString(FIELD_STUDENT_ID) ?: "",
            name = doc.getString("name") ?: "Unknown",
            photoUrl = doc.getString("photoUrl"), 
            embedding = embedding,
            className = doc.getString(FIELD_CLASS_NAME) ?: "",
            subClass = doc.getString("subClass") ?: "",
            grade = doc.getString("grade") ?: "",
            subGrade = doc.getString("subGrade") ?: "",
            program = doc.getString("program") ?: "",
            role = doc.getString("role") ?: "",
            classId = doc.getLong("classId")?.toInt(),
            subClassId = doc.getLong("subClassId")?.toInt(),
            gradeId = doc.getLong("gradeId")?.toInt(),
            subGradeId = doc.getLong("subGradeId")?.toInt(),
            programId = doc.getLong("programId")?.toInt(),
            roleId = doc.getLong("roleId")?.toInt(),
            timestamp = System.currentTimeMillis()
        )
    }

    private fun mapFaceEntityToMap(face: FaceEntity, embedding: List<Double>): Map<String, Any?> {
        return hashMapOf(
            FIELD_STUDENT_ID to face.studentId,
            "name" to face.name,
            FIELD_CLASS_NAME to face.className,
            "subClass" to face.subClass,
            "grade" to face.grade,
            "subGrade" to face.subGrade,
            "program" to face.program,
            "role" to face.role,
            "classId" to face.classId,
            "subClassId" to face.subClassId,
            "gradeId" to face.gradeId,
            "subGradeId" to face.subGradeId,
            "programId" to face.programId,
            "roleId" to face.roleId,
            "embedding" to embedding,
            FIELD_LAST_UPDATED to System.currentTimeMillis(),
            "photoUrl" to (face.photoUrl ?: "")
        )
    }

    private fun mapDocToCheckInRecord(doc: com.google.firebase.firestore.DocumentSnapshot): CheckInRecord? {
        return try {
            val fsTimestamp = doc.getTimestamp(FIELD_TIMESTAMP)?.toDate() ?: Date()
            val localDt = fsTimestamp.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
            CheckInRecord(
                id = 0, 
                firestoreId = doc.id, 
                studentId = doc.getString(FIELD_STUDENT_ID) ?: "",
                name = doc.getString("name") ?: "Unknown",
                timestamp = localDt,
                faceId = 0, 
                status = doc.getString("status") ?: "PRESENT",
                note = doc.getString("note") ?: "",
                className = doc.getString(FIELD_CLASS_NAME),
                gradeName = doc.getString("gradeName")
            )
        } catch (e: Exception) {
            null
        }
    }
}