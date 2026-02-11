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
 * Dedicated repository for syncing Unit Rakitan (MasterClass) between Room and Firestore.
 */
object FirestoreRombel {

    private const val TAG = "FirestoreRombel"
    private val db = FirestoreCore.db

    // ==========================================
    // 1️⃣ FETCH (DOWNLOAD)
    // ==========================================
    suspend fun fetchMasterClasses(sekolahId: String): List<MasterClassRoom> {
        return try {
            db.collection(FirestorePaths.MASTER_CLASSES)
                .whereEqualTo(Constants.KEY_SEKOLAH_ID, sekolahId)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    try {
                        MasterClassRoom(
                            classId = doc.getLong(Constants.KEY_ID)?.toInt() ?: 0,
                            sekolahId = doc.getString(Constants.KEY_SEKOLAH_ID) ?: sekolahId,
                            className = doc.getString(Constants.PILLAR_CLASS) ?: "",
                            gradeId = doc.getLong("gradeId")?.toInt() ?: 0,
                            classOptionId = doc.getLong("classOptionId")?.toInt() ?: 0,
                            programId = doc.getLong("programId")?.toInt() ?: 0,
                            subClassId = doc.getLong("subClassId")?.toInt() ?: 0,
                            subGradeId = doc.getLong("subGradeId")?.toInt() ?: 0,
                            roleId = doc.getLong("roleId")?.toInt() ?: 0
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error mapping doc ${doc.id}", e)
                        null
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "❌ fetchMasterClasses failed", e)
            emptyList()
        }
    }

    // ==========================================
    // 2️⃣ SAVE (UPSERT)
    // ==========================================
    suspend fun saveMasterClass(data: MasterClassRoom) {
        try {
            // Use classId as Document ID to ensure 1-to-1 sync
            val docId = data.classId.toString()

            val firestoreData = mapOf(
                Constants.KEY_ID to data.classId,
                Constants.KEY_SEKOLAH_ID to data.sekolahId,
                Constants.PILLAR_CLASS to data.className,
                "gradeId" to data.gradeId,
                "classOptionId" to data.classOptionId,
                "programId" to data.programId,
                "subClassId" to data.subClassId,
                "subGradeId" to data.subGradeId,
                "roleId" to data.roleId
            )

            db.collection(FirestorePaths.MASTER_CLASSES)
                .document(docId)
                .set(firestoreData, SetOptions.merge())
                .await()

        } catch (e: Exception) {
            Log.e(TAG, "❌ saveMasterClass failed", e)
            throw e
        }
    }

    // ==========================================
    // 3️⃣ DELETE
    // ==========================================
    suspend fun deleteMasterClass(classId: Int) {
        try {
            db.collection(FirestorePaths.MASTER_CLASSES)
                .document(classId.toString())
                .delete()
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "❌ deleteMasterClass failed", e)
            throw e
        }
    }
}