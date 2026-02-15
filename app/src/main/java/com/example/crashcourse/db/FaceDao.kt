package com.example.crashcourse.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 👤 Azura Tech Face DAO (V.10.24 - Unified Sync Edition)
 * Dioptimalkan untuk sinkronisasi Cloud-Local menggunakan identitas schoolId tunggal.
 */
@Dao
interface FaceDao {

    // ==========================================
    // 💾 INSERT & UPDATE
    // ==========================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(face: FaceEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(faces: List<FaceEntity>)

    // ==========================================
    // 🧠 AI ENGINE LOADERS (Gallery Pipeline)
    // ==========================================

    @Query("SELECT * FROM students WHERE schoolId = :schoolId")
    suspend fun getFacesBySchool(schoolId: String): List<FaceEntity>

    /**
     * 🔥 FIXED: Mengambil schoolId (bukan schoolId) dari tabel user.
     * Ini adalah kunci agar AI Gallery terisi otomatis tanpa data kosong.
     */
    @Query("SELECT schoolId FROM current_user LIMIT 1")
    suspend fun getSchoolIdFromSession(): String?

    /**
     * 🧠 SMART LOADER
     * Mengambil gallery biometrik secara instan berdasarkan identitas user yang login.
     */
    @Transaction
    suspend fun getMyStudentsForAI(): List<FaceEntity> {
        val currentSchoolId = getSchoolIdFromSession() ?: return emptyList()
        return getFacesBySchool(currentSchoolId)
    }

    // ==========================================
    // 🕒 SYNC & SAFETY UTILS 
    // ==========================================

    @Query("SELECT COUNT(*) FROM students WHERE schoolId = :schoolId")
    suspend fun getStudentCount(schoolId: String): Int 

    @Query("SELECT MAX(timestamp) FROM students WHERE schoolId = :schoolId")
    suspend fun getLastSyncTimestamp(schoolId: String): Long?

    // ==========================================
    // 📱 UI FLOWS & SEARCH
    // ==========================================

    @Query("SELECT * FROM students WHERE schoolId = :schoolId ORDER BY name ASC")
    fun getAllFacesFlow(schoolId: String): Flow<List<FaceEntity>>

    @Query("SELECT * FROM students WHERE studentId = :studentId LIMIT 1")
    suspend fun getFaceByStudentId(studentId: String): FaceEntity?
    
    @Query("SELECT * FROM students WHERE enrolledClasses LIKE '%' || :className || '%'")
    suspend fun getStudentsByClass(className: String): List<FaceEntity>

    @Query("SELECT * FROM students WHERE name LIKE '%' || :searchQuery || '%' AND schoolId = :schoolId")
    suspend fun searchFaceByName(searchQuery: String, schoolId: String): List<FaceEntity>

    // ==========================================
    // 🗑️ CLEANUP (LOGOUT & RESET)
    // ==========================================

    @Query("DELETE FROM students WHERE schoolId = :schoolId")
    suspend fun deleteFacesBySchool(schoolId: String)
    
    @Delete
    suspend fun delete(face: FaceEntity)

    /**
     * ✅ Wajib ada agar UserRepository.clearAllData() berfungsi total saat Logout.
     */
    @Query("DELETE FROM students")
    suspend fun deleteAll()
}