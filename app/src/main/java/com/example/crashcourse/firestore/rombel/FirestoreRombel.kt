package com.example.crashcourse.firestore.rombel

import android.util.Log
import com.example.crashcourse.db.MasterClassRoom
import com.example.crashcourse.firestore.core.FirestoreCore
import com.example.crashcourse.firestore.core.FirestorePaths
import com.example.crashcourse.utils.Constants
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * 🏗️ FirestoreRombel (V.10.13 - Refactored)
 * Database layer untuk sinkronisasi Unit/Rombel ke Cloud.
 * Menggunakan Composite ID (schoolId_classId) untuk isolasi data Multi-Tenant.
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
    suspend fun fetchMasterClasses(schoolId: String): List<MasterClassRoom> {
        return try {
            if (schoolId.isBlank()) return emptyList()

            val snapshot = db.collection(FirestorePaths.MASTER_CLASSES)
                .whereEqualTo("schoolId", schoolId) // ✅ Ganti dari sekolahId
                .get()
                .await()

            Log.d(TAG, "📥 Fetching Rombel for $schoolId: ${snapshot.size()} items found.")

            snapshot.documents.mapNotNull { doc ->
                try {
                    // Mapping dari Firestore Document ke Room Entity
                    MasterClassRoom(
                        classId = doc.getLong("classId")?.toInt() ?: 0,
                        schoolId = doc.getString("schoolId") ?: schoolId, // ✅ Consistent naming
                        className = doc.getString("className") ?: "Unit Tanpa Nama",
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
            // Menggunakan data.schoolId (Pastikan property di MasterClassRoom sudah di-rename)
            val docId = "${data.schoolId}_${data.classId}"

            val firestoreData = hashMapOf(
                "classId" to data.classId,
                "schoolId" to data.schoolId, // ✅ Sync dengan Entity
                "className" to data.className,
                "gradeId" to data.gradeId,
                "classOptionId" to data.classOptionId,
                "programId" to data.programId,
                "subClassId" to data.subClassId,
                "subGradeId" to data.subGradeId,
                "roleId" to data.roleId,
                "updatedAt" to System.currentTimeMillis()
            )

            db.collection(FirestorePaths.MASTER_CLASSES)
                .document(docId)
                .set(firestoreData, SetOptions.merge())
                .await()

            Log.d(TAG, "✅ Rombel Synced: $docId (${data.className})")
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
    suspend fun deleteMasterClass(schoolId: String, classId: Int) {
        try {
            val docId = "${schoolId}_${classId}"
            
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