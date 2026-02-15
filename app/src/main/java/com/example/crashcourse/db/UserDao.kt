package com.example.crashcourse.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 🗝️ UserDao (V.10.19 - Optimized Identity)
 * Fokus pada satu sumber kebenaran: Identitas User & SchoolId.
 * Semua logika "Active Context" (pilihan kelas/mapel) dipindah ke RAM (ViewModel).
 */
@Dao
interface UserDao {

    // ==========================================
    // 💾 WRITE OPERATIONS
    // ==========================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    /**
     * 🔥 THE CLEAN SESSION STRATEGY
     * Menjamin hanya ada satu user yang login di HP ini.
     */
    @Transaction
    suspend fun replaceCurrentUser(user: UserEntity) {
        deleteAll() 
        insertUser(user) 
    }

    // ==========================================
    // 🔍 READ OPERATIONS
    // ==========================================

    @Query("SELECT * FROM current_user LIMIT 1")
    fun getCurrentUserFlow(): Flow<UserEntity?>

    @Query("SELECT * FROM current_user LIMIT 1")
    suspend fun getCurrentUser(): UserEntity?

    /**
     * Langsung ambil schoolId untuk stempel siswa.
     */
    @Query("SELECT schoolId FROM current_user LIMIT 1")
    suspend fun getSchoolId(): String?

    // ==========================================
    // 🆙 UPDATE OPERATIONS (Minimalist)
    // ==========================================

    @Query("UPDATE current_user SET schoolId = :newSchoolId WHERE uid = :uid")
    suspend fun updateSchoolId(uid: String, newSchoolId: String)

    @Query("UPDATE current_user SET lastSync = :timestamp WHERE uid = :uid")
    suspend fun updateLastSync(uid: String, timestamp: Long)

    // ==========================================
    // 🗑️ DELETE OPERATIONS
    // ==========================================
    
    @Query("DELETE FROM current_user")
    suspend fun deleteAll()
}