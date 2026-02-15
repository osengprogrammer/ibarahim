package com.example.crashcourse.repository

import android.app.Application
import android.util.Log
import com.example.crashcourse.db.AppDatabase
import com.example.crashcourse.db.UserEntity
import com.example.crashcourse.db.FaceCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext

/**
 * 🔐 UserRepository (V.10.20 - Unified Identity)
 * Mengelola sesi User (Guru/Admin) menggunakan identitas tunggal schoolId.
 * Terintegrasi dengan pembersihan data total saat Logout.
 */
class UserRepository(application: Application) {
    private val database = AppDatabase.getInstance(application)
    private val userDao = database.userDao()
    private val faceDao = database.faceDao()
    private val checkInDao = database.checkInRecordDao()

    companion object {
        private const val TAG = "UserRepository"
    }

    // --- 🔍 READ OPERATIONS ---

    suspend fun getCurrentUser(): UserEntity? = withContext(Dispatchers.IO) {
        userDao.getCurrentUser()
    }

    /**
     * Memantau perubahan User secara reaktif.
     */
    fun getCurrentUserFlow(): Flow<UserEntity?> {
        return userDao.getCurrentUserFlow().distinctUntilChanged()
    }

    /**
     * 🔥 FIXED: Menggunakan schoolId (Identitas Tunggal)
     * Tidak lagi menggunakan schoolId yang membingungkan.
     */
    suspend fun getSchoolId(): String? = withContext(Dispatchers.IO) {
        userDao.getCurrentUser()?.schoolId
    }

    // --- ✍️ WRITE OPERATIONS ---

    /**
     * Menyimpan sesi login user ke database lokal.
     */
    suspend fun saveUserSession(user: UserEntity) = withContext(Dispatchers.IO) {
        userDao.replaceCurrentUser(user)
    }

    /**
     * 🔥 FIXED: Update schoolId jika diperlukan (Misal ganti sekolah)
     */
    suspend fun updateSchoolId(uid: String, schoolId: String) = withContext(Dispatchers.IO) {
        userDao.updateSchoolId(uid, schoolId)
    }

    // --- 🗑️ CLEANUP OPERATIONS ---

    /**
     * 🛡️ PROSES LOGOUT TOTAL (Stainless Steel Cleanup)
     */
    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        try {
            // 1. Hapus Sesi User
            userDao.deleteAll()
            
            // 2. Hapus Data Wajah Siswa (RAM & Disk)
            faceDao.deleteAll()
            FaceCache.clear() 
            
            // 3. Hapus Riwayat Absensi Lokal
            checkInDao.deleteAll()
            
            Log.d(TAG, "✅ Logout Sukses: Semua data lokal dan cache dibersihkan.")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Logout Gagal: Terjadi kesalahan saat cleanup data.", e)
            throw e
        }
    }
}