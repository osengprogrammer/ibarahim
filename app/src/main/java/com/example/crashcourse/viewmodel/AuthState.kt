package com.example.crashcourse.viewmodel

/**
 * 🏗️ Auth State Sealed Class
 * Dipisahkan ke file sendiri agar tidak error "Redeclaration".
 */
sealed class AuthState {
    object Checking : AuthState()
    object LoggedOut : AuthState()
    
    data class Loading(val message: String) : AuthState()
    data class StatusWaiting(val message: String) : AuthState()
    data class Error(val message: String) : AuthState()
    
    // ✅ ACTIVE STATE (Data Siap Pakai di UI)
    data class Active(
        val uid: String,
        val email: String,
        val role: String,
        val schoolName: String,
        val sekolahId: String,
        val expiryMillis: Long,          // Field Baru
        val assignedClasses: List<String> // Field Baru
    ) : AuthState()
}