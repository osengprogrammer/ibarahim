package com.example.crashcourse.ui // Sesuai folder

import com.example.crashcourse.firestore.user.UserProfile

sealed class SyncState {
    object Idle : SyncState()
    data class Loading(val message: String, val progress: Float = 0f) : SyncState()
    data class Success(val message: String) : SyncState()
    data class Error(val message: String) : SyncState()
}