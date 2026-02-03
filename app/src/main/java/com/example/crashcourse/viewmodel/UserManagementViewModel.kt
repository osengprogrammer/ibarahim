package com.example.crashcourse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val role: String = "USER",
    val schoolName: String = "",
    val assignedClasses: List<String> = emptyList()
)

class UserManagementViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val _users = MutableStateFlow<List<UserProfile>>(emptyList())
    val users = _users.asStateFlow()

    init { fetchUsers() }

    fun fetchUsers() {
        viewModelScope.launch {
            try {
                val snapshot = db.collection("users").get().await()
                val list = snapshot.documents.map { doc ->
                    UserProfile(
                        uid = doc.id,
                        email = doc.getString("email") ?: "",
                        role = doc.getString("role") ?: "USER",
                        schoolName = doc.getString("school_name") ?: "",
                        assignedClasses = doc.get("assigned_classes") as? List<String> ?: emptyList()
                    )
                }
                _users.value = list
            } catch (e: Exception) { /* Log Error */ }
        }
    }
}