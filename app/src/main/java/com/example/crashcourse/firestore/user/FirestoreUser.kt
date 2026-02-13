package com.example.crashcourse.firestore.user

import android.util.Log
import com.example.crashcourse.firestore.core.FirestoreCore
import com.example.crashcourse.firestore.core.FirestorePaths
import com.example.crashcourse.utils.Constants
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * 👤 FirestoreUser
 * Repository khusus untuk operasi Manajemen User/Staff.
 */
object FirestoreUser {

    private const val TAG = "FirestoreUser"
    private val db = FirestoreCore.db 

    // ==========================================
    // 1️⃣ FETCH USERS BY SCHOOL (Clean Version)
    // ==========================================
    suspend fun fetchUsersBySchool(sekolahId: String): List<UserProfile> {
        return try {
            val snapshot = db.collection(FirestorePaths.USERS)
                .whereEqualTo(Constants.KEY_SEKOLAH_ID, sekolahId)
                .get()
                .await()

            // 🔥 Menggunakan toObject: Jauh lebih simple & menangani mapping otomatis
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(UserProfile::class.java)?.copy(uid = doc.id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ fetchUsersBySchool failed", e)
            emptyList()
        }
    }

    // ==========================================
    // 2️⃣ INVITE STAFF BY MAP
    // ==========================================
    suspend fun inviteStaffByMap(email: String, data: HashMap<String, Any>) {
        try {
            db.collection(FirestorePaths.USERS)
                .document(email) 
                .set(data, SetOptions.merge()) 
                .await()
            Log.d(TAG, "✅ Invite Map berhasil disimpan untuk: $email")
        } catch (e: Exception) {
            Log.e(TAG, "❌ inviteStaffByMap failed", e)
            throw e
        }
    }

    // ==========================================
    // 3️⃣ INVITE STAFF (Legacy Object Version)
    // ==========================================
    suspend fun inviteStaff(user: UserProfile) {
        try {
            // Kita gunakan Map agar field names di Firestore tetap menggunakan underscore
            val data = hashMapOf(
                "email" to user.email,
                "role" to user.role,
                "sekolahId" to user.sekolahId,
                "school_name" to user.schoolName,
                "isRegistered" to false,
                "status" to "PENDING",
                "assigned_classes" to user.assigned_classes,
                "created_at" to System.currentTimeMillis()
            )

            db.collection(FirestorePaths.USERS)
                .document(user.email) 
                .set(data, SetOptions.merge())
                .await()
            
            Log.d(TAG, "✅ Undangan berhasil untuk: ${user.email}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ inviteStaff failed", e)
            throw e
        }
    }

    // ==========================================
    // 4️⃣ GET USER PROFILE (🔥 FIX UNTUK LOGIN 🔥)
    // ==========================================
    suspend fun getUserProfile(uid: String): UserProfile? {
        return try {
            val doc = db.collection(FirestorePaths.USERS)
                .document(uid)
                .get()
                .await()
            
            // Jika data ada, langsung diconvert ke Object UserProfile
            doc.toObject(UserProfile::class.java)?.copy(uid = doc.id)
        } catch (e: Exception) {
            Log.e(TAG, "❌ getUserProfile failed", e)
            null
        }
    }

    // ==========================================
    // 5️⃣ UPDATE DEVICE BINDING
    // ==========================================
    suspend fun updateDeviceBinding(uid: String, deviceId: String) {
        try {
            db.collection(FirestorePaths.USERS)
                .document(uid)
                .update("device_id", deviceId)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "❌ updateDeviceBinding failed", e)
        }
    }

    // ==========================================
    // 6️⃣ UPDATE USER SCOPE
    // ==========================================
    suspend fun updateUserScope(docId: String, classes: List<String>) {
        try {
            // Pastikan key "assigned_classes" sesuai dengan yang ada di Firestore
            val data = mapOf("assigned_classes" to classes)
            
            db.collection(FirestorePaths.USERS)
                .document(docId)
                .set(data, SetOptions.merge()) 
                .await()
                
            Log.d(TAG, "✅ Scope berhasil diupdate untuk: $docId")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Gagal update scope", e)
            throw e
        }
    }
}