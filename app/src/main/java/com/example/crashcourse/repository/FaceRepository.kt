package com.example.crashcourse.repository

import android.app.Application
import android.util.Log
import com.example.crashcourse.db.*
import com.example.crashcourse.firestore.student.FirestoreStudent
import com.example.crashcourse.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * 📂 FaceRepository (V.10.4 - Unified Identity Edition)
 * Jembatan antara Cloud, Room, dan RAM Cache.
 * Menggunakan schoolId tunggal untuk memastikan data tidak pernah bocor antar sekolah.
 */
class FaceRepository(private val application: Application) {
    
    private val db = AppDatabase.getInstance(application)
    private val faceDao = db.faceDao()

    // ==========================================
    // 🔍 1. READ OPERATIONS
    // ==========================================

    fun getAllFacesFlow(schoolId: String): Flow<List<FaceEntity>> {
        return faceDao.getAllFacesFlow(schoolId)
    }

    suspend fun getFaceByStudentId(studentId: String): FaceEntity? = withContext(Dispatchers.IO) {
        faceDao.getFaceByStudentId(studentId.trim())
    }

    suspend fun getStudentsByClass(className: String): List<FaceEntity> = withContext(Dispatchers.IO) {
        faceDao.getStudentsByClass(className)
    }

    // ==========================================
    // ✍️ 2. WRITE OPERATIONS (Enrollment)
    // ==========================================

    suspend fun registerFace(
        studentId: String,
        schoolId: String,
        name: String,
        embedding: FloatArray,
        units: List<MasterClassWithNames>,
        photoUrl: String?
    ) = withContext(Dispatchers.IO) {
        
        if (embedding.isEmpty()) {
            throw IllegalArgumentException("Data biometrik (embedding) tidak ditemukan!")
        }

        val enrolledClassList = units.map { it.className } 
        val primaryUnit = units.firstOrNull()

        val face = FaceEntity(
            studentId = studentId.trim(),
            schoolId = schoolId, // 🛡️ Stempel schoolId yang sah
            name = name.trim(),
            photoUrl = photoUrl,
            embedding = embedding,
            enrolledClasses = enrolledClassList, 
            grade = primaryUnit?.gradeName ?: "",
            subClass = primaryUnit?.subClassName ?: "",
            timestamp = System.currentTimeMillis()
        )

        try {
            // 1. Cloud First (Source of Truth)
            FirestoreStudent.uploadStudent(face)

            // 2. Local Persistence
            faceDao.insert(face)
            
            // 3. AI Update
            FaceCache.refresh(application)
            
            Log.d("FaceRepo", "✅ Registrasi Berhasil: ${face.name} ke School: $schoolId")
        } catch (e: Exception) {
            Log.e("FaceRepo", "❌ Registrasi Gagal", e)
            throw e 
        }
    }

    // ==========================================
    // 🔄 3. SMART SYNC (The Core Logic)
    // ==========================================

    /**
     * 🚀 SMART SYNC AZURA TECH
     * Menggunakan schoolId tunggal hasil reformasi database.
     */
    suspend fun syncStudents(user: UserEntity) = withContext(Dispatchers.IO) {
        // 🔥 FIXED: Menggunakan schoolId, bukan schoolId
        val targetSchoolId = user.schoolId 
        
        if (targetSchoolId.isBlank()) {
            Log.e("FaceRepo", "⚠️ Sync Aborted: schoolId User kosong!")
            return@withContext
        }
        
        // 🛡️ Cek jumlah data lokal untuk menentukan Full Sync atau Delta Sync
        val localCount = faceDao.getStudentCount(targetSchoolId)
        val lastSync = if (localCount == 0) 0L else faceDao.getLastSyncTimestamp(targetSchoolId) ?: 0L
        
        try {
            Log.d("FaceRepo", "🔄 Memulai Sync: Sekolah $targetSchoolId, LastSync: $lastSync")

            val remoteStudents = FirestoreStudent.fetchSmartSyncStudents(
                schoolId = targetSchoolId,
                assignedClasses = user.assignedClasses,
                role = user.role,
                lastSync = lastSync
            )
            
            if (remoteStudents.isNotEmpty()) {
                faceDao.insertAll(remoteStudents)
                FaceCache.refresh(application)
                Log.d("FaceRepo", "✅ Sync Berhasil: ${remoteStudents.size} jiwa baru masuk.")
            } else {
                Log.d("FaceRepo", "✅ Sinkronisasi Selesai: Data sudah up-to-date.")
            }
        } catch (e: Exception) {
            Log.e("FaceRepo", "❌ Sinkronisasi Gagal", e)
        }
    }

    // ==========================================
    // 🗑️ 4. DELETE OPERATIONS
    // ==========================================

    suspend fun deleteFace(studentId: String, face: FaceEntity) = withContext(Dispatchers.IO) {
        try {
            FirestoreStudent.deleteStudent(studentId.trim())
            faceDao.delete(face)
            FaceCache.refresh(application)
            Log.d("FaceRepo", "🗑️ Siswa dihapus: ${face.name}")
        } catch (e: Exception) {
            Log.e("FaceRepo", "❌ Gagal menghapus", e)
            throw e
        }
    }
}