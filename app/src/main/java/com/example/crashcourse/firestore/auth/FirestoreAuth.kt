package com.example.crashcourse.firestore.auth

import android.util.Log
import com.example.crashcourse.firestore.core.FirestoreCore
import com.example.crashcourse.firestore.core.FirestorePaths
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * 🔐 FirestoreAuth
 * Handles database operations related to Account Creation and Session Monitoring.
 * (Does NOT handle Firebase Auth login/logout, only Firestore data).
 */
object FirestoreAuth {

    private const val TAG = "FirestoreAuth"
    private val db = FirestoreCore.db

    // ==========================================
    // 1️⃣ CREATE ADMIN ACCOUNT (Register)
    // ==========================================
    suspend fun createAdminAccount(
        uid: String,
        email: String,
        schoolName: String,
        sekolahId: String,
        deviceId: String,
        expiryDate: Timestamp
    ) {
        try {
            val userData = hashMapOf(
                "uid" to uid,
                "email" to email,
                "school_name" to schoolName,
                "sekolahId" to sekolahId,
                "device_id" to deviceId,
                "status" to "ACTIVE",
                "role" to "ADMIN",
                "expiry_date" to expiryDate,
                "assigned_classes" to emptyList<String>(),
                "created_at" to System.currentTimeMillis()
            )

            db.collection(FirestorePaths.USERS)
                .document(uid)
                .set(userData, SetOptions.merge())
                .await()
            
            Log.d(TAG, "✅ Admin account created in Firestore: $uid")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to create admin account", e)
            throw e
        }
    }

    // ==========================================
    // 2️⃣ SESSION LISTENER
    // ==========================================
    fun listenToUserSession(
        uid: String,
        onEvent: (DocumentSnapshot?, FirebaseFirestoreException?) -> Unit
    ): ListenerRegistration {
        return db.collection(FirestorePaths.USERS)
            .document(uid)
            .addSnapshotListener(onEvent)
    }
}