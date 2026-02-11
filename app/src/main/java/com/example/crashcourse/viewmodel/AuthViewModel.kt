package com.example.crashcourse.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crashcourse.db.AppDatabase
import com.example.crashcourse.db.FaceCache
import com.example.crashcourse.db.UserEntity
import com.example.crashcourse.firestore.auth.FirestoreAuth // ✅ NEW IMPORT
import com.example.crashcourse.util.DeviceUtil
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*

/**
 * 🔐 Azura Tech Auth ViewModel
 * The Single Source of Truth for user sessions and multi-tenant security.
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = Firebase.auth
    // private val db = Firebase.firestore ❌ REMOVED (Replaced by FirestoreAuth)
    
    private val database = AppDatabase.getInstance(application)
    private val userDao = database.userDao()
    
    private val currentDeviceId = DeviceUtil.getUniqueDeviceId(application)
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

    // ... (resetError & sendPasswordReset remain unchanged) ...

    fun resetError() {
        _authState.value = AuthState.LoggedOut
    }

    fun sendPasswordReset(email: String) {
        if (email.isBlank()) return
        viewModelScope.launch {
            try {
                auth.sendPasswordResetEmail(email).await()
                Log.d("Auth", "Reset email sent to $email")
            } catch (e: Exception) {
                Log.e("Auth", "Gagal reset pass: ${e.message}")
            }
        }
    }

    /**
     * 🚀 REGISTER
     */
    fun register(email: String, pass: String, schoolName: String) {
        if (email.isBlank() || pass.isBlank() || schoolName.isBlank()) {
            _authState.value = AuthState.Error("Semua kolom wajib diisi.")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading("Mendaftarkan Sekolah...")
            try {
                // 1. Create Auth User
                val result = auth.createUserWithEmailAndPassword(email, pass).await()
                val uid = result.user?.uid ?: throw Exception("Gagal mendapatkan UID")
                
                // 2. Prepare Data
                val calendar = Calendar.getInstance().apply { 
                    add(Calendar.DAY_OF_YEAR, 30) 
                }
                val defaultExpiry = Timestamp(calendar.time)
                val sekolahId = "SCH-${System.currentTimeMillis().toString().takeLast(6)}"

                // 3. Save to Firestore via Repository ✅
                FirestoreAuth.createAdminAccount(
                    uid = uid,
                    email = email,
                    schoolName = schoolName,
                    sekolahId = sekolahId,
                    deviceId = currentDeviceId,
                    expiryDate = defaultExpiry
                )
                
                startListeningToUserStatus(uid)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Registrasi Gagal")
            }
        }
    }

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _authState.value = AuthState.Error("Email/Password kosong.")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading("Sedang masuk...")
            try {
                auth.signInWithEmailAndPassword(email, pass).await()
                val uid = auth.currentUser?.uid ?: return@launch
                startListeningToUserStatus(uid)
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Login Gagal: ${e.message}")
            }
        }
    }

    private fun startListeningToUserStatus(uid: String) {
        statusListener?.remove()
        
        // ✅ Use FirestoreAuth Listener
        statusListener = FirestoreAuth.listenToUserSession(uid) { snapshot, e ->
            if (e != null || snapshot == null || !snapshot.exists()) {
                _authState.value = AuthState.LoggedOut
                return@listenToUserSession
            }

            val status = snapshot.getString("status") ?: "PENDING"
            val cloudDeviceId = snapshot.getString("device_id")
            val expiryTimestamp = snapshot.getTimestamp("expiry_date")
            val expiryMillis = expiryTimestamp?.toDate()?.time ?: 0L
            val currentTime = System.currentTimeMillis()

            // 🔐 Security Checks
            if (!cloudDeviceId.isNullOrEmpty() && cloudDeviceId != currentDeviceId) {
                _authState.value = AuthState.Error("Akun terikat di perangkat lain.")
                logout() 
                return@listenToUserSession
            }

            when {
                status != "ACTIVE" -> {
                    _authState.value = AuthState.StatusWaiting("AKUN BELUM AKTIF.")
                }
                expiryMillis < currentTime -> {
                    _authState.value = AuthState.StatusWaiting("LISENSI EXPIRED.")
                }
                else -> {
                    syncUserToRoom(snapshot, uid, expiryMillis)
                }
            }
        }
    }

    private fun syncUserToRoom(snapshot: DocumentSnapshot, uid: String, expiryMillis: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sid = snapshot.getString("sekolahId") ?: "SCH-UNKNOWN"

                val user = UserEntity(
                    uid = uid,
                    email = snapshot.getString("email") ?: "",
                    deviceId = currentDeviceId,
                    sekolahId = sid,
                    name = snapshot.getString("school_name") ?: "No Name",
                    role = snapshot.getString("role") ?: "TEACHER",
                    assignedClasses = try {
                        (snapshot.get("assigned_classes") as? List<*>)?.map { it.toString() } ?: emptyList()
                    } catch (e: Exception) { emptyList() },
                    expiryMillis = expiryMillis
                )
                
                userDao.insertUser(user)

                withContext(Dispatchers.Main) {
                    _authState.value = AuthState.Active(
                        uid = user.uid,
                        email = user.email,
                        role = user.role,
                        schoolName = user.name,
                        sekolahId = sid,
                        expiryMillis = user.expiryMillis,
                        assignedClasses = user.assignedClasses
                    )
                }
            } catch (err: Exception) {
                withContext(Dispatchers.Main) { 
                    _authState.value = AuthState.Error("Sinkronisasi profil gagal.") 
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                statusListener?.remove()
                auth.signOut()
                prefs.edit().clear().apply()
                database.clearAllTables()
                FaceCache.clear()
                
                withContext(Dispatchers.Main) {
                    _authState.value = AuthState.LoggedOut
                }
            } catch (e: Exception) {
                Log.e("Auth", "Logout error: ${e.message}")
            }
        }
    }
}