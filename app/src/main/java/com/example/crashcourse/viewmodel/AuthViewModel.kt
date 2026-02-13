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

/**
 * 🔐 AuthViewModel (V.7.5)
 * Mengelola siklus hidup autentikasi, pendaftaran, dan migrasi dokumen (Email -> UID).
 */
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
                val user = auth.currentUser ?: throw Exception("User tidak ditemukan")
                identifyAndListen(user.uid, user.email)
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Login Gagal: Periksa kembali email/password.")
            }
        }
    }

    // ==========================================
    // 📝 REGISTER LOGIC (Fix Unresolved Reference)
    // ==========================================

    fun register(email: String, pass: String, schoolNameInput: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading("Memeriksa status pendaftaran...")
            try {
                // Cek apakah email ini ada di daftar undangan staff
                val invitationSnapshot = FirestoreAuth.getInvitationByEmail(email)

                if (invitationSnapshot != null && invitationSnapshot.exists()) {
                    // JALUR 1: AKTIVASI STAFF
                    registerAsStaff(email, pass)
                } else {
                    // JALUR 2: PENDAFTARAN ADMIN BARU
                    if (schoolNameInput.isBlank()) {
                        _authState.value = AuthState.Error("Nama Sekolah wajib diisi untuk pendaftaran baru.")
                        return@launch
                    }
                    registerAsNewAdmin(email, pass, schoolNameInput)
                }
            } catch (e: Exception) {
                Log.e("AuthRegister", "Error Register", e)
                _authState.value = AuthState.Error(e.message ?: "Gagal memproses pendaftaran.")
            }
        }
    }

    private suspend fun registerAsStaff(email: String, pass: String) {
        try {
            _authState.value = AuthState.Loading("Mengaktifkan Akun Staff...")
            val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
            val uid = authResult.user?.uid ?: throw Exception("Gagal membuat UID.")

            // Aktivasi di Firestore (Masih menggunakan ID Email sebagai identitas awal)
            FirestoreAuth.activateStaffAccount(uid, email, currentDeviceId)

            // Setelah sukses, arahkan ke identifyAndListen untuk proses MIGRASI ke UID
            identifyAndListen(uid, email)
        } catch (e: Exception) {
            handleAuthError(e)
        }
    }

    private suspend fun registerAsNewAdmin(email: String, pass: String, schoolNameInput: String) {
        try {
            _authState.value = AuthState.Loading("Mendaftarkan Sekolah Baru...")
            val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
            val uid = authResult.user?.uid ?: throw Exception("UID Gagal")

            val calendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 30) }
            val sekolahId = "SCH-${System.currentTimeMillis().toString().takeLast(6)}"

            // Buat akun admin (ID dokumen langsung menggunakan UID)
            FirestoreAuth.createAdminAccount(
                uid = uid,
                email = email,
                schoolName = schoolNameInput,
                sekolahId = sekolahId,
                deviceId = currentDeviceId,
                expiryDate = Timestamp(calendar.time)
            )

            identifyAndListen(uid, email)
        } catch (e: Exception) {
            handleAuthError(e)
        }
    }

    // ==========================================
    // 🚀 CORE LOGIC: IDENTIFY & MIGRATE
    // ==========================================
    
    private fun identifyAndListen(uid: String, email: String?) {
        viewModelScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val userRef = db.collection("users")

                // 1. PRIORITAS: Cek apakah dokumen UID sudah ada
                val uidDoc = userRef.document(uid).get().await()
                if (uidDoc.exists()) {
                    Log.d("AzuraAuth", "✅ UID Document found.")
                    startListeningToUserStatus(docId = uid)
                    return@launch
                }

                // 2. LEGACY CHECK: Jika UID tidak ada, cari berdasarkan ID Email (Aktivasi Staff)
                if (email != null) {
                    val normalizedEmail = email.lowercase().trim()
                    val emailDoc = userRef.document(normalizedEmail).get().await()
                    
                    if (emailDoc.exists()) {
                        _authState.value = AuthState.Loading("Menyiapkan akun...")
                        
                        // 🚀 MIGRASI: Ubah ID dokumen dari Email menjadi UID
                        val newDocId = migrateUserDocument(normalizedEmail, uid)
                        
                        startListeningToUserStatus(docId = newDocId)
                        return@launch
                    }
                }

                _authState.value = AuthState.Error("Profil tidak ditemukan. Hubungi Admin.")
                
            } catch (e: Exception) {
                Log.e("AzuraAuth", "❌ identifyAndListen failed", e)
                _authState.value = AuthState.Error("Gagal sinkronisasi profil.")
            }
        }
    }

    private suspend fun migrateUserDocument(oldDocId: String, newUid: String): String {
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users")
        
        return try {
            val oldDoc = userRef.document(oldDocId).get().await()
            if (oldDoc.exists()) {
                val data = oldDoc.data?.toMutableMap() ?: mutableMapOf()
                
                data["uid"] = newUid
                data["isRegistered"] = true
                data["status"] = "ACTIVE"

                // Tulis dokumen baru (ID = UID), Hapus dokumen lama (ID = Email)
                userRef.document(newUid).set(data).await()
                userRef.document(oldDocId).delete().await()
                
                Log.d("AzuraAuth", "✅ Migrasi Berhasil: $oldDocId -> $newUid")
                newUid 
            } else {
                oldDocId
            }
        } catch (e: Exception) {
            Log.e("AzuraAuth", "❌ Migrasi Gagal", e)
            oldDocId 
        }
    }

    // ==========================================
    // 🎧 REALTIME LISTENER & SYNC
    // ==========================================

    private fun startListeningToUserStatus(docId: String) {
        statusListener?.remove()
        statusListener = FirebaseFirestore.getInstance()
            .collection("users").document(docId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _authState.value = AuthState.Error("Cloud disconnected.")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    processUserSnapshot(snapshot, auth.currentUser?.uid ?: docId)
                } else {
                    _authState.value = AuthState.LoggedOut
                }
            }
    }

    private fun processUserSnapshot(snapshot: DocumentSnapshot, uid: String) {
        try {
            val profile = snapshot.toObject(UserProfile::class.java) ?: return
            val rawStatus = snapshot.getString("status")?.trim() ?: "PENDING"

            val cloudDeviceId = snapshot.getString("device_id")
            if (cloudDeviceId.isNullOrEmpty()) {
                snapshot.reference.update("device_id", currentDeviceId)
            } else if (cloudDeviceId != currentDeviceId) {
                _authState.value = AuthState.Error("Akun aktif di perangkat lain.")
                logout()
                return
            }

            val isAccountActive = rawStatus.equals("ACTIVE", ignoreCase = true) || profile.isRegistered

            if (isAccountActive) {
                val expiry = snapshot.getTimestamp("expiry_date")?.toDate()?.time
                    ?: (System.currentTimeMillis() + 31536000000L)
                syncUserToRoom(profile, uid, expiry)
            } else {
                _authState.value = AuthState.StatusWaiting("Status: $rawStatus")
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Error("Data profil corrupt.")
        }
    }

    private fun syncUserToRoom(profile: UserProfile, uid: String, expiryMillis: Long) {
        syncJob?.cancel()
        syncJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val userEntity = UserEntity(
                    uid = uid,
                    sekolahId = profile.sekolahId,
                    deviceId = currentDeviceId,
                    name = if (profile.role == "ADMIN") profile.schoolName else profile.email.split("@")[0],
                    email = profile.email,
                    role = profile.role,
                    assignedClasses = profile.assigned_classes, 
                    expiryMillis = expiryMillis,
                    lastSync = System.currentTimeMillis()
                )

                database.userDao().insertUser(userEntity)

                withContext(Dispatchers.Main) {
                    _authState.value = AuthState.Active(
                        uid = uid,
                        email = profile.email,
                        role = profile.role,
                        schoolName = profile.schoolName,
                        sekolahId = profile.sekolahId,
                        expiryMillis = expiryMillis,
                        assignedClasses = profile.assigned_classes
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { _authState.value = AuthState.Error("Sync lokal gagal.") }
            }
        }
    }

    fun logout() {
        syncJob?.cancel()
        statusListener?.remove()
        statusListener = null
        _authState.value = AuthState.LoggedOut
        viewModelScope.launch(Dispatchers.IO) {
            auth.signOut()
            database.clearAllTables()
        }
    }

    private fun handleAuthError(e: Exception) {
        val msg = if (e.message?.contains("already in use") == true) "Email sudah terdaftar." 
                  else e.message ?: "Terjadi kesalahan."
        _authState.value = AuthState.Error(msg)
    }
}