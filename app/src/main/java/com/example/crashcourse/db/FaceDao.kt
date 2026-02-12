package com.example.crashcourse.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 👤 Azura Tech Face DAO (V.3 - Optimized for Many-to-Many)
 * Mengelola akses data biometrik mahasiswa/personel.
 */
@Dao
interface FaceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(face: FaceEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(faces: List<FaceEntity>)

    /**
     * 🧠 Digunakan oleh FaceCache untuk memuat semua data ke RAM saat aplikasi dimulai.
     */
    @Query("SELECT * FROM students")
    fun getAllFaces(): List<FaceEntity> 
    
    /**
     * 📱 Digunakan oleh UI untuk menampilkan daftar mahasiswa secara real-time.
     */
    @Query("SELECT * FROM students ORDER BY name ASC")
    fun getAllFacesFlow(): Flow<List<FaceEntity>>

    /**
     * 🔍 Mengambil data berdasarkan ID unik mahasiswa.
     */
    @Query("SELECT * FROM students WHERE studentId = :studentId LIMIT 1")
    suspend fun getFaceByStudentId(studentId: String): FaceEntity?
    
    /**
     * 🔎 Mencari nama mahasiswa (Full-text search sederhana).
     * Gunakan "%name%" saat memanggil fungsi ini.
     */
    @Query("SELECT * FROM students WHERE name LIKE :searchQuery")
    suspend fun getFaceByName(searchQuery: String): List<FaceEntity>

    /**
     * 🔥 FILTER MANY-TO-MANY (Optimized)
     * Mencari mahasiswa yang terdaftar di salah satu kelas dari daftar yang diberikan.
     * Digunakan agar Guru hanya melihat mahasiswa di mata kuliah yang ia ampu.
     */
    @Query("SELECT * FROM students WHERE className IN (:classes)")
    fun getFacesByClasses(classes: List<String>): Flow<List<FaceEntity>>

    /**
     * 🏫 Mengambil data mahasiswa berdasarkan Sekolah ID.
     */
    @Query("SELECT * FROM students WHERE sekolahId = :sekolahId")
    fun getFacesBySchool(sekolahId: String): Flow<List<FaceEntity>>

    /**
     * 🕒 Smart Sync: Mendapatkan waktu pembaruan terakhir di database lokal.
     * Digunakan untuk menarik hanya "data baru" (incremental sync) dari Firestore.
     */
    @Query("SELECT MAX(timestamp) FROM students")
    suspend fun getLastSyncTimestamp(): Long?

    @Query("DELETE FROM students WHERE sekolahId = :sekolahId")
    suspend fun deleteAllFacesBySchool(sekolahId: String)
    
    @Query("DELETE FROM students")
    suspend fun deleteAll()
    
    @Delete
    suspend fun delete(face: FaceEntity)
}