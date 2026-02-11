package com.example.crashcourse.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 🔑 Azura Tech User DAO
 * Handles local login session & sync metadata.
 */
@Dao
interface UserDao {

    // ------------------------------------------
    // INSERT / REPLACE
    // ------------------------------------------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    // ------------------------------------------
    // READ
    // ------------------------------------------
    @Query("SELECT * FROM current_user LIMIT 1")
    fun getCurrentUserFlow(): Flow<UserEntity?>

    @Query("SELECT * FROM current_user LIMIT 1")
    suspend fun getCurrentUser(): UserEntity?

    @Query("SELECT deviceId FROM current_user LIMIT 1")
    suspend fun getLocalDeviceId(): String?

    // ------------------------------------------
    // UPDATE
    // ------------------------------------------
    @Query("""
        UPDATE current_user
        SET lastSync = :timestamp
        WHERE uid = :uid
    """)
    suspend fun updateLastSync(uid: String, timestamp: Long)

    // ------------------------------------------
    // DELETE
    // ------------------------------------------
    @Query("DELETE FROM current_user")
    suspend fun deleteUser()
}
