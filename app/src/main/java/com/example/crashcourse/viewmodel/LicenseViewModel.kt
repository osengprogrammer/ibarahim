package com.example.crashcourse.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LicenseViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Firebase.firestore
    private val prefs = application.getSharedPreferences("azura_license_prefs", Context.MODE_PRIVATE)

    // State untuk UI: Loading, Sukses, atau Gagal
    private val _licenseState = MutableStateFlow<LicenseState>(LicenseState.Checking)
    val licenseState: StateFlow<LicenseState> = _licenseState.asStateFlow()

    init {
        // Pas aplikasi dibuka, cek apakah sudah ada lisensi tersimpan di HP?
        checkSavedLicense()
    }

    private fun checkSavedLicense() {
        val savedKey = prefs.getString("license_key", null)
        if (savedKey != null) {
            // Kalau ada key tersimpan, validasi lagi ke server (biar aman kalau dicabut sewaktu-waktu)
            verifyLicenseKey(savedKey)
        } else {
            // Kalau tidak ada key, minta user login
            _licenseState.value = LicenseState.NeedsKey
        }
    }

    fun verifyLicenseKey(key: String) {
        viewModelScope.launch {
            _licenseState.value = LicenseState.Loading
            
            try {
                // Cari dokumen dengan ID = Key yang dimasukkan
                val docRef = db.collection("licenses").document(key).get().await()

                if (docRef.exists()) {
                    val isActive = docRef.getBoolean("active") ?: false
                    val schoolName = docRef.getString("school_name") ?: "Unknown School"

                    if (isActive) {
                        // LISENSI VALID!
                        // Simpan ke memori HP biar besok gak usah login lagi
                        prefs.edit().putString("license_key", key).apply()
                        prefs.edit().putString("school_name", schoolName).apply()
                        
                        _licenseState.value = LicenseState.Valid(schoolName)
                    } else {
                        _licenseState.value = LicenseState.Error("Lisensi ini sudah dinonaktifkan.")
                    }
                } else {
                    _licenseState.value = LicenseState.Error("Kode Lisensi tidak ditemukan.")
                }
            } catch (e: Exception) {
                // Kalau internet mati, tapi di HP ada key tersimpan, kita anggap Valid sementara (Offline Mode)
                val savedKey = prefs.getString("license_key", null)
                if (savedKey == key) {
                     val savedSchool = prefs.getString("school_name", "Offline Mode")
                     _licenseState.value = LicenseState.Valid(savedSchool!!)
                } else {
                    _licenseState.value = LicenseState.Error("Gagal koneksi: ${e.message}")
                }
            }
        }
    }
    
    // Fungsi Logout (untuk testing atau ganti lisensi)
    fun clearLicense() {
        prefs.edit().clear().apply()
        _licenseState.value = LicenseState.NeedsKey
    }
}

// Status untuk UI
sealed class LicenseState {
    object Checking : LicenseState() // Sedang cek otomatis
    object NeedsKey : LicenseState() // Belum punya kunci, tampilkan form input
    object Loading : LicenseState()  // Sedang loading connect ke server
    data class Valid(val schoolName: String) : LicenseState() // Sukses masuk
    data class Error(val message: String) : LicenseState() // Kunci salah
}