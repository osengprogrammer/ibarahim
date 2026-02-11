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

    // Nullable: Firestore may not send immediately
    val sekolahId: String?,

    val deviceId: String,
    val name: String,
    val email: String,
    val role: String,

    // Requires TypeConverters (List<String>)
    val assignedClasses: List<String> = emptyList(),

    // License / session validity (offline-safe)
    val expiryMillis: Long = 0L,

    val photoUrl: String? = null,

    // Last successful sync timestamp
    val lastSync: Long = System.currentTimeMillis()
)
