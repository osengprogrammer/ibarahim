package com.example.crashcourse.firestore.student

import android.util.Log
import com.example.crashcourse.db.FaceEntity
import com.example.crashcourse.firestore.core.FirestorePaths
import com.example.crashcourse.firestore.core.FirestoreCore
import com.example.crashcourse.utils.Constants
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * 👥 FirestoreStudent (V.10.25 - Final Guarded Identity)
 * Authority utama data siswa Azura Attendance.
 * Memastikan schoolId adalah identitas mutlak yang tidak boleh kosong.
 */
object FirestoreStudent {

    private const val TAG = "FirestoreStudent"
    private val db = FirestoreCore.db

    // ==========================================
    // 🔍 1. READ OPERATIONS (Sync)
    // ==========================================

    /**
     * 🔄 SMART SYNC STUDENTS
     * Menarik data biometrik secara Global (Open Set) untuk satu sekolah.
     */
    suspend fun fetchSmartSyncStudents(
        schoolId: String,
        assignedClasses: List<String>,
        role: String,
        lastSync: Long
    ): List<FaceEntity> {
        return try {
            if (schoolId.isBlank()) {
                Log.e(TAG, "⚠️ Sync aborted: schoolId parameter is blank!")
                return emptyList()
            }

            // 1. Base Query: Isolasi data berdasarkan Sekolah (Multi-Tenant)
            var query: Query = db.collection(FirestorePaths.STUDENTS)
                .whereEqualTo("schoolId", schoolId)

            // 2. Incremental Filter: Hanya ambil data yang berubah sejak sync terakhir
            if (lastSync > 0) {
                query = query.whereGreaterThan("timestamp", lastSync)
            }

            Log.d(TAG, "📡 Syncing Cloud -> Local untuk School ID: $schoolId")

            val snapshot = query.get().await()
            val list = snapshot.documents.mapNotNull { doc ->
                // Mengirim schoolId sebagai fallback jika data di cloud corrupt
                mapStudentDocument(doc.data, schoolId)
            }

            Log.d(TAG, "✅ Berhasil menarik ${list.size} siswa dari Firestore.")
            list

        } catch (e: Exception) {
            Log.e(TAG, "❌ fetchSmartSyncStudents failed: ${e.message}")
            emptyList()
        }
    }

    // ==========================================
    // ✍️ 2. WRITE / DELETE OPERATIONS
    // ==========================================

    /**
     * 🔥 UPLOAD STUDENT (Guarded Version)
     * Memastikan data biometrik dan identitas sekolah terstempel dengan benar.
     */
    suspend fun uploadStudent(face: FaceEntity) {
        try {
            // Validasi Keras: Jangan biarkan data tanpa schoolId naik ke Cloud
            if (face.schoolId.isBlank()) {
                throw IllegalArgumentException("CRITICAL: Mencoba upload siswa tanpa schoolId!")
            }

            // Konversi FloatArray ke List<Double> (Firestore Standard)
            val embeddingList = face.embedding.map { it.toDouble() }

            val data = hashMapOf(
                "studentId" to face.studentId,
                "schoolId" to face.schoolId, // Identitas Sekolah
                "name" to face.name,
                "embedding" to embeddingList,
                "enrolledClasses" to face.enrolledClasses,
                "photoUrl" to face.photoUrl,
                "grade" to face.grade,
                "subClass" to face.subClass,
                "timestamp" to System.currentTimeMillis()
            )

            db.collection(FirestorePaths.STUDENTS)
                .document(face.studentId)
                .set(data, SetOptions.merge())
                .await()

            Log.d(TAG, "✅ Student ${face.name} berhasil di-upload dengan ID Sekolah: ${face.schoolId}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Gagal uploadStudent: ${e.message}")
            throw e
        }
    }

    /**
     * 🗑️ DELETE STUDENT
     */
    suspend fun deleteStudent(studentId: String) {
        try {
            db.collection(FirestorePaths.STUDENTS)
                .document(studentId)
                .delete()
                .await()
            Log.d(TAG, "🗑️ Document $studentId dihapus dari Cloud.")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Gagal deleteStudent: ${e.message}")
            throw e
        }
    }

    // ==========================================
    // 🛠️ 3. PRIVATE HELPERS (Mapping Logic)
    // ==========================================

    /**
     * Mengubah data Map mentah dari Firestore menjadi objek FaceEntity.
     */
    private fun mapStudentDocument(
        data: Map<String, Any>?,
        schoolIdFallback: String
    ): FaceEntity? {
        return try {
            if (data == null) return null

            // 📐 Konversi Embedding: Double List (Cloud) -> FloatArray (AI Engine)
            val rawEmbedding = data["embedding"] as? List<*>
            val embedding = rawEmbedding?.mapNotNull { (it as? Number)?.toFloat() }?.toFloatArray()

            if (embedding == null || embedding.isEmpty()) {
                Log.w(TAG, "⚠️ Data biometrik rusak untuk student: ${data["name"]}")
                return null
            }

            // 📚 Konversi Array: Firestore List -> Kotlin List<String>
            val enrolledClasses = (data["enrolledClasses"] as? List<*>)
                ?.mapNotNull { it?.toString() }
                ?: emptyList()

            // 🔥 LOGIKA PENYELAMAT: Ambil schoolId dari cloud, jika null pakai fallback dari session.
            val finalSchoolId = data["schoolId"]?.toString() ?: schoolIdFallback

            if (finalSchoolId.isBlank()) {
                Log.e(TAG, "❌ Mapping Error: schoolId tidak ditemukan bahkan di fallback!")
                return null
            }

            FaceEntity(
                studentId = data["studentId"]?.toString() ?: "",
                schoolId = finalSchoolId,
                name = data["name"]?.toString() ?: "Unknown Student",
                embedding = embedding,
                enrolledClasses = enrolledClasses,
                photoUrl = data["photoUrl"]?.toString(),
                grade = data["grade"]?.toString() ?: "",
                subClass = data["subClass"]?.toString() ?: "",
                timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Mapping Error: ${e.message}")
            null
        }
    }
}