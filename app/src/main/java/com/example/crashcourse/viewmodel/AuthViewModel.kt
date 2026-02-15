package com.example.crashcourse.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crashcourse.db.AppDatabase
import com.example.crashcourse.db.UserEntity
import com.example.crashcourse.firestore.auth.FirestoreAuth
import com.example.crashcourse.firestore.user.UserProfile
import com.example.crashcourse.util.DeviceUtil
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.*

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = Firebase.auth
    private val database = AppDatabase.getInstance(application)
    private val currentDeviceId = DeviceUtil.getUniqueDeviceId(application)
    
    private var statusListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var syncJob: Job? = null

    private val _authState = MutableStateFlow<AuthState>(AuthState.Checking)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            _authState.value = AuthState.Loading("Memulihkan Sesi...")
            identifyAndListen(currentUser.uid, currentUser.email)
        } else {
            _authState.value = AuthState.LoggedOut
        }
    }

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _authState.value = AuthState.Error("Email dan Password wajib diisi.")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading("Otentikasi...")
            try {
                auth.signInWithEmailAndPassword(email, pass).await()
                val user = auth.currentUser ?: throw Exception("Sesi tidak valid.")
                identifyAndListen(user.uid, user.email)
            } catch (e: Exception) {
                Log.e("AuthVM", "Login Error", e)
                _authState.value = AuthState.Error("Login Gagal: Periksa Email/Password.")
            }
        }
    }

    // --- REGISTER LOGIC ---
    fun register(email: String, pass: String, schoolNameInput: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading("Memproses...")
            try {
                val invitationSnapshot = FirestoreAuth.getInvitationByEmail(email)
                if (invitationSnapshot != null && invitationSnapshot.exists()) {
                    registerAsStaff(email, pass)
                } else {
                    if (schoolNameInput.isBlank()) {
                        _authState.value = AuthState.Error("Nama Sekolah wajib diisi.")
                        return@launch
                    }
                    registerAsNewAdmin(email, pass, schoolNameInput)
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Gagal daftar.")
            }
        }
    }

    private suspend fun registerAsNewAdmin(email: String, pass: String, schoolNameInput: String) {
        try {
            val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
            val uid = authResult.user?.uid ?: throw Exception("UID Gagal")
            val calendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 30) }
            val generatedSchoolId = "SCH-${System.currentTimeMillis().toString().takeLast(6)}"

            FirestoreAuth.createAdminAccount(
                uid = uid, email = email, schoolName = schoolNameInput,
                sekolahId = generatedSchoolId, deviceId = currentDeviceId,
                expiryDate = Timestamp(calendar.time)
            )
            identifyAndListen(uid, email)
        } catch (e: Exception) { handleAuthError(e) }
    }

    private suspend fun registerAsStaff(email: String, pass: String) {
        try {
            val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
            val uid = authResult.user?.uid ?: throw Exception("UID Gagal")
            FirestoreAuth.activateStaffAccount(uid, email, currentDeviceId)
            identifyAndListen(uid, email)
        } catch (e: Exception) { handleAuthError(e) }
    }

    // --- SYNC CORE ---
    private fun identifyAndListen(uid: String, email: String?) {
        viewModelScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val userRef = db.collection("users")
                val uidDoc = userRef.document(uid).get().await()
                
                if (uidDoc.exists()) {
                    startListeningToUserStatus(uid)
                } else if (email != null) {
                    val normalizedEmail = email.lowercase().trim()
                    val emailDoc = userRef.document(normalizedEmail).get().await()
                    if (emailDoc.exists()) {
                        val newId = migrateUserDocument(normalizedEmail, uid)
                        startListeningToUserStatus(newId)
                    } else {
                        _authState.value = AuthState.Error("Profil tidak ditemukan.")
                    }
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Identifikasi Gagal.")
            }
        }
    }

    private suspend fun migrateUserDocument(oldDocId: String, newUid: String): String {
        val userRef = FirebaseFirestore.getInstance().collection("users")
        val oldDoc = userRef.document(oldDocId).get().await()
        val data = oldDoc.data?.toMutableMap() ?: mutableMapOf()
        data["uid"] = newUid
        data["isRegistered"] = true
        data["status"] = "ACTIVE"
        userRef.document(newUid).set(data).await()
        userRef.document(oldDocId).delete().await()
        return newUid
    }

    private fun startListeningToUserStatus(docId: String) {
        statusListener?.remove()
        statusListener = FirebaseFirestore.getInstance()
            .collection("users").document(docId)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                processUserSnapshot(snapshot, auth.currentUser?.uid ?: docId)
            }
    }

    private fun processUserSnapshot(snapshot: DocumentSnapshot, uid: String) {
        try {
            val profile = snapshot.toObject(UserProfile::class.java) ?: return
            
            // 🔥 LOGIKA "SAPU JAGAT" UNTUK STATUS ACTIVE
            val rawStatus = snapshot.getString("status") ?: ""
            val boolActive = snapshot.getBoolean("isActive") ?: false
            val isRegistered = snapshot.getBoolean("isRegistered") ?: false
            
            // Bypass verifikasi jika status ACTIVE (String) atau isActive (Boolean)
            val isAccountActive = rawStatus.equals("ACTIVE", ignoreCase = true) || boolActive || isRegistered

            Log.d("AuthVM", "🔍 Status Check: String=$rawStatus, Bool=$boolActive, Result=$isAccountActive")

            val cloudDeviceId = snapshot.getString("device_id") ?: ""
            if (cloudDeviceId.isNotEmpty() && cloudDeviceId != currentDeviceId) {
                _authState.value = AuthState.Error("Akun aktif di HP lain.")
                logout()
                return
            }

            if (isAccountActive) {
                val expiry = snapshot.getTimestamp("expiry_date")?.toDate()?.time
                    ?: (System.currentTimeMillis() + 31536000000L)
                syncUserToRoom(profile, uid, expiry, isAccountActive)
            } else {
                _authState.value = AuthState.StatusWaiting("Menunggu persetujuan Admin.")
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Error("Gagal memproses profil.")
        }
    }

    private fun syncUserToRoom(profile: UserProfile, uid: String, expiryMillis: Long, active: Boolean) {
        syncJob?.cancel()
        syncJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val userEntity = UserEntity(
                    uid = uid,
                    name = if (profile.role == "ADMIN") profile.schoolName else profile.email,
                    schoolId = profile.schoolId,
                    role = profile.role,
                    isActive = active,
                    expiryMillis = expiryMillis,
                    assignedClasses = profile.assigned_classes,
                    lastSync = System.currentTimeMillis()
                )
                database.userDao().replaceCurrentUser(userEntity)

                withContext(Dispatchers.Main) {
                    _authState.value = AuthState.Active(
                        uid = uid, email = profile.email, role = profile.role,
                        schoolName = profile.schoolName, schoolId = profile.schoolId,
                        expiryMillis = expiryMillis, assignedClasses = profile.assigned_classes
                    )
                }
            } catch (e: Exception) {
                Log.e("AuthVM", "Room Error", e)
            }
        }
    }

    fun logout() {
        statusListener?.remove()
        _authState.value = AuthState.LoggedOut
        viewModelScope.launch(Dispatchers.IO) {
            auth.signOut()
            database.userDao().deleteAll()
        }
    }

    private fun handleAuthError(e: Exception) {
        _authState.value = AuthState.Error(e.message ?: "Auth Gagal.")
    }
}