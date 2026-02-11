package com.example.crashcourse.firestore.student

import android.util.Log
import com.example.crashcourse.db.FaceEntity
import com.example.crashcourse.firestore.core.FirestorePaths
import com.example.crashcourse.firestore.core.FirestoreCore
import com.example.crashcourse.utils.Constants
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * 👥 FirestoreStudent (FINAL)
 * Single source of truth for Student-related Firestore operations.
 * Handles Smart Sync, Upload, Delete, and Fetching.
 */
object FirestoreStudent {

    private const val TAG = "FirestoreStudent"
    private val db = FirestoreCore.db

    /**
     * 🔄 SMART SYNC STUDENTS
     * Mengambil data siswa dengan 3 Layer Filter:
     * 1. Sekolah ID (Wajib)
     * 2. Role/Class Security (Hanya kelas yang diizinkan untuk guru tsb)
     * 3. Incremental Sync (Hanya data yang berubah sejak lastSync)
     * * (Sebelumnya bernama fetchStudentsForUser, kini diganti agar konsisten dengan SyncViewModel)
     */
    suspend fun fetchSmartSyncStudents(
        sekolahId: String,
        assignedClasses: List<String>,
        role: String,
        lastSync: Long
    ): List<FaceEntity> {
        return try {
            // 1. Base Query: Sekolah ID
            var query: Query = db.collection(FirestorePaths.STUDENTS)
                .whereEqualTo(Constants.KEY_SEKOLAH_ID, sekolahId)

            // 2. Filter Incremental (Time-based)
            if (lastSync > 0) {
                query = query.whereGreaterThan(Constants.KEY_TIMESTAMP, lastSync)
            }

            // 3. Filter Security (Jika bukan Admin, batasi kelas)
            if (role != Constants.ROLE_ADMIN && assignedClasses.isNotEmpty()) {
                // Catatan: Firestore membatasi 'whereIn' maksimal 10 item.
                query = query.whereIn(Constants.PILLAR_CLASS, assignedClasses)
            }

            query.get().await().documents.mapNotNull { doc ->
                mapStudentDocument(doc.data, sekolahId)
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ fetchSmartSyncStudents failed", e)
            emptyList()
        }
    }

    /**
     * 🎯 DOWNLOAD STUDENTS BY ONE ROMBEL
     * Digunakan untuk fitur spesifik melihat 1 kelas full.
     */
    suspend fun fetchStudentsByRombel(
        sekolahId: String,
        className: String
    ): List<FaceEntity> {
        return try {
            db.collection(FirestorePaths.STUDENTS)
                .whereEqualTo(Constants.KEY_SEKOLAH_ID, sekolahId)
                .whereEqualTo(Constants.PILLAR_CLASS, className)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    mapStudentDocument(doc.data, sekolahId)
                }
        } catch (e: Exception) {
            Log.e(TAG, "❌ fetchStudentsByRombel failed", e)
            emptyList()
        }
    }

    // ==========================================
    // WRITE / DELETE
    // ==========================================

    suspend fun uploadStudent(face: FaceEntity) {
        try {
            db.collection(FirestorePaths.STUDENTS)
                .document(face.studentId)
                .set(
                    mapOf(
                        Constants.FIELD_STUDENT_ID to face.studentId,
                        Constants.KEY_SEKOLAH_ID to face.sekolahId,
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
                )
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "❌ uploadStudent failed", e)
            throw e
        }
    }

    /**
     * Alias untuk uploadStudent (digunakan oleh EditUserScreen)
     */
    suspend fun updateFaceWithPhoto(face: FaceEntity) {
        uploadStudent(face)
    }

    suspend fun deleteStudent(studentId: String) {
        try {
            db.collection(FirestorePaths.STUDENTS)
                .document(studentId)
                .delete()
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "❌ deleteStudent failed", e)
            throw e
        }
    }

    // ==========================================
    // PRIVATE HELPERS
    // ==========================================

    /**
     * 🧬 Mapper Firestore → FaceEntity
     */
    private fun mapStudentDocument(
        data: Map<String, Any>?,
        sekolahIdFallback: String
    ): FaceEntity? {
        return try {
            if (data == null) return null

            val embedding = (data[Constants.FIELD_EMBEDDING] as? List<*>)
                ?.mapNotNull { (it as? Number)?.toFloat() }
                ?.toFloatArray()
                ?: return null

            FaceEntity(
                studentId = data[Constants.FIELD_STUDENT_ID]?.toString() ?: "",
                sekolahId = data[Constants.KEY_SEKOLAH_ID]?.toString() ?: sekolahIdFallback,
                name = data[Constants.KEY_NAME]?.toString() ?: "Unknown",
                photoUrl = data[Constants.FIELD_PHOTO_URL]?.toString(),
                embedding = embedding,
                className = data[Constants.PILLAR_CLASS]?.toString() ?: "",
                role = data[Constants.FIELD_ROLE]?.toString() ?: Constants.ROLE_USER,
                grade = data[Constants.PILLAR_GRADE]?.toString() ?: "",
                subGrade = data[Constants.PILLAR_SUB_GRADE]?.toString() ?: "",
                program = data[Constants.PILLAR_PROGRAM]?.toString() ?: "",
                subClass = data[Constants.PILLAR_SUB_CLASS]?.toString() ?: "",
                timestamp = (data[Constants.KEY_TIMESTAMP] as? Number)?.toLong()
                    ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ mapStudentDocument error", e)
            null
        }
    }
}