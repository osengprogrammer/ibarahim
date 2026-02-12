package com.example.crashcourse.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crashcourse.db.AppDatabase
import com.example.crashcourse.firestore.user.FirestoreUser
import com.example.crashcourse.firestore.user.UserProfile
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.HashMap // ✅ Wajib Import Java HashMap

// State untuk UI
sealed class UserListState {
    object Idle : UserListState()
    object Loading : UserListState()
    data class Success(val users: List<UserProfile>) : UserListState()
    data class Error(val message: String) : UserListState()
}

// State Khusus untuk Operasi Simpan
sealed class SaveState {
    object Idle : SaveState()
    object Loading : SaveState()
    object Success : SaveState()
    data class Error(val message: String) : SaveState()
}

class UserManagementViewModel(application: Application) : AndroidViewModel(application) {

    private val userDao = AppDatabase.getInstance(application).userDao()
    
    // State untuk List User
    private val _uiState = MutableStateFlow<UserListState>(UserListState.Idle)
    val uiState = _uiState.asStateFlow()

    // State untuk Proses Simpan (Scope)
    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState = _saveState.asStateFlow()

    init {
        fetchUsers()
    }

    /**
     * 📥 FETCH USERS
     */
    fun fetchUsers() {
        viewModelScope.launch {
            _uiState.value = UserListState.Loading
            try {
                val currentAdmin = withContext(Dispatchers.IO) { userDao.getCurrentUser() }
                if (currentAdmin == null) {
                    _uiState.value = UserListState.Error("Sesi Admin tidak ditemukan.")
                    return@launch
                }

                val mySekolahId = currentAdmin.sekolahId ?: ""
                val userList = withContext(Dispatchers.IO) {
                    FirestoreUser.fetchUsersBySchool(mySekolahId)
                }
                
                if (userList.isEmpty()) {
                    _uiState.value = UserListState.Error("Belum ada staff terdaftar.")
                } else {
                    _uiState.value = UserListState.Success(userList)
                }
            } catch (e: Exception) {
                _uiState.value = UserListState.Error("Gagal: ${e.message}")
            }
        }
    }

    /**
     * 🚀 INVITE STAFF (VERSI ANTI-ERROR)
     * Menggunakan HashMap manual (bukan hashMapOf) untuk menghindari error Serializable vs Pair.
     */
    fun inviteStaff(email: String, role: String) {
        viewModelScope.launch {
            try {
                val currentAdmin = withContext(Dispatchers.IO) { userDao.getCurrentUser() }
                if (currentAdmin == null) return@launch

                val inheritedSekolahId = currentAdmin.sekolahId ?: ""
                val inheritedSchoolName = currentAdmin.name ?: ""

                val calendar = Calendar.getInstance()
                calendar.add(Calendar.YEAR, 1) 

                // 🔥 SOLUSI UTAMA: Buat HashMap kosong, lalu isi satu per satu.
                // Ini memaksa compiler menerima tipe data apa pun (Any) tanpa error.
                val inviteData = HashMap<String, Any>()
                
                inviteData["email"] = email
                inviteData["role"] = role
                inviteData["sekolahId"] = inheritedSekolahId
                inviteData["school_name"] = inheritedSchoolName
                inviteData["status"] = "PENDING"
                inviteData["isRegistered"] = false
                inviteData["uid"] = ""
                inviteData["device_id"] = ""
                // List kosong aman dimasukkan sebagai Any
                inviteData["assigned_classes"] = emptyList<String>() 
                inviteData["expiry_date"] = Timestamp(calendar.time)
                inviteData["created_at"] = System.currentTimeMillis()

                // Kirim ke Repository
                withContext(Dispatchers.IO) {
                    FirestoreUser.inviteStaffByMap(email, inviteData)
                }
                
                Log.d("UserVM", "Berhasil invite $email")
                fetchUsers() // Refresh list agar UI update

            } catch (e: Exception) {
                Log.e("UserVM", "Gagal invite: ${e.message}")
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
                withContext(Dispatchers.IO) {
                    FirestoreUser.updateUserScope(docId, classes)
                }
                _saveState.value = SaveState.Success
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