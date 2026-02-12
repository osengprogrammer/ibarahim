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
 * Diperbarui: Menambahkan fungsi updateUserScope untuk menyimpan akses kelas.
 */
object FirestoreUser {

    private const val TAG = "FirestoreUser"
    // Pastikan FirestoreCore.db sudah terinisialisasi
    private val db = FirestoreCore.db 

    // ==========================================
    // 1️⃣ FETCH USERS BY SCHOOL
    // ==========================================
    suspend fun fetchUsersBySchool(sekolahId: String): List<UserProfile> {
        return try {
            val snapshot = db.collection(FirestorePaths.USERS)
                .whereEqualTo(Constants.KEY_SEKOLAH_ID, sekolahId)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    UserProfile(
                        uid = doc.getString("uid") ?: doc.id, 
                        email = doc.getString("email") ?: "No Email",
                        role = doc.getString(Constants.FIELD_ROLE) ?: "TEACHER",
                        schoolName = doc.getString("school_name") ?: "",
                        sekolahId = doc.getString(Constants.KEY_SEKOLAH_ID) ?: "",
                        isRegistered = doc.getBoolean("isRegistered") ?: false,
                        assigned_classes = try {
                            (doc.get("assigned_classes") as? List<*>)?.map { it.toString() } ?: emptyList()
                        } catch (e: Exception) {
                            emptyList()
                        }
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing doc: ${doc.id}", e)
                    null 
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ fetchUsersBySchool failed", e)
            emptyList()
        }
    }

    // ==========================================
    // 2️⃣ INVITE STAFF BY MAP (Untuk ViewModel)
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
    // 3️⃣ INVITE STAFF (Legacy)
    // ==========================================
    suspend fun inviteStaff(user: UserProfile) {
        try {
            val data = hashMapOf(
                "email" to user.email,
                "role" to user.role,
                "sekolahId" to user.sekolahId,
                "school_name" to user.schoolName,
                "isRegistered" to false,
                "status" to "PENDING",
                "assigned_classes" to (user.assigned_classes ?: emptyList<String>()),
                "created_at" to System.currentTimeMillis()
            )

            db.collection(FirestorePaths.USERS)
                .document(user.email) 
                .set(data, SetOptions.merge())
                .await()
            
            Log.d(TAG, "✅ Undangan (Legacy) berhasil untuk: ${user.email}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ inviteStaff failed", e)
            throw e
        }
    }

    // ==========================================
    // 4️⃣ GET USER PROFILE
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
    // 6️⃣ UPDATE USER SCOPE (🔥 INI YANG BARU 🔥)
    // ==========================================
    suspend fun updateUserScope(docId: String, classes: List<String>) {
        try {
            val data = mapOf("assigned_classes" to classes)
            
            // Menggunakan set + merge lebih aman daripada update
            // agar tidak crash jika field assigned_classes belum ada sebelumnya
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