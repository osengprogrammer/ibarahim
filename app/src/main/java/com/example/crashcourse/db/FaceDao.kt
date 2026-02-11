package com.example.crashcourse.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 👤 Azura Tech Face DAO
 * Mengelola akses data biometrik dengan sinkronisasi konstan.
 * Query menggunakan nama tabel "students" secara eksplisit untuk menjamin validasi KSP.
 */
@Dao
interface FaceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(face: FaceEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(faces: List<FaceEntity>)

    /**
     * 🧠 Dipakai oleh FaceCache (RAM Loading)
     * Mengambil semua data untuk diolah ke dalam memory.
     */
    @Query("SELECT * FROM students")
    fun getAllFaces(): List<FaceEntity> 
    
    /**
     * 📱 Dipakai oleh FaceListScreen (Reactive UI)
     * Memantau perubahan data secara real-time menggunakan Flow.
     */
    @Query("SELECT * FROM students ORDER BY timestamp DESC")
    fun getAllFacesFlow(): Flow<List<FaceEntity>>

    /**
     * 🔍 Pencarian ID tunggal untuk keperluan verifikasi.
     */
    @Query("SELECT * FROM students WHERE studentId = :studentId LIMIT 1")
    suspend fun getFaceByStudentId(studentId: String): FaceEntity?
    
    /**
     * 🔎 Fitur Search Bar: Mencari nama siswa berdasarkan string input.
     */
    @Query("SELECT * FROM students WHERE name LIKE :searchQuery")
    suspend fun getFaceByName(searchQuery: String): List<FaceEntity>

    /**
     * 🏫 Scoped Deletion: Menghapus data hanya untuk sekolah tertentu (Multi-tenancy).
     */
    @Query("DELETE FROM students WHERE sekolahId = :sekolahId")
    suspend fun deleteAllFacesBySchool(sekolahId: String)
    
    /**
     * 🧨 Wipe Out: Membersihkan seluruh data lokal untuk resync total.
     */
    @Query("DELETE FROM students")
    suspend fun deleteAll()
    
    @Delete
    suspend fun delete(face: FaceEntity)
    
    /**
     * 🕒 Smart Sync Engine: Mengambil timestamp terakhir untuk sinkronisasi delta/incremental.
     * Return type Long? menangani kondisi database kosong (null).
     */
    @Query("SELECT MAX(timestamp) FROM students")
    suspend fun getLastSyncTimestamp(): Long?
}