package com.example.crashcourse.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FaceDao {
    // --- CRUD Dasar & Sync Logic ---

    /**
     * Logika UPSERT: Jika studentId sudah ada, data lama akan ditimpa dengan data baru dari Cloud.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(face: FaceEntity)

    /**
     * 🚀 BATCH UPSERT: Digunakan oleh SyncOrchestrator untuk memasukkan banyak data 
     * sekaligus dari Firestore dengan performa tinggi.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(faces: List<FaceEntity>)

    @Delete
    suspend fun delete(face: FaceEntity)

    @Update
    suspend fun update(face: FaceEntity)

    // --- 🛠️ LOGIKA SINKRONISASI (SMART SYNC) ---

    /**
     * 🧹 CLEANUP: Menghapus data lokal yang ID-nya sudah tidak ada di list Cloud.
     * Ini memastikan "Source of Truth" tetap di Firestore.
     * studentId != '' ditambahkan untuk menjaga agar record kosong tidak terhapus.
     */
    @Query("DELETE FROM faces WHERE studentId NOT IN (:cloudIds) AND studentId != ''")
    suspend fun deleteOrphanedRecords(cloudIds: List<String>)

    /**
     * ⏱️ INCREMENTAL SYNC: Mendapatkan timestamp terbaru dari data lokal.
     * Berguna untuk bertanya ke Firestore: "Ada data baru sejak waktu X ini tidak?"
     */
    @Query("SELECT MAX(timestamp) FROM faces")
    suspend fun getLastSyncTimestamp(): Long?

    // --- Query Semua Data ---
    @Query("SELECT * FROM faces ORDER BY timestamp DESC")
    fun getAllFacesFlow(): Flow<List<FaceEntity>>

    @Query("SELECT * FROM faces")
    suspend fun getAllFaces(): List<FaceEntity>

    // --- Pencarian Spesifik ---
    @Query("SELECT * FROM faces WHERE studentId = :studentId")
    suspend fun getFaceByStudentId(studentId: String): FaceEntity?

    @Query("SELECT * FROM faces WHERE name LIKE '%' || :query || '%'")
    fun searchFacesByName(query: String): Flow<List<FaceEntity>>

    @Query("SELECT * FROM faces WHERE name = :name LIMIT 1")
    suspend fun getFaceByName(name: String): FaceEntity?

    // --- 📊 Filter Berdasarkan Kategori Utama (Strings) ---
    @Query("SELECT * FROM faces WHERE className = :className")
    fun getFacesByClassName(className: String): Flow<List<FaceEntity>>

    @Query("SELECT * FROM faces WHERE grade = :grade")
    fun getFacesByGrade(grade: String): Flow<List<FaceEntity>>

    @Query("SELECT * FROM faces WHERE program = :program")
    fun getFacesByProgram(program: String): Flow<List<FaceEntity>>

    @Query("SELECT * FROM faces WHERE role = :role")
    fun getFacesByRole(role: String): Flow<List<FaceEntity>>

    // --- 🎯 Filter Berdasarkan Kategori Baru (IDs & Sub-Categories) ---
    @Query("SELECT * FROM faces WHERE subClassId = :subClassId")
    fun getFacesBySubClassId(subClassId: Int): Flow<List<FaceEntity>>

    @Query("SELECT * FROM faces WHERE subGradeId = :subGradeId")
    fun getFacesBySubGradeId(subGradeId: Int): Flow<List<FaceEntity>>

    @Query("SELECT * FROM faces WHERE roleId = :roleId")
    fun getFacesByRoleId(roleId: Int): Flow<List<FaceEntity>>

    @Query("SELECT * FROM faces WHERE classId = :classId AND subClassId = :subClassId")
    fun getFacesByClassAndSubClass(classId: Int, subClassId: Int): Flow<List<FaceEntity>>

    // --- Utilitas ---
    @Query("DELETE FROM faces")
    suspend fun deleteAllFaces()

    @Query("SELECT COUNT(*) FROM faces")
    suspend fun getFaceCount(): Int
}