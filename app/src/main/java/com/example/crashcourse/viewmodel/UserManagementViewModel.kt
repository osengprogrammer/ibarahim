package com.example.crashcourse.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crashcourse.db.AppDatabase
import com.example.crashcourse.firestore.user.FirestoreUser // ✅ NEW IMPORT
import com.example.crashcourse.firestore.user.UserProfile // ✅ NEW IMPORT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// State Management
sealed class UserListState {
    object Idle : UserListState()
    object Loading : UserListState()
    data class Success(val users: List<UserProfile>) : UserListState()
    data class Error(val message: String) : UserListState()
}

/**
 * 👥 User Management ViewModel
 * Handles loading staff list via FirestoreUser Repository.
 */
class UserManagementViewModel(application: Application) : AndroidViewModel(application) {

    private val userDao = AppDatabase.getInstance(application).userDao()
    
    private val _uiState = MutableStateFlow<UserListState>(UserListState.Idle)
    val uiState = _uiState.asStateFlow()

    init {
        fetchUsers()
    }

    fun fetchUsers() {
        viewModelScope.launch {
            _uiState.value = UserListState.Loading
            
            try {
                // 1. Ambil Identitas Admin yang sedang Login dari Room
                val currentAdmin = withContext(Dispatchers.IO) {
                    userDao.getCurrentUser()
                }

                if (currentAdmin == null) {
                    _uiState.value = UserListState.Error("Sesi Admin tidak ditemukan. Silakan login ulang.")
                    return@launch
                }

                val mySekolahId = currentAdmin.sekolahId ?: ""
                Log.d("UserVM", "Mengambil staff untuk sekolah: $mySekolahId")

                // 2. Query Firestore via Repository (FirestoreUser)
                val userList = withContext(Dispatchers.IO) {
                    FirestoreUser.fetchUsersBySchool(mySekolahId)
                }
                
                // 3. Update UI State
                if (userList.isEmpty()) {
                    _uiState.value = UserListState.Error("Belum ada staff terdaftar.")
                } else {
                    _uiState.value = UserListState.Success(userList)
                }
                
            } catch (e: Exception) {
                Log.e("UserVM", "Error fetching users", e)
                _uiState.value = UserListState.Error("Gagal mengambil data: ${e.message}")
            }
        }
    }
}