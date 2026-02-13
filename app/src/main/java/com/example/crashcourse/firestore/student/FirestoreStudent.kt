package com.example.crashcourse.firestore.student

import android.util.Log
import com.example.crashcourse.db.FaceEntity
import com.example.crashcourse.firestore.core.FirestorePaths
import com.example.crashcourse.firestore.core.FirestoreCore
import com.example.crashcourse.utils.Constants
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * 👥 FirestoreStudent (MANY-TO-MANY SUPPORTED)
 * Source of truth untuk operasi data Siswa di Firestore.
 */
object FirestoreStudent {

    private const val TAG = "FirestoreStudent"
    private val db = FirestoreCore.db

    /**
     * 🔄 SMART SYNC STUDENTS
     * Menarik data dari Firestore dengan filter keamanan Many-to-Many.
     */
    suspend fun fetchSmartSyncStudents(
        sekolahId: String,
        assignedClasses: List<String>,
        role: String,
        lastSync: Long
    ): List<FaceEntity> {
        return try {
            if (sekolahId.isBlank()) return emptyList()

            // 1. Base Query: Harus sesuai Sekolah ID
            var query: Query = db.collection(FirestorePaths.STUDENTS)
                .whereEqualTo(Constants.KEY_SEKOLAH_ID, sekolahId)

            // 2. Filter Security Many-to-Many
            // Jika bukan ADMIN, filter hanya rombel yang diampu (assignedClasses)
            if (role != Constants.ROLE_ADMIN) {
                if (assignedClasses.isNotEmpty()) {
                    // Hanya tarik siswa yang Rombel-nya ada di daftar assignedClasses Guru
                    query = query.whereArrayContainsAny(Constants.PILLAR_CLASS, assignedClasses)
                } else {
                    // 🚩 PERINGATAN: Jika Guru tidak punya kelas, jangan tarik data (Security)
                    Log.w(TAG, "⚠️ Guru/User tidak memiliki assignedClasses. Sync dibatalkan.")
                    return emptyList()
                }
            }
            
            // 3. Filter Incremental (Hanya ambil yang terbaru sejak sync terakhir)
            if (lastSync > 0) {
                query = query.whereGreaterThan(Constants.KEY_TIMESTAMP, lastSync)
            }

            Log.d(TAG, "📡 Fetching students for school: $sekolahId with classes: $assignedClasses")

            val snapshot = query.get().await()
            val list = snapshot.documents.mapNotNull { doc ->
                mapStudentDocument(doc.data, sekolahId)
            }
            
            Log.d(TAG, "✅ Berhasil menarik ${list.size} siswa dari Cloud")
            list

        } catch (e: Exception) {
            Log.e(TAG, "❌ fetchSmartSyncStudents failed: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * 🎯 DOWNLOAD STUDENTS BY ONE ROMBEL
     */
    suspend fun fetchStudentsByRombel(
        sekolahId: String,
        className: String
    ): List<FaceEntity> {
        return try {
            db.collection(FirestorePaths.STUDENTS)
                .whereEqualTo(Constants.KEY_SEKOLAH_ID, sekolahId)
                .whereArrayContains(Constants.PILLAR_CLASS, className)
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
            // Konversi String CSV (Room) ke Array (Firestore)
            val classList = face.className.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            val data = mapOf(
                Constants.FIELD_STUDENT_ID to face.studentId,
                Constants.KEY_SEKOLAH_ID to face.sekolahId,
                Constants.KEY_NAME to face.name,
                Constants.FIELD_EMBEDDING to face.embedding.map { it.toDouble() },
                Constants.KEY_TIMESTAMP to System.currentTimeMillis(),
                Constants.FIELD_PHOTO_URL to face.photoUrl,
                Constants.PILLAR_CLASS to classList, // Disimpan sebagai Array
                Constants.FIELD_ROLE to face.role,
                Constants.PILLAR_GRADE to face.grade,
                Constants.PILLAR_SUB_GRADE to face.subGrade,
                Constants.PILLAR_PROGRAM to face.program,
                Constants.PILLAR_SUB_CLASS to face.subClass
            )

            db.collection(FirestorePaths.STUDENTS)
                .document(face.studentId)
                .set(data)
                .await()
                
            Log.d(TAG, "✅ Student ${face.name} uploaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ uploadStudent failed", e)
            throw e
        }
    }

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

    private fun mapStudentDocument(
        data: Map<String, Any>?,
        sekolahIdFallback: String
    ): FaceEntity? {
        return try {
            if (data == null) return null

            // Mapping embedding (Double dari Firestore -> FloatArray untuk AI)
            val embedding = (data[Constants.FIELD_EMBEDDING] as? List<*>)
                ?.mapNotNull { (it as? Number)?.toFloat() }
                ?.toFloatArray()
                ?: return null

            // Konversi Array (Firestore) ke String CSV (Room)
            val rawClass = data[Constants.PILLAR_CLASS]
            val classNameString = when (rawClass) {
                is List<*> -> rawClass.joinToString(", ")
                else -> rawClass?.toString() ?: ""
            }

            FaceEntity(
                studentId = data[Constants.FIELD_STUDENT_ID]?.toString() ?: "",
                sekolahId = data[Constants.KEY_SEKOLAH_ID]?.toString() ?: sekolahIdFallback,
                name = data[Constants.KEY_NAME]?.toString() ?: "Unknown",
                photoUrl = data[Constants.FIELD_PHOTO_URL]?.toString(),
                embedding = embedding,
                className = classNameString,
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