package com.example.crashcourse.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 👤 Azura Tech User Entity
 * Single-row table for current logged-in user (offline-first).
 */
@Entity(tableName = "current_user")
data class UserEntity(
    @PrimaryKey
    val uid: String,
    val sekolahId: String?,
    val deviceId: String,
    val name: String, // Ini akan diisi dari school_name
    val email: String,
    val role: String,
    val assignedClasses: List<String> = emptyList(),
    
    // 🛡️ BUAT DEFAULT 0 agar jika User biasa tidak punya expiry_date di Firestore, tidak crash
    val expiryMillis: Long = 0L, 
    
    val photoUrl: String? = null,
    val lastSync: Long = System.currentTimeMillis()
)