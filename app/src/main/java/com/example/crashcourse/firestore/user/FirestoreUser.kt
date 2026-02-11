package com.example.crashcourse.firestore.user

import android.util.Log
import com.example.crashcourse.firestore.core.FirestoreCore
import com.example.crashcourse.firestore.core.FirestorePaths
import com.example.crashcourse.utils.Constants
import kotlinx.coroutines.tasks.await

/**
 * 💡 UserProfile DTO
 * Moved here to be shared between FirestoreUser and ViewModel.
 */
data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val role: String = "USER",
    val schoolName: String = "",
    val sekolahId: String = "",
    val assignedClasses: List<String> = emptyList()
)

/**
 * 👤 FirestoreUser
 * Dedicated repository for User Management operations.
 */
object FirestoreUser {

    private const val TAG = "FirestoreUser"
    private val db = FirestoreCore.db

    // ==========================================
    // 1️⃣ FETCH USERS BY SCHOOL (For Admin List)
    // ==========================================
    suspend fun fetchUsersBySchool(sekolahId: String): List<UserProfile> {
        return try {
            // 🛡️ SECURITY FILTER: Only fetch users from the specific school
            val snapshot = db.collection(FirestorePaths.USERS)
                .whereEqualTo(Constants.KEY_SEKOLAH_ID, sekolahId)
                .get()
                .await()

            snapshot.documents.map { doc ->
                UserProfile(
                    uid = doc.id,
                    email = doc.getString("email") ?: "No Email",
                    role = doc.getString(Constants.FIELD_ROLE) ?: "USER",
                    schoolName = doc.getString("school_name") ?: "",
                    sekolahId = doc.getString(Constants.KEY_SEKOLAH_ID) ?: "",
                    // 🛡️ Safe Casting for Arrays
                    assignedClasses = try {
                        (doc.get("assigned_classes") as? List<*>)?.map { it.toString() } ?: emptyList()
                    } catch (e: Exception) {
                        emptyList()
                    }
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ fetchUsersBySchool failed", e)
            emptyList()
        }
    }

    // ==========================================
    // 2️⃣ GET USER PROFILE (For Auth/Session)
    // ==========================================
    suspend fun getUserProfile(uid: String): Map<String, Any?>? {
        return try {
            db.collection(FirestorePaths.USERS)
                .document(uid)
                .get()
                .await()
                .data
        } catch (e: Exception) {
            Log.e(TAG, "❌ getUserProfile failed", e)
            null
        }
    }

    // ==========================================
    // 3️⃣ UPDATE DEVICE BINDING
    // ==========================================
    suspend fun updateDeviceBinding(uid: String, deviceId: String) {
        try {
            db.collection(FirestorePaths.USERS)
                .document(uid)
                .update("deviceId", deviceId)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "❌ updateDeviceBinding failed", e)
        }
    }
}