package com.example.crashcourse.viewmodel

/**
 * 🏗️ Auth State Sealed Class (V.10.26 - Unified Edition)
 * Mewakili state UI saat proses autentikasi berlangsung.
 * Selaras dengan skema schoolId tunggal AzuraTech.
 */
sealed class AuthState {
    
    // ⏳ Status Transisi
    object Checking : AuthState()
    object LoggedOut : AuthState()
    
    data class Loading(val message: String) : AuthState()
    data class StatusWaiting(val message: String) : AuthState()
    data class Error(val message: String) : AuthState()
    
    // ✅ ACTIVE STATE (Data Siap Pakai di UI / Dashboard)
    data class Active(
        val uid: String,
        val email: String,
        val role: String,
        val schoolName: String,
        
        /**
         * 🏫 Identitas Sekolah (Unified)
         * Menggunakan schoolId agar sinkron dengan UserEntity dan FaceEntity.
         * Tidak lagi menggunakan 'schoolId'.
         */
        val schoolId: String, 
        
        // 🔐 Security: Masa berlaku lisensi
        val expiryMillis: Long,
        
        // 📚 Hak Akses Multi-Tenant
        val assignedClasses: List<String> = emptyList(),
        val assignedSubjects: List<String> = emptyList(),
        val accessibleSchoolIds: List<String> = emptyList()
    ) : AuthState()
}