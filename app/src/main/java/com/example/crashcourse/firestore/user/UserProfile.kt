package com.example.crashcourse.firestore.user

import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * 👤 UserProfile Data Transfer Object (DTO)
 * Model data pusat untuk informasi akun Staff/Guru di Firestore.
 * * @param uid ID unik dari Firebase Auth (kosong jika baru diundang).
 * @param email Email resmi yang didaftarkan.
 * @param role Peran dalam sistem (ADMIN, SUPERVISOR, TEACHER).
 * @param schoolName Nama sekolah yang terikat dengan user ini.
 * @param sekolahId ID unik sekolah untuk isolasi data Multi-Tenant.
 * @param isRegistered Status aktivasi (false = baru diundang, true = sudah punya password).
 * @param assigned_classes Daftar nama kelas (Rombel) yang boleh diakses oleh user ini.
 */
@IgnoreExtraProperties // Mencegah crash jika ada field tambahan di Firestore yang tidak didefinisikan di sini
data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val role: String = "TEACHER",
    val schoolName: String = "",
    val sekolahId: String = "",
    val isRegistered: Boolean = false,
    val assigned_classes: List<String> = emptyList()
)