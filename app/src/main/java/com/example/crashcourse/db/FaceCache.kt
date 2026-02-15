package com.example.crashcourse.db

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * 🧠 Azura Tech Face Cache (V.12.5)
 * Partner utama RecognitionViewModel untuk akses biometrik latency 0ms.
 * Memastikan data di RAM selalu sinkron dengan Database Room.
 */
object FaceCache {
    private const val TAG = "FaceCache"
    private val mutex = Mutex()

    @Volatile
    private var faceList: List<FaceEntity> = emptyList()

    /**
     * 👁️ Ambil Galeri Wajah dari RAM.
     */
    fun getFaces(): List<FaceEntity> = faceList

    /**
     * 🧹 Bersihkan RAM saat Logout.
     */
    fun clear() {
        faceList = emptyList()
        Log.d(TAG, "FaceCache: RAM cleared.")
    }

    /**
     * 🛡️ Pastikan data ter-load sebelum scanner aktif.
     */
    suspend fun ensureLoaded(context: Context) {
        if (faceList.isEmpty()) {
            refresh(context)
        }
    }

    /**
     * 🔄 Sinkronisasi RAM dengan Room Database.
     */
    suspend fun refresh(context: Context) {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                try {
                    // ✅ SESUAIKAN: Jika di AppDatabase.kt namanya 'getInstance', ganti jadi getInstance(context)
                    // Jika namanya 'getDatabase', tetap gunakan getDatabase(context)
                    val database = AppDatabase.getInstance(context) 
                    val faceDao = database.faceDao()
                    
                    val smartList = faceDao.getMyStudentsForAI()
                    
                    if (smartList.isNotEmpty()) {
                        faceList = smartList
                        Log.i(TAG, "✅ CACHE SUCCESS: Loaded ${faceList.size} faces.")
                    } else {
                        Log.w(TAG, "⚠️ CACHE EMPTY: No data found in DB.")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ CACHE ERROR: Gagal memuat data biometrik", e)
                }
            }
        }
    }

    suspend fun forceRefresh(context: Context) {
        refresh(context)
    }

    fun isReady(): Boolean = faceList.isNotEmpty()
}