package com.example.crashcourse.repository

import android.content.Context
import android.graphics.BitmapFactory
import com.example.crashcourse.db.*
import com.example.crashcourse.firestore.student.FirestoreStudent
import com.example.crashcourse.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RegisterRepository(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val faceDao = db.faceDao()
    private val userDao = db.userDao()

    suspend fun getCurrentSekolahId(): String? = withContext(Dispatchers.IO) {
        userDao.getCurrentUser()?.sekolahId
    }

    suspend fun processAndRegisterStudent(
        student: CsvImportUtils.CsvStudentData,
        sekolahId: String,
        existingFacesCache: List<FaceEntity>
    ): ProcessResult = withContext(Dispatchers.IO) {
        // 1. Cek Primary Key
        if (faceDao.getFaceByStudentId(student.studentId) != null) {
            return@withContext ProcessResult(student.studentId, student.name, status = "Duplicate ID", isSuccess = false)
        }

        // 2. Photo & AI Embedding
        val photoResult = BulkPhotoProcessor.processPhotoSource(context, student.photoUrl, student.studentId)
        if (!photoResult.success) return@withContext ProcessResult(student.studentId, student.name, status = "Photo Error", error = photoResult.error)

        val bitmap = BitmapFactory.decodeFile(photoResult.localPhotoUrl) ?: return@withContext ProcessResult(student.studentId, student.name, status = "Decode Error")
        val embeddingResult = PhotoProcessingUtils.processBitmapForFaceEmbedding(context, bitmap) ?: return@withContext ProcessResult(student.studentId, student.name, status = "No Face Detected")

        val (faceBitmap, embedding) = embeddingResult

        // 3. AI Anti-Duplicate
        for (face in existingFacesCache) {
            if (NativeMath.cosineDistance(face.embedding, embedding) < 0.45f) {
                return@withContext ProcessResult(student.studentId, student.name, status = "Duplicate Face: ${face.name}")
            }
        }

        // 4. Save & Sync
        val path = PhotoStorageUtils.saveFacePhoto(context, faceBitmap, student.studentId)
        val faceEntity = FaceEntity(
            studentId = student.studentId,
            sekolahId = sekolahId,
            name = student.name,
            photoUrl = path ?: "",
            embedding = embedding,
            className = student.className ?: "Umum"
        )
        
        faceDao.insert(faceEntity)
        FirestoreStudent.uploadStudent(faceEntity)

        ProcessResult(student.studentId, student.name, status = "Registered", isSuccess = true, photoSize = photoResult.processedSize)
    }

    suspend fun getAllLocalFaces() = withContext(Dispatchers.IO) { faceDao.getAllFaces() }
    
    // 🔥 FIX: FaceCache.refresh adalah suspend function
    suspend fun refreshFaceCache() = withContext(Dispatchers.IO) {
        FaceCache.refresh(context)
    }
}