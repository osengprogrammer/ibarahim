package com.example.crashcourse.db

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * 🚀 OPTIMIZED FACE CACHE
 * Menyimpan seluruh data wajah di RAM agar pengenalan wajah berjalan realtime (tanpa lag database).
 */
object FaceCache {
    private const val TAG = "FaceCache"
    
    // 🔒 Mutex untuk mencegah crash saat akses bersamaan (Thread Safety)
    private val mutex = Mutex()
    
    // 💾 Simpan Entity lengkap, bukan cuma Nama & Embedding
    // Agar kita punya akses ke StudentID, Kelas, dll tanpa query ulang.
    private var cachedFaces: List<FaceEntity> = emptyList()

    /**
     * Memastikan cache terisi. Jika kosong, tarik dari Database.
     */
    private suspend fun ensureCacheLoaded(context: Context) {
        if (cachedFaces.isEmpty()) {
            mutex.withLock {
                // Double check (jika thread lain sudah mengisinya saat kita antri lock)
                if (cachedFaces.isEmpty()) {
                    Log.d(TAG, "📥 Cache kosong. Memuat dari Database...")
                    cachedFaces = AppDatabase.getInstance(context).faceDao().getAllFaces()
                    Log.d(TAG, "✅ Cache terisi: ${cachedFaces.size} wajah loaded.")
                }
            }
        }
    }

    /**
     * Mengembalikan List Wajah (Format Lama: Nama, Embedding)
     * Digunakan jika C++ engine hanya butuh nama.
     */
    suspend fun load(context: Context): List<Pair<String, FloatArray>> = withContext(Dispatchers.IO) {
        ensureCacheLoaded(context)
        // Transformasi dari Cache Memory (Cepat, 0ms IO)
        cachedFaces.map { it.name to it.embedding }
    }

    /**
     * 🚀 Mengembalikan List Wajah Lengkap (ID, Nama, Embedding)
     * Ini yang harus dipakai untuk Recognition agar ID siswa tidak hilang.
     */
    suspend fun loadWithStudentIds(context: Context): List<Triple<String, String, FloatArray>> = withContext(Dispatchers.IO) {
        ensureCacheLoaded(context)
        // Transformasi dari Cache Memory (Cepat, 0ms IO)
        cachedFaces.map { 
            Triple(it.studentId, it.name, it.embedding) 
        }
    }

    /**
     * Mengembalikan raw list entity jika butuh data lain (Kelas, Grade, dll)
     */
    suspend fun getFullEntities(context: Context): List<FaceEntity> = withContext(Dispatchers.IO) {
        ensureCacheLoaded(context)
        cachedFaces
    }

    /**
     * Reset cache. Panggil ini setelah Register/Update/Delete wajah.
     */
    suspend fun clear() {
        mutex.withLock {
            Log.d(TAG, "🧹 Cache dibersihkan.")
            cachedFaces = emptyList()
        }
    }

    /**
     * Refresh paksa: Hapus cache lalu muat ulang.
     */
    suspend fun refresh(context: Context) {
        clear()
        ensureCacheLoaded(context)
    }
}