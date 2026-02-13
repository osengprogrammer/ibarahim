package com.example.crashcourse.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crashcourse.firestore.user.FirestoreUser
import com.example.crashcourse.repository.UserRepository
import com.example.crashcourse.ui.management.SaveState     
import com.example.crashcourse.ui.management.UserListState 
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.HashMap

/**
 * 👨‍💼 UserManagementViewModel (V.6.0 - Anti-Stuck & Migration Ready)
 * Mengelola daftar staff, undangan, dan pembaruan scope kelas.
 */
class UserManagementViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepo = UserRepository(application)
    
    private val _uiState = MutableStateFlow<UserListState>(UserListState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState = _saveState.asStateFlow()

    init {
        fetchUsers()
    }

    /**
     * 📥 FETCH USERS
     * Mengambil daftar staff sekolah dari Firestore.
     */
    fun fetchUsers() {
        viewModelScope.launch {
            _uiState.value = UserListState.Loading
            try {
                val currentAdmin = userRepo.getCurrentUser() 
                
                if (currentAdmin == null) {
                    _uiState.value = UserListState.Error("Sesi Admin tidak ditemukan.")
                    return@launch
                }

                val mySekolahId = currentAdmin.sekolahId ?: ""
                val userList = FirestoreUser.fetchUsersBySchool(mySekolahId)
                
                if (userList.isEmpty()) {
                    _uiState.value = UserListState.Error("Belum ada staff terdaftar.")
                } else {
                    _uiState.value = UserListState.Success(userList)
                }
            } catch (e: Exception) {
                Log.e("UserVM", "Fetch Error", e)
                _uiState.value = UserListState.Error("Gagal mengambil data.")
            }
        }
    }

    /**
     * 🚀 INVITE STAFF (Updated for Migration Strategy)
     * Membuat dokumen awal dengan ID Email sebagai "umpan" migrasi ke UID.
     */
    fun inviteStaff(email: String, role: String) {
        viewModelScope.launch {
            try {
                val currentAdmin = userRepo.getCurrentUser() ?: return@launch

                // 🔥 Normalisasi email agar Document ID konsisten (lowercase)
                val normalizedEmail = email.lowercase().trim()
                val inheritedSekolahId = currentAdmin.sekolahId ?: ""
                val inheritedSchoolName = currentAdmin.name ?: ""

                val calendar = Calendar.getInstance().apply { add(Calendar.YEAR, 1) }

                val inviteData = HashMap<String, Any>().apply {
                    put("email", normalizedEmail)
                    put("role", role)
                    put("sekolahId", inheritedSekolahId)
                    put("school_name", inheritedSchoolName)
                    put("status", "ACTIVE") // 🔥 Set ACTIVE agar tidak stuck di 'Status Waiting'
                    put("isRegistered", false)
                    put("uid", "") // Akan diisi otomatis oleh AuthViewModel saat login/migrasi
                    put("device_id", "")
                    put("assigned_classes", emptyList<String>())
                    put("expiry_date", Timestamp(calendar.time))
                    put("created_at", System.currentTimeMillis())
                }

                // Simpan dokumen dengan ID = Email
                FirestoreUser.inviteStaffByMap(normalizedEmail, inviteData)
                
                Log.d("UserVM", "✅ Berhasil invite: $normalizedEmail. Menunggu migrasi saat login.")
                fetchUsers() 

            } catch (e: Exception) {
                Log.e("UserVM", "❌ Gagal invite: ${e.message}")
            }
        }
    }

    /**
     * 💾 SAVE USER SCOPE
     * Memperbarui daftar kelas. docId bisa berupa Email (sebelum migrasi) atau UID (setelah migrasi).
     */
    fun saveUserScope(docId: String, classes: List<String>) {
        viewModelScope.launch {
            _saveState.value = SaveState.Loading
            try {
                FirestoreUser.updateUserScope(docId, classes)
                _saveState.value = SaveState.Success
                
                // Refresh data lokal agar perubahan langsung terlihat di UI
                fetchUsers() 
            } catch (e: Exception) {
                Log.e("UserVM", "Gagal simpan scope", e)
                _saveState.value = SaveState.Error(e.message ?: "Gagal menyimpan perubahan.")
            }
        }
    }
    
    fun resetSaveState() {
        _saveState.value = SaveState.Idle
    }
}