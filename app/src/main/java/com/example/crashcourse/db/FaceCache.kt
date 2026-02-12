package com.example.crashcourse.db

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 🧠 Azura Tech Face Cache (Optimized)
 */
object FaceCache {
    private const val TAG = "FaceCache"
    
    @Volatile
    private var faceList: List<FaceEntity> = emptyList()

    fun getFaces(): List<FaceEntity> = faceList

    fun clear() {
        faceList = emptyList()
        Log.d(TAG, "FaceCache cleared.")
    }

    /**
     * Memastikan data tersedia di RAM. 
     * Bisa dipanggil di 'onCreate' layar kamera untuk antisipasi cache kosong.
     */
    suspend fun ensureLoaded(context: Context) {
        if (faceList.isEmpty()) refresh(context)
    }

    suspend fun refresh(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                // Pastikan akses DAO aman
                val dao = AppDatabase.getInstance(context).faceDao()
                val updatedList = dao.getAllFaces() 
                
                faceList = updatedList
                Log.d(TAG, "RAM Cache updated: ${faceList.size} identities ready for AI scan.")
            } catch (e: Exception) {
                Log.e(TAG, "Refresh Error", e)
            }
        }
    }
}