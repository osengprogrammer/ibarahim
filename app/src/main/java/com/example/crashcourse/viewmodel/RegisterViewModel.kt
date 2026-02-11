package com.example.crashcourse.viewmodel

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.crashcourse.db.AppDatabase
import com.example.crashcourse.db.FaceCache
import com.example.crashcourse.db.FaceDao
import com.example.crashcourse.db.FaceEntity
import com.example.crashcourse.firestore.student.FirestoreStudent // ✅ NEW IMPORT
import com.example.crashcourse.utils.BulkPhotoProcessor
import com.example.crashcourse.utils.CsvImportUtils
import com.example.crashcourse.utils.NativeMath
import com.example.crashcourse.utils.PhotoProcessingUtils
import com.example.crashcourse.utils.PhotoStorageUtils
import com.example.crashcourse.utils.ProcessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 📊 Azura Tech Register ViewModel
 * Handles bulk student registration via CSV with automated photo processing
 * and AI-based visual duplicate detection.
 */
data class ProcessingState(
    val isProcessing: Boolean = false,
    val status: String = "Menunggu CSV...",
    val progress: Float = 0f,
    val estimatedTime: String = "",
    val results: List<ProcessResult> = emptyList(),
    val successCount: Int = 0,
    val duplicateCount: Int = 0,
    val errorCount: Int = 0,
    val currentPhotoType: String = "",
    val currentPhotoSize: String = ""
)

class RegisterViewModel : ViewModel() {
    private val _state = MutableStateFlow(ProcessingState())
    val state: StateFlow<ProcessingState> = _state.asStateFlow()

    /**
     * Estimates processing time based on the photo sources in the CSV.
     */
    fun prepareProcessing(context: Context, uri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val csvResult = CsvImportUtils.parseCsvFile(context, uri)
                    val photoSources = csvResult.students.map { it.photoUrl }
                    val seconds = BulkPhotoProcessor.estimateProcessingTime(photoSources)
                    val estimate = when {
                        seconds > 120 -> "${seconds / 60} menit"
                        seconds > 60 -> "1 menit ${seconds % 60} detik"
                        else -> "$seconds detik"
                    }
                    _state.value = _state.value.copy(estimatedTime = "Estimasi waktu: $estimate")
                } catch (e: Exception) {
                    _state.value = _state.value.copy(estimatedTime = "Estimasi tidak tersedia")
                }
            }
        }
    }

    /**
     * 🚀 MAIN BULK PROCESSOR
     * Orchestrates CSV parsing, photo downloading, and biometric syncing.
     */
    fun processCsvFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            _state.value = ProcessingState(
                isProcessing = true,
                status = "Inisialisasi...",
                estimatedTime = state.value.estimatedTime
            )

            withContext(Dispatchers.IO) {
                try {
                    val db = AppDatabase.getInstance(context)
                    val currentUser = db.userDao().getCurrentUser()
                    val sid = currentUser?.sekolahId ?: "UNKNOWN"

                    if (sid == "UNKNOWN") {
                        _state.value = _state.value.copy(
                            isProcessing = false,
                            status = "Error: Sesi sekolah tidak ditemukan."
                        )
                        return@withContext
                    }

                    val csvResult = CsvImportUtils.parseCsvFile(context, uri)
                    val faceDao = db.faceDao()
                    val existingFaces = faceDao.getAllFaces()
                    val resultsList = mutableListOf<ProcessResult>()
                    
                    var success = 0
                    var dupes = 0
                    var errors = 0
                    val totalStudents = csvResult.students.size

                    csvResult.students.forEachIndexed { index, student ->
                        try {
                            _state.value = _state.value.copy(
                                progress = (index + 1).toFloat() / totalStudents,
                                status = "Memproses ${index + 1}/$totalStudents: ${student.name}"
                            )

                            val result = processStudent(context, student, sid, faceDao, existingFaces)
                            
                            if (result.isSuccess) success++ 
                            else if (result.status.contains("Duplicate")) dupes++ 
                            else errors++
                            
                            resultsList.add(result)
                        } catch (e: Exception) {
                            errors++
                            resultsList.add(
                                ProcessResult(
                                    studentId = student.studentId, 
                                    name = student.name, 
                                    status = "Error", 
                                    error = e.message
                                )
                            )
                        }
                    }

                    // 🧠 Refresh Cache so scanner recognizes new students immediately
                    FaceCache.refresh(context)

                    _state.value = _state.value.copy(
                        isProcessing = false,
                        results = resultsList,
                        successCount = success,
                        duplicateCount = dupes,
                        errorCount = errors,
                        status = "Selesai: $success sukses, $dupes duplikat, $errors error"
                    )
                } catch (e: Exception) {
                    _state.value = _state.value.copy(
                        isProcessing = false, 
                        status = "Terhenti: ${e.message}"
                    )
                }
            }
        }
    }

    private suspend fun processStudent(
        context: Context,
        student: CsvImportUtils.CsvStudentData,
        sekolahId: String,
        faceDao: FaceDao,
        existingFacesCache: List<FaceEntity>
    ): ProcessResult {
        // 1. Primary Key Check
        if (faceDao.getFaceByStudentId(student.studentId) != null) {
            return ProcessResult(
                studentId = student.studentId, 
                name = student.name, 
                status = "Duplicate ID", 
                isSuccess = false
            )
        }

        // 2. Photo & AI Embedding
        val photoResult = BulkPhotoProcessor.processPhotoSource(context, student.photoUrl, student.studentId)
        if (!photoResult.success) {
            return ProcessResult(
                studentId = student.studentId, 
                name = student.name, 
                status = "Photo Error", 
                error = photoResult.error
            )
        }

        val bitmap = BitmapFactory.decodeFile(photoResult.localPhotoUrl) 
            ?: return ProcessResult(student.studentId, student.name, status = "Decode Error")
            
        val embeddingResult = PhotoProcessingUtils.processBitmapForFaceEmbedding(context, bitmap) 
            ?: return ProcessResult(student.studentId, student.name, status = "No Face Detected")

        val (faceBitmap, embedding) = embeddingResult

        // 3. AI Anti-Duplicate Check (Cosine Similarity)
        for (face in existingFacesCache) {
            if (NativeMath.cosineDistance(face.embedding, embedding) < 0.45f) {
                return ProcessResult(
                    studentId = student.studentId, 
                    name = student.name, 
                    status = "Duplicate Face: ${face.name}"
                )
            }
        }

        // 4. Persistence & Cloud Sync
        val path = PhotoStorageUtils.saveFacePhoto(context, faceBitmap, student.studentId)
        val faceEntity = FaceEntity(
            studentId = student.studentId,
            sekolahId = sekolahId,
            name = student.name,
            photoUrl = path ?: "",
            embedding = embedding,
            className = student.className ?: "Umum"
        )
        
        // A. Save to Local Room
        faceDao.insert(faceEntity)
        
        // B. Save to Cloud Firestore (Using Modular Repository)
        // ✅ UPDATED: Use FirestoreStudent instead of FirestoreHelper
        FirestoreStudent.uploadStudent(faceEntity)

        return ProcessResult(
            studentId = student.studentId, 
            name = student.name, 
            status = "Registered", 
            isSuccess = true, 
            photoSize = photoResult.processedSize
        )
    }

    fun resetState() {
        _state.value = ProcessingState()
    }

    private fun formatFileSize(size: Long): String {
        return when {
            size == 0L -> "0 KB"
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> String.format("%.1f MB", size / (1024.0 * 1024.0))
        }
    }
}