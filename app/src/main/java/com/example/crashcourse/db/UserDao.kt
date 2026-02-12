package com.example.crashcourse.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 🔑 Azura Tech User DAO
 * Single Source of Truth untuk sesi login lokal.
 * Strategi: REPLACE digunakan untuk memastikan transisi antar akun mulus.
 */
@Dao
interface UserDao {

    // ------------------------------------------
    // 📥 INSERT / REPLACE (Kunci Utama Fix Stuck)
    // ------------------------------------------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    /**
     * Fungsi pembantu untuk memastikan database bersih sebelum diisi user baru.
     * Digunakan saat proses login untuk mencegah residu data akun lama.
     */
    @Transaction
    suspend fun replaceCurrentUser(user: UserEntity) {
        deleteUser()
        insertUser(user)
    }

    // ------------------------------------------
    // 🔍 READ (Data Retrieval)
    // ------------------------------------------
    
    @Query("SELECT * FROM current_user LIMIT 1")
    fun getCurrentUserFlow(): Flow<UserEntity?>

    @Query("SELECT * FROM current_user LIMIT 1")
    suspend fun getCurrentUser(): UserEntity?

    @Query("SELECT deviceId FROM current_user LIMIT 1")
    suspend fun getLocalDeviceId(): String?

    @Query("SELECT sekolahId FROM current_user LIMIT 1")
    suspend fun getSekolahId(): String?

    // ------------------------------------------
    // 🆙 UPDATE (Metadata Sync)
    // ------------------------------------------
    @Query("""
        UPDATE current_user
        SET lastSync = :timestamp
        WHERE uid = :uid
    """)
    suspend fun updateLastSync(uid: String, timestamp: Long)

    // ------------------------------------------
    // 🗑️ DELETE (Logout / Cleanup)
    // ------------------------------------------
    @Query("DELETE FROM current_user")
    suspend fun deleteUser()
}