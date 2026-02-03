package com.example.crashcourse.utils

import android.util.Log
import com.example.crashcourse.db.CheckInRecord
import com.example.crashcourse.db.FaceEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

object FirestoreHelper {
    private const val TAG = "FirestoreHelper"
    
    private val db by lazy { FirebaseFirestore.getInstance() }

    // ==========================================
    // 1. STUDENT DATA SYNC (WITH TEACHER SCOPE)
    // ==========================================

    /**
     * SYNC DOWN (SCOPED): Downloads students based on Teacher's assigned classes.
     * If user is ADMIN or has no assigned classes, it downloads everything.
     */
    suspend fun getScopedStudentsFromFirestore(uid: String): List<FaceEntity> {
        return try {
            // 1. Get User Profile to check scope
            val userDoc = db.collection("users").document(uid).get().await()
            val role = userDoc.getString("role") ?: "USER"
            val assignedClasses = userDoc.get("assigned_classes") as? List<String> ?: emptyList()

            Log.d(TAG, "Fetching scope for UID: $uid | Role: $role | Classes: $assignedClasses")

            // 2. Define Query based on Scope
            val query: Query = if (role == "ADMIN" || assignedClasses.isEmpty()) {
                db.collection("students") // Full Access
            } else {
                // Scoped Access: Only students in these classes
                db.collection("students").whereIn("className", assignedClasses)
            }

            val snapshot = query.get().await()
            val students = mutableListOf<FaceEntity>()

            for (doc in snapshot.documents) {
                try {
                    val embeddingList = doc.get("embedding") as? List<Double>
                    val embeddingFloatArray = embeddingList?.map { it.toFloat() }?.toFloatArray()

                    if (embeddingFloatArray != null) {
                        students.add(mapDocToFaceEntity(doc, embeddingFloatArray))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing doc ${doc.id}: ${e.message}")
                }
            }
            Log.d(TAG, "Scope Sync Complete. Downloaded ${students.size} students.")
            students
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download scoped students", e)
            emptyList()
        }
    }

    /**
     * Helper to map Firestore Document to FaceEntity
     */
    private fun mapDocToFaceEntity(doc: com.google.firebase.firestore.DocumentSnapshot, embedding: FloatArray): FaceEntity {
        return FaceEntity(
            studentId = doc.getString("studentId") ?: "",
            name = doc.getString("name") ?: "Unknown",
            photoUrl = null, 
            embedding = embedding,
            className = doc.getString("className") ?: "",
            subClass = doc.getString("subClass") ?: "",
            grade = doc.getString("grade") ?: "",
            subGrade = doc.getString("subGrade") ?: "",
            program = doc.getString("program") ?: "",
            role = doc.getString("role") ?: "",
            classId = (doc.getLong("classId") ?: 0).toInt().takeIf { it != 0 },
            subClassId = (doc.getLong("subClassId") ?: 0).toInt().takeIf { it != 0 },
            gradeId = (doc.getLong("gradeId") ?: 0).toInt().takeIf { it != 0 },
            subGradeId = (doc.getLong("subGradeId") ?: 0).toInt().takeIf { it != 0 },
            programId = (doc.getLong("programId") ?: 0).toInt().takeIf { it != 0 },
            roleId = (doc.getLong("roleId") ?: 0).toInt().takeIf { it != 0 },
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * SYNC UP: Uploads a local FaceEntity to Firestore
     */
    suspend fun syncStudentToFirestore(face: FaceEntity): Boolean {
        return try {
            val embeddingList = face.embedding.map { it.toDouble() }
            val studentData = hashMapOf(
                "studentId" to face.studentId,
                "name" to face.name,
                "className" to face.className,
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
                "embedding" to embeddingList,
                "last_updated" to System.currentTimeMillis(),
                "photoUrl" to "" 
            )

            db.collection("students") 
                .document(face.studentId)
                .set(studentData, SetOptions.merge())
                .await()

            Log.d(TAG, "Synced Student to Firestore: ${face.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync student ${face.name}: ${e.message}")
            false
        }
    }

    // ==========================================
    // 2. ATTENDANCE LOG SYNC (REAL-TIME DATA)
    // ==========================================

    suspend fun syncAttendanceLog(record: CheckInRecord) {
        try {
            val logData = hashMapOf(
                "name" to record.name,
                "timestamp" to record.timestamp.toString(),
                "status" to record.status,
                "note" to (record.note ?: ""),
                "className" to record.className,
                "gradeName" to record.gradeName,
                "classId" to record.classId,
                "gradeId" to record.gradeId,
                "synced_at" to System.currentTimeMillis()
            )

            db.collection("attendance_logs").add(logData).await()
            Log.d(TAG, "✅ Attendance Log Synced: ${record.name}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to sync attendance log: ${e.message}")
        }
    }
}