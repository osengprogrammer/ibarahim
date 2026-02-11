package com.example.crashcourse.viewmodel

/**
 * 🔐 State Management Pusat untuk Autentikasi
 */
sealed class AuthState {
    // Sedang memeriksa status login (Splash Screen)
    object Checking : AuthState()
    
    // User belum login
    object LoggedOut : AuthState()
    
    // Sedang loading (Login/Register process)
    data class Loading(val message: String? = null) : AuthState()
    
    // User Aktif (Login Berhasil)
    data class Active(
        val uid: String,
        val email: String,
        val role: String,       // ADMIN / TEACHER
        val schoolName: String,
        val sekolahId: String,  // 🚀 ID Sekolah Penting
        val expiryMillis: Long,
        val assignedClasses: List<String> // Scope Akses Guru
    ) : AuthState()
    
    // Error terjadi (Wrong password, network fail)
    data class Error(val message: String) : AuthState()
    
    // Menunggu Aktivasi / Lisensi Habis
    data class StatusWaiting(val message: String) : AuthState()
}