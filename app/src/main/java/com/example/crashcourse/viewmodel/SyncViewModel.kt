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
     * @param uid The UID of the currently logged-in teacher/admin
     */
    fun syncStudentsDown(uid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (uid.isBlank()) {
                _syncState.value = SyncState.Error("User ID tidak valid. Silakan login ulang.")
                return@launch
            }

            _syncState.value = SyncState.Loading("Mengecek skop kelas Anda...")

            try {
                // 1. Download dari Firestore berdasarkan SCOPE
                // Fungsi ini otomatis cek Role & assigned_classes di Firestore
                val cloudStudents = FirestoreHelper.getScopedStudentsFromFirestore(uid)
                
                if (cloudStudents.isEmpty()) {
                    _syncState.value = SyncState.Error("Tidak ada data murid untuk skop kelas Anda.")
                    return@launch
                }

                _syncState.value = SyncState.Loading("Sinkronisasi ${cloudStudents.size} data...")

                // 2. Simpan ke Local Room
                cloudStudents.forEach { student ->
                    faceDao.insert(student)
                }

                // 3. Refresh Cache Wajah untuk Recognition Engine (C++)
                FaceCache.refresh(getApplication())

                _syncState.value = SyncState.Success("Berhasil! ${cloudStudents.size} murid tersinkronisasi.")
            } catch (e: Exception) {
                _syncState.value = SyncState.Error("Gagal Sync: ${e.message}")
            }
        }
    }
    
    fun resetState() {
        _syncState.value = SyncState.Idle
    }
}

sealed class SyncState {
    object Idle : SyncState()
    data class Loading(val message: String) : SyncState()
    data class Success(val message: String) : SyncState()
    data class Error(val message: String) : SyncState()
}