package com.example.crashcourse.repository

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import com.example.crashcourse.db.*
import com.example.crashcourse.firestore.student.FirestoreStudent
import com.example.crashcourse.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.crashcourse.ml.nativeutils.NativeMath

/**
 * 📝 RegisterRepository (V.10.3 - Migration Fixed)
 * Menangani pendaftaran massal (Bulk Register) dan proses AI di background.
 */
class RegisterRepository(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val faceDao = db.faceDao()
    private val userDao = db.userDao()

    companion object {
        private const val TAG = "RegisterRepo"
    }

    /**
     * 🔐 Ambil ID Sekolah yang sedang aktif dari database User.
     */
    suspend fun getCurrentSchoolId(): String? = withContext(Dispatchers.IO) {
        userDao.getCurrentUser()?.schoolId // FIX: Ganti dari sekolahId
    }

    /**
     * ⚙️ CORE LOGIC: Proses AI & Registrasi Siswa
     */
    suspend fun processAndRegisterStudent(
        student: CsvImportUtils.CsvStudentData,
        schoolId: String, // FIX: Ganti dari sekolahId
        existingFacesCache: List<FaceEntity>
    ): ProcessResult = withContext(Dispatchers.IO) {
        try {
            // 1. Cek Primary Key (Mencegah ID Duplikat)
            if (faceDao.getFaceByStudentId(student.studentId) != null) {
                return@withContext ProcessResult(student.studentId, student.name, status = "Duplicate ID", isSuccess = false)
            }

            // 2. Photo & AI Embedding
            val photoResult = BulkPhotoProcessor.processPhotoSource(context, student.photoUrl, student.studentId)
            if (!photoResult.success) {
                return@withContext ProcessResult(student.studentId, student.name, status = "Photo Error", error = photoResult.error)
            }

            val bitmap = BitmapFactory.decodeFile(photoResult.localPhotoUrl) 
                ?: return@withContext ProcessResult(student.studentId, student.name, status = "Decode Error")
            
            val embeddingResult = PhotoProcessingUtils.processBitmapForFaceEmbedding(context, bitmap) 
                ?: return@withContext ProcessResult(student.studentId, student.name, status = "No Face Detected")

            val (faceBitmap, embedding) = embeddingResult

            // 3. AI Anti-Duplicate (Mencegah satu orang daftar dua kali dengan ID beda)
            for (face in existingFacesCache) {
                // Menggunakan Cosine Distance (Threshold 0.45f)
                if (NativeMath.cosineDistance(face.embedding, embedding) < 0.45f) {
                    return@withContext ProcessResult(student.studentId, student.name, status = "Face matched with ${face.name}")
                }
            }

            // 4. Konversi Data ke Entity Baru
            val path = PhotoStorageUtils.saveFacePhoto(context, faceBitmap, student.studentId)
            
            // Konversi String CSV Matkul ke List<String>
            val classList = student.className?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() } ?: listOf("Umum")

            val faceEntity = FaceEntity(
                studentId = student.studentId,
                schoolId = schoolId, // FIX: Naming consistency
                name = student.name,
                photoUrl = path ?: "",
                embedding = embedding,
                enrolledClasses = classList // FIX: Menggunakan List<String>
            )
            
            // 5. Save & Sync
            faceDao.insert(faceEntity)
            FirestoreStudent.uploadStudent(faceEntity)

            ProcessResult(
                student.studentId, 
                student.name, 
                status = "Registered", 
                isSuccess = true, 
                photoSize = photoResult.processedSize
            )

        } catch (e: Exception) {
            Log.e(TAG, "❌ Gagal memproses ${student.name}", e)
            ProcessResult(student.studentId, student.name, status = "System Error", error = e.message)
        }
    }

    /**
     * Mengambil data siswa lokal berdasarkan sekolah aktif.
     */
    suspend fun getAllLocalFaces(schoolId: String) = withContext(Dispatchers.IO) { 
        faceDao.getFacesBySchool(schoolId) 
    }
    
    /**
     * Memperbarui RAM Cache AI.
     */
    suspend fun refreshFaceCache() = withContext(Dispatchers.IO) {
        FaceCache.refresh(context)
    }
}