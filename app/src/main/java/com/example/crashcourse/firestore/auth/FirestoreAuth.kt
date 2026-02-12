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
 * Single Source of Truth untuk manajemen akun di Cloud.
 * Dioptimalkan untuk performa tinggi dengan Direct Document ID Access.
 */
object FirestoreAuth {

    private const val TAG = "FirestoreAuth"
    private val db = FirestoreCore.db

    // ==========================================
    // 1️⃣ CREATE ADMIN ACCOUNT (Registrasi Sekolah Baru)
    // ==========================================
    /**
     * Membuat akun Admin pertama untuk sekolah baru.
     * Status default: PENDING (Menunggu verifikasi AzuraTech).
     */
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
                "status" to "PENDING", // 🚀 UPDATE: Admin baru tidak langsung aktif
                "role" to "ADMIN",
                "isRegistered" to true, 
                "expiry_date" to expiryDate,
                "assigned_classes" to emptyList<String>(),
                "created_at" to System.currentTimeMillis()
            )

            // Admin menggunakan UID sebagai ID Dokumen agar sinkron dengan Firebase Auth
            db.collection(FirestorePaths.USERS)
                .document(uid)
                .set(userData, SetOptions.merge())
                .await()
            
            Log.d(TAG, "✅ Admin PENDING created: $uid")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to create admin account", e)
            throw e
        }
    }

    // ==========================================
    // 2️⃣ CHECK INVITATION (Optimized)
    // ==========================================
    /**
     * Mencari undangan menggunakan Email sebagai ID Dokumen.
     * Jauh lebih cepat daripada query 'whereEqualTo'.
     */
    suspend fun getInvitationByEmail(email: String): DocumentSnapshot? {
        return try {
            val doc = db.collection(FirestorePaths.USERS)
                .document(email) // 🚀 Direct Access: ID dokumen adalah Email
                .get()
                .await()
            
            if (doc.exists() && doc.getBoolean("isRegistered") == false) {
                doc
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching invitation by email", e)
            null
        }
    }

    // ==========================================
    // 3️⃣ ACTIVATE STAFF ACCOUNT
    // ==========================================
    /**
     * Mengaktifkan akun yang sebelumnya diundang oleh Admin.
     * Menempelkan UID permanen dan mengubah status menjadi ACTIVE.
     */
    suspend fun activateStaffAccount(
        uid: String,
        email: String,
        deviceId: String
    ) {
        try {
            val updateData = hashMapOf(
                "uid" to uid,
                "device_id" to deviceId,
                "status" to "ACTIVE", // 🚀 Staff langsung aktif (tanggung jawab Admin Sekolah)
                "isRegistered" to true,
                "activated_at" to System.currentTimeMillis()
            )

            // Update dokumen undangan yang ID-nya adalah email
            db.collection(FirestorePaths.USERS)
                .document(email) 
                .update(updateData as Map<String, Any>)
                .await()

            Log.d(TAG, "✅ Staff account activated: $email")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to activate staff account", e)
            throw e
        }
    }

    // ==========================================
    // 4️⃣ SESSION LISTENER (Real-time Status)
    // ==========================================
    /**
     * Memantau perubahan status (misal: saat Admin di-approve oleh AzuraTech).
     */
    fun listenToUserSession(
        uid: String,
        onEvent: (DocumentSnapshot?, FirebaseFirestoreException?) -> Unit
    ): ListenerRegistration {
        // Kita gunakan query whereEqualTo UID karena ID dokumen bisa berupa Email (Staff) atau UID (Admin)
        return db.collection(FirestorePaths.USERS)
            .whereEqualTo("uid", uid)
            .limit(1)
            .addSnapshotListener { snapshot, e ->
                onEvent(snapshot?.documents?.firstOrNull(), e)
            }
    }
}