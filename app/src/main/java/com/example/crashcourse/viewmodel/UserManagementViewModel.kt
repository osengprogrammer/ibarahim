package com.example.crashcourse.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.crashcourse.utils.Constants // 🚀 Import Constants
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

// 💡 Sebaiknya pindahkan data class ini ke package 'model' jika proyek makin besar
data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val role: String = "USER", // Default USER
    val schoolName: String = "",
    val assignedClasses: List<String> = emptyList()
)

// State Management agar UI bisa menampilkan Loading/Error
sealed class UserListState {
    object Idle : UserListState()
    object Loading : UserListState()
    data class Success(val users: List<UserProfile>) : UserListState()
    data class Error(val message: String) : UserListState()
}

class UserManagementViewModel : ViewModel() {
    
    private val db = FirebaseFirestore.getInstance()
    
    // Menggunakan State Sealed Class yang lebih informatif
    private val _uiState = MutableStateFlow<UserListState>(UserListState.Idle)
    val uiState = _uiState.asStateFlow()

    init {
        fetchUsers()
    }

    fun fetchUsers() {
        viewModelScope.launch {
            _uiState.value = UserListState.Loading
            
            try {
                // 🚀 Pindah ke IO Thread untuk Network & Mapping
                val userList = withContext(Dispatchers.IO) {
                    val snapshot = db.collection(Constants.COLL_USERS).get().await()
                    
                    snapshot.documents.map { doc ->
                        // 🛡️ Safe Parsing: Menangani kemungkinan null data
                        UserProfile(
                            uid = doc.id,
                            email = doc.getString("email") ?: "No Email",
                            role = doc.getString("role") ?: "USER",
                            schoolName = doc.getString("school_name") ?: "",
                            
                            // 🛡️ Safe Casting: Pastikan List tidak crash
                            assignedClasses = try {
                                (doc.get("assigned_classes") as? List<*>)?.map { it.toString() } ?: emptyList()
                            } catch (e: Exception) {
                                emptyList()
                            }
                        )
                    }
                }
                
                _uiState.value = UserListState.Success(userList)
                
            } catch (e: Exception) {
                Log.e("UserVM", "Error fetching users", e)
                _uiState.value = UserListState.Error(e.message ?: "Gagal mengambil data user")
            }
        }
    }
}