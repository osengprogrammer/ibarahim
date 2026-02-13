package com.example.crashcourse.repository

import android.app.Application
import android.content.Context
import com.example.crashcourse.db.AppDatabase
import com.example.crashcourse.db.FaceCache
import com.example.crashcourse.firestore.student.FirestoreStudent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncRepository(application: Application) {
    private val db = AppDatabase.getInstance(application)
    private val faceDao = db.faceDao()
    private val userDao = db.userDao()
    private val prefs = application.getSharedPreferences("azura_sync_prefs", Context.MODE_PRIVATE)
    private val context = application

    /**
     * 🔥 THE BRAIN: Smart Sync Logic
     * Menjalankan urutan sinkronisasi yang aman & efisien.
     */
    suspend fun performSmartSync(forceFullSync: Boolean): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // 1. Ambil User
            val currentUser = userDao.getCurrentUser() ?: return@withContext Result.failure(Exception("Sesi tidak ditemukan"))
            
            val sid = currentUser.sekolahId ?: ""
            
            // 2. Hitung Timestamp
            val lastSync = if (forceFullSync) 0L else prefs.getLong("last_sync_$sid", 0L)

            // 3. Download
            val cloudStudents = FirestoreStudent.fetchSmartSyncStudents(
                sekolahId = sid,
                assignedClasses = currentUser.assignedClasses,
                role = currentUser.role,
                lastSync = lastSync
            )

            if (cloudStudents.isEmpty()) return@withContext Result.success(0)

            // 4. Batch Save & Update Metadata
            faceDao.insertAll(cloudStudents)
            prefs.edit().putLong("last_sync_$sid", System.currentTimeMillis()).apply()
            
            // 5. Refresh Cache AI
            FaceCache.refresh(context)

            Result.success(cloudStudents.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun clearHistory(sekolahId: String) {
        prefs.edit().remove("last_sync_$sekolahId").apply()
    }
}