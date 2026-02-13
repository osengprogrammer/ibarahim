package com.example.crashcourse.firestore.user

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName // 🔥 WAJIB IMPORT

/**
 * 👤 UserProfile Data Transfer Object (DTO)
 */
@IgnoreExtraProperties
data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val role: String = "TEACHER",
    
    @get:PropertyName("school_name")
    @set:PropertyName("school_name")
    var schoolName: String = "",
    
    val sekolahId: String = "",
    val isRegistered: Boolean = false,
    
    // Mapping otomatis untuk assigned_classes agar tidak STUCK saat login
    @get:PropertyName("assigned_classes")
    @set:PropertyName("assigned_classes")
    var assigned_classes: List<String> = emptyList()
)