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
 * 📂 FaceRepository (V.6.6 - Strict Logic & Double Validation Ready)
 * Pusat kendali data siswa dan biometrik wajah dengan validasi ketat.
 */
class FaceRepository(private val application: Application) {
    
    private val db = AppDatabase.getInstance(application)
    private val faceDao = db.faceDao()

    // 1. Ambil Flow Data Wajah (Live Data untuk UI)
    fun getAllFacesFlow(): Flow<List<FaceEntity>> = faceDao.getAllFacesFlow()

    // 2. Ambil Data Wajah Berdasarkan Nama
    // Digunakan oleh ViewModel untuk memvalidasi hasil deteksi AI Scanner
    suspend fun getFaceByName(name: String): List<FaceEntity> = withContext(Dispatchers.IO) {
        try {
            faceDao.getFaceByName(name.trim())
        } catch (e: Exception) {
            Log.e("FaceRepo", "Error fetching face by name: ${e.message}")
            emptyList()
        }
    }

    // 3. Logika Registrasi & Update (Atomic Operation)
    suspend fun registerFace(
        studentId: String,
        sekolahId: String,
        name: String,
        embedding: FloatArray,
        units: List<MasterClassWithNames>,
        photoUrl: String?
    ) = withContext(Dispatchers.IO) {
        
        // Gabungkan list kelas menjadi CSV untuk kompatibilitas database lama
        val combinedClassName = units.joinToString(", ") { it.className }
        val primaryUnit = units.firstOrNull()

        val face = FaceEntity(
            studentId = studentId.trim(),
            sekolahId = sekolahId,
            name = name.trim(),
            photoUrl = photoUrl,
            embedding = embedding,
            className = combinedClassName,
            grade = primaryUnit?.gradeName ?: "",
            role = primaryUnit?.roleName ?: Constants.ROLE_USER,
            program = primaryUnit?.programName ?: "",
            subClass = primaryUnit?.subClassName ?: "",
            subGrade = primaryUnit?.subGradeName ?: "",
            timestamp = System.currentTimeMillis()
        )

        try {
            // Aksi 1: Simpan ke Local Room DB
            faceDao.insert(face)
            
            // Aksi 2: Upload ke Firestore Cloud
            FirestoreStudent.uploadStudent(face)
            
            // Aksi 3: Refresh Cache agar AI langsung mengenali wajah baru
            FaceCache.refresh(application)
            
            Log.d("FaceRepo", "✅ Success Register: ${face.name}")
        } catch (e: Exception) {
            Log.e("FaceRepo", "❌ Register Failed: ${e.message}")
            throw e // Re-throw agar UI bisa menangkap error
        }
    }

    // 4. Logika Smart Sync Siswa dari Firestore
    suspend fun syncStudents(user: UserEntity) = withContext(Dispatchers.IO) {
        val lastSync = faceDao.getLastSyncTimestamp() ?: 0L
        
        try {
            val students = FirestoreStudent.fetchSmartSyncStudents(
                sekolahId = user.sekolahId ?: "",
                assignedClasses = user.assignedClasses,
                role = user.role,
                lastSync = lastSync
            )
            
            if (students.isNotEmpty()) {
                faceDao.insertAll(students)
                // Wajib Refresh Cache setelah sinkronisasi massal
                FaceCache.refresh(application)
                Log.d("FaceRepo", "✅ Sync Complete: ${students.size} students added/updated")
            }
        } catch (e: Exception) {
            Log.e("FaceRepo", "❌ Sync Failed: ${e.message}")
        }
    }

    // 5. Cek Duplikasi Student ID (Primary Key Check)
    suspend fun getFaceByStudentId(studentId: String): FaceEntity? = withContext(Dispatchers.IO) {
        faceDao.getFaceByStudentId(studentId.trim())
    }

    // 6. Delete Student (Sync Local + Cloud)
    suspend fun deleteFace(studentId: String, face: FaceEntity) = withContext(Dispatchers.IO) {
        try {
            // Hapus dari Cloud dulu
            FirestoreStudent.deleteStudent(studentId.trim())
            
            // Hapus dari Local
            faceDao.delete(face)
            
            // Refresh Cache agar AI berhenti mengenali wajah ini
            FaceCache.refresh(application)
            
            Log.d("FaceRepo", "✅ Delete Success: $studentId")
        } catch (e: Exception) {
            Log.e("FaceRepo", "❌ Delete Failed: ${e.message}")
            throw e
        }
    }
}