package com.example.crashcourse.db// 🚀 FIXED: Now matches ViewModel imports

import android.content.Context
import android.util.Log
import com.example.crashcourse.db.AppDatabase
import com.example.crashcourse.db.FaceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 🧠 Azura Tech Face Cache
 * Acts as the "RAM Memory" for the AI Scanner.
 * Ensures the C++ engine has instant access to face embeddings.
 */
object FaceCache {
    private const val TAG = "FaceCache"
    
    // RAM Cache - Volatile ensures all threads see the latest update immediately
    @Volatile
    private var faceList: List<FaceEntity> = emptyList()

    /**
     * Returns the current list of faces stored in RAM.
     */
    fun getFaces(): List<FaceEntity> {
        return faceList
    }

    /**
     * 🛡️ CLEAR CACHE
     * Must be called during logout to prevent data leaking between schools.
     */
    fun clear() {
        faceList = emptyList()
        Log.d(TAG, "FaceCache cleared for security.")
    }

    /**
     * 🔄 REFRESH (Suspend)
     * Pulls data from Room DB into RAM. Call this after any Bulk Registration.
     */
    suspend fun refresh(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.getInstance(context)
                // 🚀 Load all faces currently in the local Room DB
                val updatedList = db.faceDao().getAllFaces() 
                faceList = updatedList
                Log.d(TAG, "FaceCache refreshed: ${faceList.size} faces loaded into RAM.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh FaceCache", e)
            }
        }
    }
}