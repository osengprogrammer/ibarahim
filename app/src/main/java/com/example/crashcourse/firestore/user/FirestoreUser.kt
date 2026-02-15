package com.example.crashcourse.firestore.user

import android.util.Log
import com.example.crashcourse.firestore.core.FirestoreCore
import com.example.crashcourse.firestore.core.FirestorePaths
import com.example.crashcourse.utils.Constants
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * 👤 FirestoreUser (V.10.22 - Unified Identity)
 * Repository khusus untuk manajemen staff dengan skema schoolId tunggal dan status Boolean.
 */
object FirestoreUser {

    private const val TAG = "FirestoreUser"
    private val db = FirestoreCore.db 

    // ==========================================
    // 1️⃣ FETCH USERS BY SCHOOL
    // ==========================================
    
    suspend fun fetchUsersBySchool(schoolId: String): List<UserProfile> {
        return try {
            if (schoolId.isBlank()) {
                Log.w(TAG, "⚠️ fetchUsersBySchool aborted: schoolId is blank")
                return emptyList()
            }

            val snapshot = db.collection(FirestorePaths.USERS)
                .whereEqualTo("schoolId", schoolId) // ✅ Tetap konsisten dengan schoolId
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(UserProfile::class.java)?.copy(uid = doc.id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ fetchUsersBySchool failed", e)
            emptyList()
        }
    }

    // ==========================================
    // 2️⃣ INVITE STAFF (Updated to Unified Logic)
    // ==========================================
    
    suspend fun inviteStaff(user: UserProfile) {
        try {
            // 🔥 FIXED: Menggunakan 'schoolId' dan 'isActive' (Boolean)
            val data = hashMapOf(
                "email" to user.email,
                "role" to user.role,
                "schoolId" to user.schoolId, // ✅ Menggunakan schoolId tunggal
                "school_name" to user.schoolName,
                "isRegistered" to false,
                "isActive" to false, // ✅ Menggunakan Boolean status
                "assigned_classes" to user.assigned_classes,
                "created_at" to System.currentTimeMillis()
            )

            db.collection(FirestorePaths.USERS)
                .document(user.email.lowercase().trim()) 
                .set(data, SetOptions.merge())
                .await()
            
            Log.d(TAG, "✅ Staff Invited dengan schoolId: ${user.schoolId}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ inviteStaff failed", e)
            throw e
        }
    }

    // ==========================================
    // 3️⃣ GET USER PROFILE
    // ==========================================
    
    suspend fun getUserProfile(uid: String): UserProfile? {
        return try {
            val doc = db.collection(FirestorePaths.USERS)
                .document(uid)
                .get()
                .await()
            
            doc.toObject(UserProfile::class.java)?.copy(uid = doc.id)
        } catch (e: Exception) {
            Log.e(TAG, "❌ getUserProfile failed", e)
            null
        }
    }

    // ==========================================
    // 4️⃣ SECURITY: UPDATE DEVICE BINDING
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
    // 5️⃣ UPDATE USER SCOPE
    // ==========================================
    
    suspend fun updateUserScope(docId: String, classes: List<String>) {
        try {
            val data = mapOf("assigned_classes" to classes)
            
            db.collection(FirestorePaths.USERS)
                .document(docId)
                .set(data, SetOptions.merge()) 
                .await()
                
            Log.d(TAG, "✅ Scope updated for: $docId")
        } catch (e: Exception) {
            Log.e(TAG, "❌ updateUserScope failed", e)
            throw e
        }
    }
}