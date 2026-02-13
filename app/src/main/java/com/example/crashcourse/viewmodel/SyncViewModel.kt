package com.example.crashcourse.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crashcourse.repository.SyncRepository
import com.example.crashcourse.ui.SyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SyncViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = SyncRepository(application)
    
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState = _syncState.asStateFlow()

    /**
     * 🔥 Fungsi utama sinkronisasi yang dipanggil dari Screen
     */
    fun syncStudentsDown(forceFullSync: Boolean = false) {
        viewModelScope.launch {
            _syncState.value = SyncState.Loading("Menganalisis data cloud...", 0.3f)

            val result = repository.performSmartSync(forceFullSync)
            
            result.onSuccess { count ->
                if (count == 0) {
                    _syncState.value = SyncState.Success("Data sudah up-to-date.")
                } else {
                    _syncState.value = SyncState.Success("Berhasil sinkron $count data baru.")
                }
            }.onFailure { e ->
                _syncState.value = SyncState.Error("Gagal: ${e.message}")
            }
        }
    }

    /**
     * 🧹 Reset state ke Idle (Dipisahkan agar UI bisa menutup dialog/toast)
     */
    fun resetState() {
        _syncState.value = SyncState.Idle
    }
}