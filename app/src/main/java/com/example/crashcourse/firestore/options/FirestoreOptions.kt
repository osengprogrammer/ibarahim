package com.example.crashcourse.firestore.options

import android.util.Log
import com.example.crashcourse.firestore.core.FirestoreCore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * 🎛️ FirestoreOptions (FINAL - INCREMENTAL SYNC READY)
 * Dedicated repository for handling Master Data (6-Pillars) operations.
 * Optimized for high-scale environments (1,000+ schools).
 */
object FirestoreOptions {

    private const val TAG = "FirestoreOptions"
    private val db = FirestoreCore.db

    // ==========================================
    // 1️⃣ FETCH UPDATES (Incremental Sync)
    // ==========================================
    /**
     * Menarik data hanya yang berubah sejak timestamp tertentu.
     * Sangat menghemat kuota Read Firestore dan mempercepat Sync.
     */
    suspend fun fetchOptionsUpdates(collection: String, sinceTimestamp: Long): List<Map<String, Any>> {
        return try {
            val snapshot = db.collection(collection)
                .whereGreaterThan("updatedAt", sinceTimestamp)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                val data = doc.data?.toMutableMap() ?: return@mapNotNull null
                // Pastikan ID Integer tetap konsisten
                data["id"] = data["id"] ?: doc.id.toIntOrNull() ?: 0
                data
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching updates from $collection", e)
            emptyList()
        }
    }

    // ==========================================
    // 2️⃣ SAVE / UPDATE (Upsert with Timestamp)
    // ==========================================
    /**
     * Menyimpan data dengan stempel waktu 'updatedAt'.
     * Penting agar HP lain tahu bahwa ada perubahan data.
     */
    suspend fun saveOption(collection: String, id: Int, data: Map<String, Any>) {
        try {
            val finalData = data.toMutableMap()
            finalData["updatedAt"] = System.currentTimeMillis() // 🚀 Stempel waktu otomatis

            db.collection(collection)
                .document(id.toString())
                .set(finalData, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving to $collection", e)
            throw e
        }
    }

    // ==========================================
    // 3️⃣ DELETE
    // ==========================================
    suspend fun deleteOption(collection: String, id: Int) {
        try {
            db.collection(collection)
                .document(id.toString())
                .delete()
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting from $collection", e)
            throw e
        }
    }

    // ==========================================
    // 4️⃣ REALTIME LISTENER
    // ==========================================
    /**
     * Listener real-time agar admin satu layar bisa melihat perubahan 
     * yang dilakukan admin lain secara instan.
     */
    fun listenToOptions(
        collection: String,
        onUpdate: (List<Map<String, Any>>) -> Unit
    ): ListenerRegistration {
        return db.collection(collection)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed for $collection", e)
                    return@addSnapshotListener
                }

                val list = snapshot?.documents?.mapNotNull { doc ->
                    val data = doc.data?.toMutableMap() ?: return@mapNotNull null
                    data["id"] = data["id"] ?: doc.id.toIntOrNull() ?: 0
                    data
                } ?: emptyList()

                onUpdate(list)
            }
    }
}