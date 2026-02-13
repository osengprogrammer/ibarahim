package com.example.crashcourse.ui.management

import com.example.crashcourse.firestore.user.UserProfile

// State untuk UI List
sealed class UserListState {
    object Idle : UserListState()
    object Loading : UserListState()
    data class Success(val users: List<UserProfile>) : UserListState()
    data class Error(val message: String) : UserListState()
}

// State untuk Operasi Simpan
sealed class SaveState {
    object Idle : SaveState()
    object Loading : SaveState()
    object Success : SaveState()
    data class Error(val message: String) : SaveState()
}