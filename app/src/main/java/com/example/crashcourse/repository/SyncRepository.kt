package com.example.crashcourse.repository

import android.app.Application
import android.content.Context
import android.util.Log
import com.example.crashcourse.db.AppDatabase
import com.example.crashcourse.db.FaceCache
import com.example.crashcourse.firestore.student.FirestoreStudent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 🔄 SyncRepository (V.10.6 - Stainless Steel Bulletproof)
 * Mengelola sinkronisasi data siswa secara cerdas (Incremental & Full Sync).
 * Dilengkapi dengan Safety Net untuk mencegah data hilang (Data Loss Prevention).
 */
class SyncRepository(application: Application) {
    private val db = AppDatabase.getInstance(application)
    private val faceDao = db.faceDao()
    private val userDao = db.userDao()
    private val prefs = application.getSharedPreferences("azura_sync_prefs", Context.MODE_PRIVATE)
    private val context = application

    /**
     * 🔥 THE BRAIN: Smart Sync Logic
     * Menjalankan sinkronisasi bertahap (Incremental) agar hemat kuota.
     */
    suspend fun performSmartSync(forceFullSync: Boolean): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // 1. Ambil User Context
            val currentUser = userDao.getCurrentUser() 
                ?: return@withContext Result.failure(Exception("Sesi login tidak ditemukan. Harap login ulang."))
            
            val sid = currentUser.schoolId ?: ""
            
            if (sid.isBlank()) return@withContext Result.failure(Exception("ID Sekolah kosong. Pilih sekolah terlebih dahulu."))

            // 2. 🛡️ THE SAFETY NET (Mencegah Data Hilang)
            // Cek apakah database fisik di HP kosong.
            val localCount = faceDao.getStudentCount(sid)
            
            // Jika dipaksa Full Sync ATAU database lokal ternyata kosong (0), paksa lastSync = 0
            val lastSync = if (forceFullSync || localCount == 0) {
                Log.w("SyncRepo", "⚠️ Full Sync dipicu! (Force=$forceFullSync, LocalCount=$localCount)")
                0L 
            } else {
                prefs.getLong("last_sync_$sid", 0L)
            }

            // 3. Download Data dari Cloud
            val cloudStudents = FirestoreStudent.fetchSmartSyncStudents(
                schoolId = sid, 
                assignedClasses = currentUser.assignedClasses,
                role = currentUser.role,
                lastSync = lastSync
            )

            if (cloudStudents.isEmpty()) {
                Log.d("SyncRepo", "✅ Sync Complete: Data lokal sudah sinkron dengan Cloud.")
                // Update timestamp juga di sini agar presisi
                prefs.edit().putLong("last_sync_$sid", System.currentTimeMillis()).apply()
                return@withContext Result.success(0)
            }

            // 4. Batch Save ke Room Database (Upsert: Update/Insert otomatis)
            faceDao.insertAll(cloudStudents)
            
            // 5. Simpan timestamp sinkronisasi terbaru
            // Catatan: System.currentTimeMillis() memakai waktu HP. 
            prefs.edit().putLong("last_sync_$sid", System.currentTimeMillis()).apply()
            
            // 6. 🧠 REFRESH CACHE AI
            // Wajib agar wajah baru langsung bisa di-scan tanpa restart aplikasi.
            FaceCache.refresh(context)

            Log.d("SyncRepo", "✅ Smart Sync Success: ${cloudStudents.size} data diperbarui di HP.")
            Result.success(cloudStudents.size)

        } catch (e: Exception) {
            Log.e("SyncRepo", "❌ Smart Sync Error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Menghapus riwayat sinkronisasi (Memicu Full Sync pada klik berikutnya)
     * Dipanggil saat user Logout atau Ganti Sekolah.
     */
    fun clearHistory(schoolId: String) {
        prefs.edit().remove("last_sync_$schoolId").apply()
        Log.d("SyncRepo", "🗑️ Riwayat sinkronisasi untuk sekolah $schoolId dihapus.")
    }
}