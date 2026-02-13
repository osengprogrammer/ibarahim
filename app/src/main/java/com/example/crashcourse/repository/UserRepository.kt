package com.example.crashcourse.repository

import android.app.Application
import com.example.crashcourse.db.AppDatabase
import com.example.crashcourse.db.UserEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

class UserRepository(application: Application) {
    private val userDao = AppDatabase.getInstance(application).userDao()

    // Ambil data user sekali jalan (untuk keperluan logika di Repository/VM)
    suspend fun getCurrentUser(): UserEntity? {
        return userDao.getCurrentUser()
    }

    // Pantau data user secara real-time (untuk UI State di VM)
    fun getCurrentUserFlow(): Flow<UserEntity?> {
        return userDao.getCurrentUserFlow().distinctUntilChanged()
    }
}