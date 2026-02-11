package com.example.crashcourse.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crashcourse.db.AppDatabase
import com.example.crashcourse.db.FaceCache
import com.example.crashcourse.firestore.student.FirestoreStudent // ✅ NEW IMPORT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 📦 Azura Tech Sync State
 * Mengelola status UI selama proses sinkronisasi.
 */
sealed class SyncState {
    object Idle : SyncState()
    data class Loading(val message: String, val progress: Float = 0f) : SyncState()
    data class Success(val message: String) : SyncState()
    data class Error(val message: String) : SyncState()
}

class SyncViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getInstance(application)
    private val faceDao = database.faceDao()
    private val userDao = database.userDao()
    
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState = _syncState.asStateFlow()

    // Key untuk menyimpan waktu sync terakhir di SharedPreferences
    private val prefs = application.getSharedPreferences("azura_sync_prefs", Context.MODE_PRIVATE)

    /**
     * 🔥 SMART SYNC DOWN
     * Menarik data dari Cloud dengan 3 filter penghemat data.
     */
    fun syncStudentsDown(forceFullSync: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _syncState.value = SyncState.Loading("Mengidentifikasi Sesi...", 0.1f)

            try {
                // 1. Ambil Profil User Lokal
                val currentUser = userDao.getCurrentUser()
                if (currentUser == null) {
                    _syncState.value = SyncState.Error("Sesi tidak ditemukan. Silakan login ulang.")
                    return@launch
                }

                // 2. Tentukan Timestamp Sinkronisasi Terakhir
                val lastSyncTimestamp = if (forceFullSync) 0L else prefs.getLong("last_sync_${currentUser.sekolahId}", 0L)
                
                _syncState.value = SyncState.Loading("Menghubungkan ke Cloud...", 0.3f)

                // 3. Download dari Firestore via FirestoreStudent (✅ UPDATED)
                val cloudStudents = FirestoreStudent.fetchSmartSyncStudents(
                    sekolahId = currentUser.sekolahId ?: "",
                    assignedClasses = currentUser.assignedClasses,
                    role = currentUser.role,
                    lastSync = lastSyncTimestamp
                )
                
                if (cloudStudents.isEmpty()) {
                    _syncState.value = SyncState.Success("Data sudah up-to-date.")
                    return@launch
                }

                _syncState.value = SyncState.Loading("Menyimpan ${cloudStudents.size} data baru...", 0.7f)

                // 4. Batch Insert (Upsert) ke Database Lokal
                faceDao.insertAll(cloudStudents)

                // 5. Simpan Timestamp Sekarang sebagai LastSync Terakhir
                val currentTimestamp = System.currentTimeMillis()
                prefs.edit().putLong("last_sync_${currentUser.sekolahId}", currentTimestamp).apply()

                // 6. Refresh Cache Wajah untuk Engine AI
                FaceCache.refresh(getApplication())

                _syncState.value = SyncState.Success("Berhasil sinkron ${cloudStudents.size} data.")
                
            } catch (e: Exception) {
                Log.e("SyncViewModel", "Smart Sync Failed", e)
                _syncState.value = SyncState.Error("Gagal: ${e.message}")
            }
        }
    }
    
    fun clearSyncHistory(sekolahId: String) {
        prefs.edit().remove("last_sync_$sekolahId").apply()
        _syncState.value = SyncState.Idle
    }

    fun resetState() {
        _syncState.value = SyncState.Idle
    }
}