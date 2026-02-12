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
 * Single source of truth for Student-related Firestore operations.
 * Diperbarui untuk mendukung sinkronisasi berbasis Array (Multiple Rombel).
 */
object FirestoreStudent {

    private const val TAG = "FirestoreStudent"
    private val db = FirestoreCore.db

    /**
     * 🔄 SMART SYNC STUDENTS
     * Menggunakan 'whereArrayContainsAny' agar Guru/Dosen bisa menarik data 
     * mahasiswa berdasarkan daftar mata kuliah yang mereka ampu (Many-to-Many).
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

            // 3. Filter Security Many-to-Many
            // Jika bukan Admin, tarik mahasiswa yang memiliki salah satu matkul yang diampu dosen
            if (role != Constants.ROLE_ADMIN && assignedClasses.isNotEmpty()) {
                // Firestore membatasi array-contains-any maksimal 10 item.
                query = query.whereArrayContainsAny(Constants.PILLAR_CLASS, assignedClasses)
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
     */
    suspend fun fetchStudentsByRombel(
        sekolahId: String,
        className: String
    ): List<FaceEntity> {
        return try {
            // Gunakan arrayContains karena di Firestore field className sekarang berbentuk Array
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
            // 🔥 LOGIKA KONVERSI CSV KE ARRAY 🔥
            // Memecah "Math, English" dari Room menjadi ["Math", "English"] untuk Firestore
            val classList = face.className.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

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
                        // SIMPAN SEBAGAI ARRAY (Penting untuk filter query)
                        Constants.PILLAR_CLASS to classList, 
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

            // 🔥 LOGIKA KONVERSI ARRAY KE CSV 🔥
            // Mengubah kembali ["Math", "English"] dari Firestore menjadi "Math, English" untuk Room
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