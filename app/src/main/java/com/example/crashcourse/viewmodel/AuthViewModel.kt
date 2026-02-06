package com.example.crashcourse.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crashcourse.util.DeviceUtil
import com.google.firebase.Firebase
import com.google.firebase.Timestamp // 🚀 Added for expiry_date
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = Firebase.auth
    private val db = Firebase.firestore
    private val currentDeviceId = DeviceUtil.getUniqueDeviceId(application.applicationContext)
    private val prefs = application.getSharedPreferences("AzuraAuthPrefs", Context.MODE_PRIVATE)
    private var statusListener: ListenerRegistration? = null

    private val _authState = MutableStateFlow<AuthState>(AuthState.Checking)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            startListeningToUserStatus(currentUser.uid)
        } else {
            _authState.value = AuthState.LoggedOut
        }
    }

    // --- 1. REGISTRASI (Automated Expiry) ---
    fun register(email: String, pass: String, schoolName: String) {
        if (email.isBlank() || pass.isBlank() || schoolName.isBlank()) {
            _authState.value = AuthState.Error("Semua kolom wajib diisi.")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = auth.createUserWithEmailAndPassword(email, pass).await()
                val uid = result.user?.uid ?: throw Exception("Gagal mendapatkan UID")

                // 🚀 AUTO-GENERATE EXPIRY DATE (e.g., 30 Days Trial)
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, 30) 
                val defaultExpiry = Timestamp(calendar.time)

                val userData = hashMapOf(
                    "uid" to uid,
                    "email" to email,
                    "school_name" to schoolName,
                    "device_id" to currentDeviceId,
                    "status" to "PENDING",
                    "role" to "ADMIN", // First registrant is usually the Admin
                    "max_offline_days" to 7,
                    "expiry_date" to defaultExpiry, // 🚀 Now added automatically
                    "assigned_classes" to emptyList<String>(),
                    "created_at" to System.currentTimeMillis()
                )
                db.collection("users").document(uid).set(userData).await()
                startListeningToUserStatus(uid)
            } catch (e: Exception) {
                val msg = if (e is FirebaseAuthUserCollisionException) {
                    "Email sudah terdaftar. Silakan login."
                } else {
                    "Registrasi Gagal: ${e.message}"
                }
                _authState.value = AuthState.Error(msg)
            }
        }
    }

    // --- 2. LOGIN ---
    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _authState.value = AuthState.Error("Email dan Password wajib diisi.")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = auth.signInWithEmailAndPassword(email, pass).await()
                val uid = result.user?.uid!!
                val doc = db.collection("users").document(uid).get().await()
                val registeredDevice = doc.getString("device_id")

                if (registeredDevice != null && registeredDevice != currentDeviceId) {
                    auth.signOut()
                    _authState.value = AuthState.Error("AKSES DITOLAK.\nPerangkat tidak sesuai lisensi.")
                } else {
                    startListeningToUserStatus(uid)
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Login Gagal: ${e.message}")
            }
        }
    }

    // --- 3. INVITE STAFF ---
    fun inviteStaff(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _authState.value = AuthState.Error("Data guru tidak lengkap.")
            return
        }
        viewModelScope.launch {
            try {
                val adminUid = auth.currentUser!!.uid
                val adminDoc = db.collection("users").document(adminUid).get().await()
                val schoolName = adminDoc.getString("school_name") ?: ""
                val expiryDate = adminDoc.getTimestamp("expiry_date")
                val maxOffline = adminDoc.getLong("max_offline_days") ?: 7

                val result = auth.createUserWithEmailAndPassword(email, pass).await()
                val newUid = result.user?.uid ?: throw Exception("Gagal")

                val staffData = hashMapOf(
                    "uid" to newUid,
                    "email" to email,
                    "school_name" to schoolName,
                    "device_id" to currentDeviceId,
                    "status" to "ACTIVE",
                    "role" to "USER",
                    "expiry_date" to expiryDate, // 🚀 Inherits expiry from Admin
                    "max_offline_days" to maxOffline,
                    "assigned_classes" to emptyList<String>(), 
                    "invited_by" to adminUid,
                    "created_at" to System.currentTimeMillis()
                )
                db.collection("users").document(newUid).set(staffData).await()

                auth.signOut() 
                statusListener?.remove()
                _authState.value = AuthState.LoggedOut
                
            } catch (e: Exception) {
                val msg = if (e is FirebaseAuthUserCollisionException) "Email guru sudah terdaftar!" else "Gagal Invite: ${e.message}"
                _authState.value = AuthState.Error(msg)
            }
        }
    }

    // --- 4. RESET PASSWORD ---
    fun sendPasswordReset(email: String) {
        if (email.isBlank()) {
            _authState.value = AuthState.Error("Isi email dulu.")
            return
        }
        viewModelScope.launch {
            try {
                auth.sendPasswordResetEmail(email).await()
                _authState.value = AuthState.Error("Link reset terkirim.")
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Gagal: ${e.message}")
            }
        }
    }

    // --- 5. MONITORING ---
    private fun startListeningToUserStatus(uid: String) {
        statusListener?.remove()
        statusListener = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    val savedMaxOffline = prefs.getInt("max_offline_days", 7)
                    checkOfflineGracePeriod(savedMaxOffline)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val status = snapshot.getString("status") ?: "PENDING"
                    val role = snapshot.getString("role") ?: "USER"
                    val email = snapshot.getString("email") ?: ""
                    val schoolName = snapshot.getString("school_name") ?: "User"
                    val registeredDevice = snapshot.getString("device_id")
                    val expiryTimestamp = snapshot.getTimestamp("expiry_date")
                    val maxOffline = snapshot.getLong("max_offline_days")?.toInt() ?: 7
                    
                    val assignedFromDb = (snapshot.get("assigned_classes") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    
                    val currentTime = Date()

                    prefs.edit().apply {
                        putLong("last_sync_time", System.currentTimeMillis())
                        putInt("max_offline_days", maxOffline)
                        apply()
                    }

                    if (registeredDevice != null && registeredDevice != currentDeviceId) {
                         _authState.value = AuthState.Error("Sesi dicabut. Hardware ID mismatch.")
                         return@addSnapshotListener
                    }

                    // 🚀 CHECK EXPIRY (Safety Lock)
                    if (expiryTimestamp != null && currentTime.after(expiryTimestamp.toDate())) {
                        _authState.value = AuthState.StatusWaiting("MASA BERLAKU HABIS.")
                        return@addSnapshotListener
                    }

                    when (status) {
                        "ACTIVE" -> {
                            if (expiryTimestamp == null) {
                                // 🛡️ Still blocks if someone manually deleted it
                                _authState.value = AuthState.StatusWaiting("MENUNGGU KONFIGURASI ADMIN.")
                            } else {
                                _authState.value = AuthState.Active(
                                    uid = uid,
                                    email = email,
                                    schoolName = schoolName,
                                    role = role,
                                    expiryMillis = expiryTimestamp.seconds * 1000,
                                    assignedClasses = assignedFromDb
                                )
                            }
                        }
                        "BANNED" -> _authState.value = AuthState.StatusWaiting("AKUN DIBEKUKAN.")
                        else -> _authState.value = AuthState.StatusWaiting("MENUNGGU AKTIVASI.\nID: $currentDeviceId")
                    }
                } else {
                    _authState.value = AuthState.LoggedOut
                }
            }
    }

    private fun checkOfflineGracePeriod(maxDays: Int) {
        val lastSync = prefs.getLong("last_sync_time", System.currentTimeMillis())
        val diffDays = (System.currentTimeMillis() - lastSync) / (1000 * 60 * 60 * 24)
        if (diffDays > maxDays) {
            _authState.value = AuthState.StatusWaiting("OFFLINE TERLALU LAMA. Hubungkan internet.")
        }
    }

    fun logout() {
        statusListener?.remove()
        auth.signOut()
        _authState.value = AuthState.LoggedOut
    }
}

sealed class AuthState {
    object Checking : AuthState()
    object LoggedOut : AuthState()
    object Loading : AuthState()
    data class Active(
        val uid: String,
        val email: String,
        val schoolName: String, 
        val role: String, 
        val expiryMillis: Long,
        val assignedClasses: List<String> 
    ) : AuthState()
    data class StatusWaiting(val message: String) : AuthState()
    data class Error(val message: String) : AuthState()
}