package com.example.crashcourse.firestore.options

import android.util.Log
import com.example.crashcourse.firestore.core.FirestoreCore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * 🎛️ FirestoreOptions
 * Dedicated repository for handling Master Data (6-Pillars) operations.
 */
object FirestoreOptions {

    private const val TAG = "FirestoreOptions"
    private val db = FirestoreCore.db

    // ==========================================
    // 1️⃣ FETCH ONE-SHOT (Manual Sync)
    // ==========================================
    suspend fun fetchOptionsOnce(collection: String): List<Map<String, Any>> {
        return try {
            val snapshot = db.collection(collection).get().await()
            snapshot.documents.mapNotNull { doc ->
                val data = doc.data?.toMutableMap() ?: return@mapNotNull null
                // Ensure ID exists in the map, fallback to docId if missing
                data["id"] = data["id"] ?: doc.id.toIntOrNull() ?: 0
                data
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching $collection", e)
            emptyList()
        }
    }

    // ==========================================
    // 2️⃣ SAVE / UPDATE (Upsert)
    // ==========================================
    suspend fun saveOption(collection: String, id: Int, data: Map<String, Any>) {
        try {
            db.collection(collection)
                .document(id.toString())
                .set(data, SetOptions.merge())
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