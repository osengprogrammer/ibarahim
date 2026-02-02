package com.example.crashcourse.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crashcourse.util.DeviceUtil
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth // Pastikan ini ada
import com.google.firebase.auth.auth         // Pastikan ini ada
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = Firebase.auth
    private val db = Firebase.firestore
    private val currentDeviceId = DeviceUtil.getUniqueDeviceId(application.applicationContext)
    
    // Listener agar app otomatis terbuka kalau Admin mengaktifkan akun
    private var statusListener: ListenerRegistration? = null

    private val _authState = MutableStateFlow<AuthState>(AuthState.Checking)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkCurrentUser()
    }

    // 1. Cek User Saat Buka Aplikasi
    private fun checkCurrentUser() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            startListeningToUserStatus(currentUser.uid)
        } else {
            _authState.value = AuthState.LoggedOut
        }
    }

    // 2. Registrasi Baru (Otomatis Kirim Hardware ID)
    fun register(email: String, pass: String, schoolName: String) {
        if (email.isBlank() || pass.isBlank() || schoolName.isBlank()) {
            _authState.value = AuthState.Error("Semua kolom wajib diisi.")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                // Buat Akun Auth
                val result = auth.createUserWithEmailAndPassword(email, pass).await()
                val uid = result.user?.uid ?: throw Exception("Gagal mendapatkan UID")

                // Simpan Data User + DEVICE ID ke Firestore
                val userData = hashMapOf(
                    "email" to email,
                    "school_name" to schoolName,
                    "device_id" to currentDeviceId, // <--- KUNCI PENGAMAN
                    "status" to "PENDING", // Menunggu Admin
                    "created_at" to System.currentTimeMillis()
                )

                db.collection("users").document(uid).set(userData).await()
                startListeningToUserStatus(uid)

            } catch (e: Exception) {
                _authState.value = AuthState.Error("Registrasi Gagal: ${e.message}")
            }
        }
    }

    // 3. Login (Cek Password & Hardware ID)
    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _authState.value = AuthState.Error("Email dan Password wajib diisi.")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = auth.signInWithEmailAndPassword(email, pass).await()
                val uid = result.user?.uid!!
                
                // Cek apakah HP ini sesuai dengan yang terdaftar?
                val doc = db.collection("users").document(uid).get().await()
                val registeredDevice = doc.getString("device_id")

                if (registeredDevice != null && registeredDevice != currentDeviceId) {
                    auth.signOut()
                    _authState.value = AuthState.Error("AKSES DITOLAK.\nAkun ini terkunci pada perangkat: $registeredDevice.\nAnda menggunakan: $currentDeviceId")
                } else {
                    startListeningToUserStatus(uid)
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Login Gagal: ${e.message}")
            }
        }
    }

    // 4. Lupa Password (Reset Link)
    fun sendPasswordReset(email: String) {
        if (email.isBlank()) {
            _authState.value = AuthState.Error("Mohon isi email terlebih dahulu.")
            return
        }
        _authState.value = AuthState.Loading
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                _authState.value = AuthState.Error("Link reset password telah dikirim ke $email.\nCek inbox/spam Anda.")
            }
            .addOnFailureListener {
                _authState.value = AuthState.Error("Gagal kirim email: ${it.message}")
            }
    }

    // 5. Realtime Monitor (Jantung Sistem)
    private fun startListeningToUserStatus(uid: String) {
        statusListener?.remove() // Hapus listener lama biar gak numpuk

        statusListener = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _authState.value = AuthState.Error("Sinkronisasi error: ${e.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val status = snapshot.getString("status") ?: "PENDING"
                    val schoolName = snapshot.getString("school_name") ?: "User"
                    val registeredDevice = snapshot.getString("device_id")

                    // Security Check Double Layer
                    if (registeredDevice != currentDeviceId) {
                         _authState.value = AuthState.Error("Sesi dicabut. Perangkat tidak cocok.")
                         return@addSnapshotListener
                    }

                    when (status) {
                        "ACTIVE" -> _authState.value = AuthState.Active(schoolName)
                        "BANNED" -> _authState.value = AuthState.StatusWaiting("AKUN DIBEKUKAN.\nHubungi Admin AzuraTech.")
                        "EXPIRED" -> _authState.value = AuthState.StatusWaiting("Masa Aktif Habis.\nSilakan perpanjang lisensi.")
                        else -> _authState.value = AuthState.StatusWaiting("MENUNGGU AKTIVASI.\n\nAdmin sedang memverifikasi data sekolah Anda.\nID Perangkat: $currentDeviceId")
                    }
                } else {
                    _authState.value = AuthState.LoggedOut
                }
            }
    }

    fun logout() {
        statusListener?.remove()
        auth.signOut()
        _authState.value = AuthState.LoggedOut
    }
}

sealed class AuthState {
    object Checking : AuthState()
    object LoggedOut : AuthState()
    object Loading : AuthState()
    data class Active(val schoolName: String) : AuthState()
    data class StatusWaiting(val message: String) : AuthState()
    data class Error(val message: String) : AuthState()
}