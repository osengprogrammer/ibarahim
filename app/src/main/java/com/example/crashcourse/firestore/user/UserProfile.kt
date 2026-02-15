package com.example.crashcourse.firestore.user

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

/**
 * 👤 UserProfile (DTO) - V.10.23
 * Unified Identity: Menggunakan schoolId tunggal dan Boolean isActive.
 * Selaras 100% dengan UserEntity dan UserDao yang baru.
 */
@IgnoreExtraProperties
data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val role: String = "TEACHER",
    
    @get:PropertyName("school_name")
    @set:PropertyName("school_name")
    var schoolName: String = "",
    
    /**
     * ✅ SATU NAMA: schoolId
     * Tidak lagi menggunakan 'schoolId'. 
     * Nama variabel di Kotlin = Nama field di Firestore.
     */
    var schoolId: String = "",
    
    /**
     * ✅ STATUS BOOLEAN
     * Mengganti sistem String status agar tidak ada lagi typo "ACTIVE" vs "active".
     */
    @get:PropertyName("isActive")
    @set:PropertyName("isActive")
    var isActive: Boolean = false,

    @get:PropertyName("isRegistered")
    @set:PropertyName("isRegistered")
    var isRegistered: Boolean = false,
    
    @get:PropertyName("assigned_classes")
    @set:PropertyName("assigned_classes")
    var assigned_classes: List<String> = emptyList(),
    
    @get:PropertyName("device_id")
    @set:PropertyName("device_id")
    var device_id: String = ""
)