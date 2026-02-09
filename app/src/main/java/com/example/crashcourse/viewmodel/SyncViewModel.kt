package com.example.crashcourse.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crashcourse.db.AppDatabase
import com.example.crashcourse.db.FaceCache
import com.example.crashcourse.utils.FirestoreHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SyncViewModel(application: Application) : AndroidViewModel(application) {
    private val faceDao = AppDatabase.getInstance(application).faceDao()
    
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState = _syncState.asStateFlow()

    /**
     * 🔥 UPDATED: Sync Down with Teacher Scope
     * Mengunduh data siswa dari Cloud ke HP, disaring berdasarkan hak akses guru.
     */
    fun syncStudentsDown(uid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Validasi UID
            if (uid.isBlank()) {
                _syncState.value = SyncState.Error("User ID tidak valid. Silakan login ulang.")
                return@launch
            }

            _syncState.value = SyncState.Loading("Mengecek data kelas Anda...")

            try {
                // 2. Download dari Firestore (Auto Scope: Admin ambil semua, Guru ambil kelasnya saja)
                val cloudStudents = FirestoreHelper.getScopedStudentsFromFirestore(uid)
                
                if (cloudStudents.isEmpty()) {
                    // Kita anggap Sukses tapi kosong, agar user tidak panik dikira error
                    _syncState.value = SyncState.Success("Data tersinkronisasi (Tidak ada murid ditemukan).")
                    return@launch
                }

                _syncState.value = SyncState.Loading("Menyimpan ${cloudStudents.size} data murid...")

                // 3. 🚀 OPTIMASI: Batch Insert
                // Menggunakan insertAll jauh lebih cepat daripada looping satu per satu
                faceDao.insertAll(cloudStudents)

                // 4. Refresh Cache Wajah (Penting untuk C++ Engine)
                // Pastikan ini berjalan agar murid yang baru didownload langsung bisa dikenali
                FaceCache.refresh(getApplication())

                _syncState.value = SyncState.Success("Selesai! ${cloudStudents.size} murid berhasil didownload.")
            } catch (e: Exception) {
                _syncState.value = SyncState.Error("Gagal Sync: ${e.message}")
            }
        }
    }
    
    fun resetState() {
        _syncState.value = SyncState.Idle
    }
}

// State Management untuk UI (Loading/Success/Error)
sealed class SyncState {
    object Idle : SyncState()
    data class Loading(val message: String) : SyncState()
    data class Success(val message: String) : SyncState()
    data class Error(val message: String) : SyncState()
}