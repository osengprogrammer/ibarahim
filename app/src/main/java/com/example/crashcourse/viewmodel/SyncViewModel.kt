package com.example.crashcourse.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crashcourse.repository.SyncRepository
import com.example.crashcourse.ui.SyncState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 🔄 SyncViewModel (V.10.23 - Auto-Dismiss Edition)
 * Mengontrol logika sinkronisasi data dengan umpan balik visual yang responsif.
 */
class SyncViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = SyncRepository(application)
    
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState = _syncState.asStateFlow()

    /**
     * 🔥 Fungsi utama sinkronisasi yang dipanggil dari Screen
     */
    fun syncStudentsDown(forceFullSync: Boolean = false) {
        // 🛡️ ANTI-SPAM: Jika sedang loading, jangan hiraukan klik tombol
        if (_syncState.value is SyncState.Loading) return

        viewModelScope.launch {
            // 1. Munculkan indikator loading
            _syncState.value = SyncState.Loading("Menganalisis data cloud...", 0.3f)

            // 2. Eksekusi sinkronisasi di Repository
            val result = repository.performSmartSync(forceFullSync)
            
            // 3. Tampilkan hasil (Overlay Hijau / Merah)
            result.onSuccess { count ->
                if (count == 0) {
                    _syncState.value = SyncState.Success("Data sudah up-to-date.")
                } else {
                    _syncState.value = SyncState.Success("Berhasil sinkron $count data baru.")
                }
            }.onFailure { e ->
                _syncState.value = SyncState.Error("Gagal: ${e.message}")
            }

            // 4. ⏱️ THE MAGIC FIX: Tahan notifikasi selama 3 detik agar bisa dibaca user
            delay(3000)
            
            // 5. Kembalikan ke Idle agar animasi bisa diulang di klik berikutnya
            // (Pengecekan ini memastikan state tidak menimpa loading dari proses lain)
            if (_syncState.value !is SyncState.Loading) {
                resetState()
            }
        }
    }

    /**
     * 🧹 Reset state ke Idle (Dipisahkan agar UI bisa menutup dialog/toast secara manual)
     */
    fun resetState() {
        _syncState.value = SyncState.Idle
    }
}