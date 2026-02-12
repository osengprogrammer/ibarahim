package com.example.crashcourse.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crashcourse.db.AppDatabase
import com.example.crashcourse.db.UserEntity
import com.example.crashcourse.util.DeviceUtil
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.*

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = Firebase.auth
    private val db = FirebaseFirestore.getInstance()
    private val database = AppDatabase.getInstance(application)
    private val userDao = database.userDao()

    // Mendapatkan Device ID unik untuk fitur penguncian perangkat
    private val currentDeviceId = DeviceUtil.getUniqueDeviceId(application)
    
    private var statusListener: ListenerRegistration? = null
    private var syncJob: Job? = null

    // State utama aplikasi
    private val _authState = MutableStateFlow<AuthState>(AuthState.Checking)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkCurrentUser()
    }

    // ----------------------------------------------------
    // 1. SESSION CHECK (Saat Aplikasi Dibuka)
    // ----------------------------------------------------

    private fun checkCurrentUser() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            _authState.value = AuthState.Loading("Memulihkan Sesi...")
            // Cari tahu dulu ID dokumennya (Email atau UID)
            identifyAndListen(currentUser.uid, currentUser.email)
        } else {
            _authState.value = AuthState.LoggedOut
        }
    }

    // ----------------------------------------------------
    // 2. LOGIN (Email & Password)
    // ----------------------------------------------------

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _authState.value = AuthState.Error("Email dan Password tidak boleh kosong.")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading("Menghubungkan ke Azura Cloud...")
            try {
                // 1. Login Authentication
                auth.signInWithEmailAndPassword(email, pass).await()
                val user = auth.currentUser ?: throw Exception("User null setelah login")
                
                // 2. Tentukan Dokumen User & Mulai Listen
                identifyAndListen(user.uid, user.email)

            } catch (e: Exception) {
                Log.e("AzuraAuth", "Login Gagal", e)
                _authState.value = AuthState.Error("Login Gagal: Periksa Email/Password.")
            }
        }
    }

    // ----------------------------------------------------
    // 3. REGISTER (SMART CHECK: STAFF VS ADMIN)
    // ----------------------------------------------------

    fun register(email: String, pass: String, schoolNameInput: String) {

        viewModelScope.launch {
            _authState.value = AuthState.Loading("Memeriksa status undangan...")

            try {
                // 1️⃣ CEK APAKAH USER INI ADALAH STAFF YANG DIUNDANG?
                // Kita cari dokumen dengan ID = Email
                val invitationSnapshot = db.collection("users").document(email).get().await()

                if (invitationSnapshot.exists()) {
                    // ==========================================
                    // 🟢 KASUS A: STAFF / GURU (Sudah Diundang)
                    // ==========================================
                    Log.d("AuthRegister", "User ditemukan sebagai Staff Invite. Melakukan aktivasi.")
                    registerAsStaff(email, pass, invitationSnapshot)

                } else {
                    // ==========================================
                    // 🔵 KASUS B: ADMIN BARU (Sekolah Baru)
                    // ==========================================
                    Log.d("AuthRegister", "User baru. Mendaftar sebagai Admin.")
                    registerAsNewAdmin(email, pass, schoolNameInput)
                }

            } catch (e: Exception) {
                Log.e("AuthRegister", "Error Register", e)
                _authState.value = AuthState.Error(e.message ?: "Gagal memproses pendaftaran.")
            }
        }
    }

    // --- LOGIKA REGISTER STAFF (MENGAKTIFKAN UNDANGAN) ---
    private suspend fun registerAsStaff(email: String, pass: String, snapshot: DocumentSnapshot) {
        try {
            _authState.value = AuthState.Loading("Mengaktifkan Akun Staff...")

            // 1. Buat User di Firebase Authentication
            val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
            val uid = authResult.user?.uid ?: throw Exception("Gagal membuat UID Auth.")

            // 2. Siapkan Data Update (Menyesuaikan struktur Admin)
            val updates = hashMapOf<String, Any>(
                "uid" to uid,                    // Isi UID dari Auth
                "device_id" to currentDeviceId,  // Bind ke HP ini
                "isRegistered" to true,          // Tandai sudah registrasi
                "status" to "ACTIVE"             // Ubah dari PENDING ke ACTIVE
                // Field lain (role, sekolahId, assigned_classes) BIARKAN TETAP (jangan ditimpa)
            )

            // 3. Update Dokumen Firestore (ID: Email)
            db.collection("users").document(email).update(updates).await()

            // 4. Auto Login & Listen
            Log.d("AuthRegister", "Staff $email berhasil diaktivasi.")
            startListeningToUserStatus(docId = email) // Listen ke dokumen Email

        } catch (e: Exception) {
            // Jika email sudah terdaftar di Auth tapi data belum update
            if (e.message?.contains("email address is already in use") == true) {
                _authState.value = AuthState.Error("Email sudah terdaftar. Silakan Login.")
            } else {
                throw e
            }
        }
    }

    // --- LOGIKA REGISTER ADMIN BARU ---
    private suspend fun registerAsNewAdmin(email: String, pass: String, schoolNameInput: String) {
        try {
            _authState.value = AuthState.Loading("Mendaftarkan Sekolah Baru...")

            // 1. Buat User Auth
            val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
            val uid = authResult.user?.uid ?: throw Exception("UID Gagal")

            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, 30) // Trial 30 Hari

            // 2. Buat Data Admin Baru
            val data = hashMapOf(
                "uid" to uid,
                "email" to email,
                "role" to "ADMIN",
                "school_name" to schoolNameInput, // Pakai nama sekolah dari input
                "sekolahId" to "SCH-${System.currentTimeMillis().toString().takeLast(6)}",
                "device_id" to currentDeviceId,
                "status" to "PENDING", // Admin baru butuh approval Super Admin? Atau langsung ACTIVE?
                "isRegistered" to true,
                "assigned_classes" to listOf<String>(),
                "expiry_date" to Timestamp(calendar.time),
                "created_at" to System.currentTimeMillis()
            )

            // 3. Simpan dengan ID = UID (Standar Admin)
            db.collection("users").document(uid).set(data).await()

            // 4. Listen
            startListeningToUserStatus(docId = uid)

        } catch (e: Exception) {
            throw e
        }
    }

    // ----------------------------------------------------
    // 4. IDENTIFY DOCUMENT ID (CRITICAL FIX)
    // ----------------------------------------------------
    
    /**
     * Mencari tahu apakah data user disimpan dengan ID = Email (Staff) 
     * atau ID = UID (Admin), lalu memasang Listener.
     */
    private fun identifyAndListen(uid: String, email: String?) {
        viewModelScope.launch {
            try {
                // SKENARIO A: Cek apakah dokumen ada di users/{email} (Untuk Staff/Guru)
                if (email != null) {
                    val emailDoc = db.collection("users").document(email).get().await()
                    if (emailDoc.exists()) {
                        Log.d("AzuraAuth", "User ditemukan via Email ID: $email")
                        startListeningToUserStatus(docId = email)
                        return@launch
                    }
                }

                // SKENARIO B: Cek apakah dokumen ada di users/{uid} (Untuk Admin)
                val uidDoc = db.collection("users").document(uid).get().await()
                if (uidDoc.exists()) {
                    Log.d("AzuraAuth", "User ditemukan via UID: $uid")
                    startListeningToUserStatus(docId = uid)
                    return@launch
                }

                // Jika tidak ditemukan di keduanya
                Log.e("AzuraAuth", "Data user tidak ditemukan di database")
                _authState.value = AuthState.Error("Akun terdaftar di Auth tapi data profil hilang.")
                auth.signOut()

            } catch (e: Exception) {
                _authState.value = AuthState.Error("Gagal mengambil data profil: ${e.message}")
            }
        }
    }

    // ----------------------------------------------------
    // 5. REALTIME LISTENER
    // ----------------------------------------------------

    private fun startListeningToUserStatus(docId: String) {
        // Hapus listener lama jika ada
        statusListener?.remove()

        statusListener = db.collection("users")
            .document(docId)
            .addSnapshotListener { snapshot, e ->

                if (e != null) {
                    _authState.value = AuthState.Error("Koneksi cloud terputus.")
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    // Berikan UID asli dari Auth agar sinkronisasi Room tetap konsisten
                    val authUid = auth.currentUser?.uid ?: docId
                    processUserSnapshot(snapshot, authUid)
                } else {
                    _authState.value = AuthState.LoggedOut
                }
            }
    }

    // ----------------------------------------------------
    // 6. SNAPSHOT PROCESS (LOGIC FIX)
    // ----------------------------------------------------

    private fun processUserSnapshot(snapshot: DocumentSnapshot, uid: String) {
        try {
            // Ambil data mentah
            val status = snapshot.getString("status") // "Active", "PENDING", dll
            val isRegistered = snapshot.getBoolean("isRegistered") ?: false
            val cloudDeviceId = snapshot.getString("device_id")

            // --- A. CEK SECURITY DEVICE LOCK ---
            if (cloudDeviceId.isNullOrEmpty()) {
                // Jika belum ada device_id, kunci ke HP ini
                snapshot.reference.update("device_id", currentDeviceId)
            } else if (cloudDeviceId != currentDeviceId) {
                // Jika login di HP lain
                _authState.value = AuthState.Error("Akun sedang digunakan di perangkat lain.")
                logout() // Auto logout
                return
            }

            // --- B. CEK STATUS AKTIF (FLEXIBLE LOGIC) ---
            // Akun dianggap aktif jika:
            // 1. Field 'status' mengandung kata "Active" atau "Approved" (Case insensitive)
            // 2. ATAU field 'isRegistered' bernilai true (Legacy data teacher)
            val isActiveStatus = status?.equals("Active", ignoreCase = true) == true || 
                                 status?.equals("APPROVED", ignoreCase = true) == true ||
                                 status?.equals("ACTIVE", ignoreCase = true) == true
            
            val isAccountActive = isActiveStatus || isRegistered

            if (isAccountActive) {
                // Hitung expiry (default 1 tahun jika null)
                val expiry = snapshot.getTimestamp("expiry_date")?.toDate()?.time
                    ?: (System.currentTimeMillis() + 31536000000L)

                // Lanjut sinkronisasi ke database lokal
                syncUserToRoom(snapshot, uid, expiry)
            } else {
                // Jika status Pending atau Rejected
                val msg = if (status != null) "Status Akun: $status" else "Menunggu Verifikasi Admin."
                _authState.value = AuthState.StatusWaiting(msg)
            }

        } catch (e: Exception) {
            Log.e("AzuraAuth", "Error processing snapshot", e)
            _authState.value = AuthState.Error("Format data akun tidak valid.")
        }
    }

    // ----------------------------------------------------
    // 7. SYNC TO ROOM (LOCAL DB)
    // ----------------------------------------------------

    private fun syncUserToRoom(
        snapshot: DocumentSnapshot,
        uid: String,
        expiryMillis: Long
    ) {
        // Batalkan proses sync sebelumnya jika ada
        syncJob?.cancel()

        syncJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("AzuraAuth", "Mulai Sinkronisasi Room...")

                // Ambil assigned_classes dengan aman (handle null atau format salah)
                val rawClasses = snapshot.get("assigned_classes")
                val safeClasses: List<String> = when (rawClasses) {
                    is List<*> -> rawClasses.map { it.toString() }
                    is String -> listOf(rawClasses) // Jaga-jaga jika admin salah input string tunggal
                    else -> emptyList()
                }

                val userEntity = UserEntity(
                    uid = uid,
                    sekolahId = snapshot.getString("sekolahId") ?: "SCH-UNKNOWN",
                    deviceId = currentDeviceId,
                    name = snapshot.getString("school_name") ?: "Azura User",
                    email = snapshot.getString("email") ?: "",
                    role = snapshot.getString("role") ?: "TEACHER",
                    assignedClasses = safeClasses,
                    expiryMillis = expiryMillis,
                    lastSync = System.currentTimeMillis()
                )

                // Simpan ke Local DB
                userDao.insertUser(userEntity)

                Log.d("AzuraAuth", "Sync Sukses. Mengubah State ke Active.")

                withContext(Dispatchers.Main) {
                    _authState.value = AuthState.Active(
                        uid = userEntity.uid,
                        email = userEntity.email,
                        role = userEntity.role,
                        schoolName = userEntity.name,
                        sekolahId = userEntity.sekolahId ?: "",
                        expiryMillis = userEntity.expiryMillis,
                        assignedClasses = userEntity.assignedClasses
                    )
                }

            } catch (e: Exception) {
                Log.e("AzuraAuth", "Gagal Sync Room", e)
                withContext(Dispatchers.Main) {
                    _authState.value = AuthState.Error("Gagal menyimpan data profil lokal.")
                }
            }
        }
    }

    // ----------------------------------------------------
    // 8. LOGOUT & RESET
    // ----------------------------------------------------

    fun logout() {
        syncJob?.cancel()
        statusListener?.remove()
        statusListener = null

        _authState.value = AuthState.LoggedOut

        viewModelScope.launch(Dispatchers.IO) {
            try {
                auth.signOut()
                database.clearAllTables() // Hapus data lokal agar bersih
                Log.d("AzuraAuth", "Logout berhasil & DB lokal dibersihkan.")
            } catch (e: Exception) {
                Log.e("AzuraAuth", "Logout Error", e)
            }
        }
    }

    fun sendPasswordReset(email: String) = viewModelScope.launch {
        try {
            auth.sendPasswordResetEmail(email).await()
        } catch (e: Exception) {
            Log.e("AzuraAuth", "Reset Pass Error", e)
        }
    }

    fun resetError() {
        _authState.value = AuthState.LoggedOut
    }
}