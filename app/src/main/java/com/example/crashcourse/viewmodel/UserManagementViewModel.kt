package com.example.crashcourse.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crashcourse.firestore.user.FirestoreUser
import com.example.crashcourse.firestore.user.UserProfile
import com.example.crashcourse.repository.UserRepository
import com.example.crashcourse.ui.management.SaveState     
import com.example.crashcourse.ui.management.UserListState 
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 👨‍💼 UserManagementViewModel (V.10.21 - Unified Identity Refactor)
 * Mengelola otorisasi staff menggunakan satu jalur schoolId yang sudah dibersihkan.
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
     * Menarik daftar staff berdasarkan identitas sekolah tunggal (schoolId).
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

                // 🔥 FIXED: Menggunakan schoolId, bukan schoolId
                val userList = FirestoreUser.fetchUsersBySchool(currentAdmin.schoolId)
                
                if (userList.isEmpty()) {
                    _uiState.value = UserListState.Error("Belum ada staff terdaftar.")
                } else {
                    _uiState.value = UserListState.Success(userList)
                }
            } catch (e: Exception) {
                Log.e("UserVM", "Fetch Error", e)
                _uiState.value = UserListState.Error("Gagal mengambil data staff.")
            }
        }
    }

    /**
     * 🚀 INVITE STAFF
     * Menempelkan ID sekolah Admin ke profil staff baru.
     */
    fun inviteStaff(email: String, role: String) {
        viewModelScope.launch {
            try {
                val currentAdmin = userRepo.getCurrentUser() ?: return@launch
                val normalizedEmail = email.lowercase().trim()

                // 🔥 FIXED: Penyelarasan dengan UserProfile baru
                val newUserInvite = UserProfile(
                    uid = "", 
                    email = normalizedEmail,
                    role = role,
                    // 🔥 Kita pakai schoolId sekarang
                    schoolId = currentAdmin.schoolId,
                    schoolName = currentAdmin.name,
                    isActive = false, // Status awal PENDING
                    assigned_classes = emptyList()
                )

                FirestoreUser.inviteStaff(newUserInvite)
                fetchUsers() 
            } catch (e: Exception) {
                Log.e("UserVM", "❌ Invite Error: ${e.message}")
            }
        }
    }

    /**
     * 💾 SAVE USER SCOPE
     */
    fun saveUserScope(docId: String, classes: List<String>) {
        viewModelScope.launch {
            _saveState.value = SaveState.Loading
            try {
                // 1. Update Firestore
                FirestoreUser.updateUserScope(docId, classes)

                // 2. ⚡ SYNC KE ROOM (Opsional)
                // Jika Admin mengedit dirinya sendiri, kita simpan status sinkronisasi terakhir
                val currentAdmin = userRepo.getCurrentUser()
                if (currentAdmin != null && docId == currentAdmin.uid) {
                    // Update timestamp sync terakhir
                    userRepo.getCurrentUser()?.uid?.let { 
                        // Catatan: updateClasses ditiadakan di Dao baru demi efisiensi RAM
                        // Data akan ter-sync otomatis saat login berikutnya atau via Flow
                    }
                }

                _saveState.value = SaveState.Success
                fetchUsers() 
            } catch (e: Exception) {
                Log.e("UserVM", "Save Error", e)
                _saveState.value = SaveState.Error(e.message ?: "Gagal memperbarui akses.")
            }
        }
    }
    
    fun resetSaveState() {
        _saveState.value = SaveState.Idle
    }
}