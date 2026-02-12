package com.example.crashcourse.firestore.rombel

import android.util.Log
import com.example.crashcourse.db.MasterClassRoom
import com.example.crashcourse.firestore.core.FirestoreCore
import com.example.crashcourse.firestore.core.FirestorePaths
import com.example.crashcourse.utils.Constants
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * 🏗️ FirestoreRombel
 * Database layer untuk sinkronisasi Unit Rakitan (Rombel) ke Cloud.
 * Menggunakan Composite ID (sekolahId_classId) untuk isolasi data yang sempurna.
 */
object FirestoreRombel {

    private const val TAG = "FirestoreRombel"
    private val db = FirestoreCore.db

    // ==========================================
    // 1️⃣ FETCH (Download Data Master Sekolah)
    // ==========================================
    /**
     * Mengambil semua daftar Rombel milik sekolah tertentu.
     */
    suspend fun fetchMasterClasses(sekolahId: String): List<MasterClassRoom> {
        return try {
            val snapshot = db.collection(FirestorePaths.MASTER_CLASSES)
                .whereEqualTo(Constants.KEY_SEKOLAH_ID, sekolahId)
                .get()
                .await()

            Log.d(TAG, "📥 Fetching Rombel for $sekolahId: ${snapshot.size()} items found.")

            snapshot.documents.mapNotNull { doc ->
                try {
                    // Safe parsing untuk menghindari crash jika tipe data di Firestore bergeser
                    MasterClassRoom(
                        classId = doc.getLong(Constants.KEY_ID)?.toInt() ?: 0,
                        sekolahId = doc.getString(Constants.KEY_SEKOLAH_ID) ?: sekolahId,
                        className = doc.getString(Constants.PILLAR_CLASS) ?: "Unit Tanpa Nama",
                        gradeId = doc.getLong("gradeId")?.toInt() ?: 0,
                        classOptionId = doc.getLong("classOptionId")?.toInt() ?: 0,
                        programId = doc.getLong("programId")?.toInt() ?: 0,
                        subClassId = doc.getLong("subClassId")?.toInt() ?: 0,
                        subGradeId = doc.getLong("subGradeId")?.toInt() ?: 0,
                        roleId = doc.getLong("roleId")?.toInt() ?: 0
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "⚠️ Error mapping doc ID: ${doc.id}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ fetchMasterClasses failed", e)
            emptyList()
        }
    }

    // ==========================================
    // 2️⃣ SAVE (Upsert dengan Composite ID)
    // ==========================================
    /**
     * Menyimpan atau memperbarui Unit Rakitan ke Cloud.
     */
    suspend fun saveMasterClass(data: MasterClassRoom) {
        try {
            // 🚀 COMPOSITE ID: Mencegah tabrakan ID '1' antara Sekolah A dan Sekolah B
            val docId = "${data.sekolahId}_${data.classId}"

            val firestoreData = hashMapOf(
                Constants.KEY_ID to data.classId,
                Constants.KEY_SEKOLAH_ID to data.sekolahId,
                Constants.PILLAR_CLASS to data.className,
                "gradeId" to data.gradeId,
                "classOptionId" to data.classOptionId,
                "programId" to data.programId,
                "subClassId" to data.subClassId,
                "subGradeId" to data.subGradeId,
                "roleId" to data.roleId,
                "updatedAt" to System.currentTimeMillis() // Penting untuk tracking perubahan
            )

            db.collection(FirestorePaths.MASTER_CLASSES)
                .document(docId)
                .set(firestoreData, SetOptions.merge())
                .await()

            Log.d(TAG, "✅ Rombel Synced: $docId ($${data.className})")
        } catch (e: Exception) {
            Log.e(TAG, "❌ saveMasterClass failed", e)
            throw e
        }
    }

    // ==========================================
    // 3️⃣ DELETE (Cleanup Cloud Data)
    // ==========================================
    /**
     * Menghapus Unit Rakitan dari Cloud berdasarkan Composite ID.
     */
    suspend fun deleteMasterClass(sekolahId: String, classId: Int) {
        try {
            val docId = "${sekolahId}_${classId}"
            
            db.collection(FirestorePaths.MASTER_CLASSES)
                .document(docId)
                .delete()
                .await()
                
            Log.d(TAG, "🗑️ MasterClass deleted from Cloud: $docId")
        } catch (e: Exception) {
            Log.e(TAG, "❌ deleteMasterClass failed", e)
            throw e
        }
    }
}