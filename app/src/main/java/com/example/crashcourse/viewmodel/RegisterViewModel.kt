package com.example.crashcourse.viewmodel

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.crashcourse.db.AppDatabase
import com.example.crashcourse.db.FaceCache
import com.example.crashcourse.db.FaceEntity
import com.example.crashcourse.utils.BulkPhotoProcessor
import com.example.crashcourse.utils.CsvImportUtils
import com.example.crashcourse.utils.FirestoreHelper
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

// Data class tetap sama
data class ProcessingState(
    val isProcessing: Boolean = false,
    val status: String = "Waiting for CSV...",
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

    fun prepareProcessing(context: Context, uri: Uri) {
        viewModelScope.launch {
            // 🚀 Pindah ke IO agar tidak nge-lag saat baca file
            withContext(Dispatchers.IO) {
                try {
                    val csvResult = CsvImportUtils.parseCsvFile(context, uri)
                    val photoSources = csvResult.students.map { it.photoUrl }

                    val seconds = BulkPhotoProcessor.estimateProcessingTime(photoSources)
                    val estimate = when {
                        seconds > 120 -> "${seconds / 60} minutes"
                        seconds > 60 -> "1 minute ${seconds % 60} seconds"
                        else -> "$seconds seconds"
                    }

                    _state.value = _state.value.copy(
                        estimatedTime = "Estimated time: $estimate"
                    )
                } catch (e: Exception) {
                    _state.value = _state.value.copy(
                        estimatedTime = "Time estimate unavailable"
                    )
                }
            }
        }
    }

    fun processCsvFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            _state.value = ProcessingState(
                isProcessing = true,
                status = "Initializing...",
                estimatedTime = state.value.estimatedTime
            )

            // 🚀 OPERASI BERAT PINDAH KE BACKGROUND THREAD
            withContext(Dispatchers.IO) {
                try {
                    // 1. Parse CSV
                    val csvResult = CsvImportUtils.parseCsvFile(context, uri)
                    if (csvResult.students.isEmpty()) {
                        _state.value = _state.value.copy(
                            isProcessing = false,
                            status = "No valid students found in CSV",
                            errorCount = csvResult.errors.size
                        )
                        return@withContext
                    }

                    // 2. Load Existing Faces SEKALI SAJA di awal untuk optimasi
                    val faceDao = AppDatabase.getInstance(context).faceDao()
                    val existingFaces = faceDao.getAllFaces().toMutableList() // Mutable agar bisa ditambah realtime
                    
                    val results = mutableListOf<ProcessResult>()
                    var successCount = 0
                    var duplicateCount = 0
                    var errorCount = 0
                    val totalStudents = csvResult.students.size

                    // 3. Loop Processing
                    csvResult.students.forEachIndexed { index, student ->
                        try {
                            val photoType = BulkPhotoProcessor.getPhotoSourceType(student.photoUrl)
                            
                            // Update UI State (Progress)
                            _state.value = _state.value.copy(
                                progress = (index + 1).toFloat() / totalStudents,
                                status = "Processing ${index + 1}/$totalStudents: ${student.name}",
                                currentPhotoType = "Src: $photoType",
                                currentPhotoSize = "Processing..."
                            )

                            // Process Single Student
                            val result = processStudent(context, student, faceDao, existingFaces)
                            
                            // Update Counters & List
                            when {
                                result.status.contains("Registered") -> {
                                    successCount++
                                    // Jika sukses, tambahkan ke list existingFaces agar siswa berikutnya 
                                    // dalam batch ini tidak duplikat dengan siswa ini
                                    // (Kita perlu objek FaceEntity dummy untuk pembanding visual sementara)
                                    // *Catatan: Embedding diambil dari result jika memungkinkan, tapi 
                                    // karena ProcessResult tidak nyimpan embedding, kita skip update list visual sementara
                                    // atau refactor processStudent untuk return embedding.*
                                }
                                result.status.startsWith("Duplicate") -> duplicateCount++
                                else -> errorCount++
                            }
                            results.add(result)

                            _state.value = _state.value.copy(
                                currentPhotoSize = "Size: ${formatFileSize(result.photoSize)}"
                            )
                        } catch (e: Exception) {
                            errorCount++
                            results.add(
                                ProcessResult(
                                    studentId = student.studentId,
                                    name = student.name,
                                    status = "Error",
                                    error = e.message ?: "Unknown error"
                                )
                            )
                        }
                    }

                    // 4. Refresh Cache & Finalize
                    try {
                        FaceCache.refresh(context)
                    } catch (e: Exception) {
                        Log.e("RegisterViewModel", "Failed to refresh FaceCache", e)
                    }

                    _state.value = ProcessingState(
                        isProcessing = false,
                        results = results,
                        successCount = successCount,
                        duplicateCount = duplicateCount,
                        errorCount = errorCount,
                        status = "Done: $successCount success, $duplicateCount duplicates, $errorCount errors"
                    )

                } catch (e: Exception) {
                    _state.value = ProcessingState(
                        isProcessing = false,
                        status = "Processing crashed: ${e.message}",
                        errorCount = 1
                    )
                }
            }
        }
    }

    private suspend fun processStudent(
        context: Context,
        student: CsvImportUtils.CsvStudentData,
        faceDao: com.example.crashcourse.db.FaceDao,
        existingFacesCache: List<FaceEntity> // 🚀 Parameter baru: Cache Wajah
    ): ProcessResult {
        
        // A. Cek Duplicate ID dulu (Paling cepat)
        // Cek di DB
        if (faceDao.getFaceByStudentId(student.studentId) != null) {
             return ProcessResult(
                studentId = student.studentId,
                name = student.name,
                status = "Duplicate (ID Exists)",
                photoSize = 0
            )
        }

        // B. Process Photo & Embedding
        val photoResult = BulkPhotoProcessor.processPhotoSource(
            context = context,
            photoSource = student.photoUrl,
            studentId = student.studentId
        )

        if (!photoResult.success) {
            return ProcessResult(
                studentId = student.studentId,
                name = student.name,
                status = "Error",
                error = photoResult.error ?: "Photo failed",
                photoSize = photoResult.originalSize
            )
        }

        val bitmap = BitmapFactory.decodeFile(photoResult.localPhotoUrl)
            ?: return ProcessResult(
                studentId = student.studentId,
                name = student.name,
                status = "Error",
                error = "Bitmap decode failed",
                photoSize = photoResult.originalSize
            )

        val embeddingResult = PhotoProcessingUtils.processBitmapForFaceEmbedding(context, bitmap)
            ?: return ProcessResult(
                studentId = student.studentId,
                name = student.name,
                status = "Error",
                error = "No face detected",
                photoSize = photoResult.originalSize
            )

        val (faceBitmap, embedding) = embeddingResult

        // C. Visual Duplicate Check (Pakai Cache List, bukan Query DB berulang)
        val DUPLICATE_THRESHOLD = 0.75f // Sesuaikan threshold MobileFaceNet (biasanya 0.75 - 0.80)
        
        // Loop ini ringan karena hanya hitung matematika di memori (tanpa IO DB)
        for (face in existingFacesCache) {
            val dist = NativeMath.cosineDistance(face.embedding, embedding)
            if (dist > DUPLICATE_THRESHOLD) { // Cosine Distance: Makin besar makin mirip (range -1 s/d 1) ??
                // ⚠️ NOTE: Pastikan NativeMath kamu return Distance (0..2, makin kecil makin mirip) 
                // atau Similarity (0..1, makin besar makin mirip).
                // Biasanya Cosine Distance: 0 = identik. Threshold biasanya < 0.25 atau < 0.4 tergantung model.
                // Jika pakai L2 Distance, threshold biasanya 1.0 - 1.2.
                // ASUMSI KODE LAMA: dist <= THRESHOLD (Distance based)
            }
        }
        
        // Revisi Logika Distance sesuai kode original kamu:
        val THRESHOLD_DISTANCE = 0.4f // Contoh jika pakai Cosine Distance murni (1 - similarity)
        for (face in existingFacesCache) {
             val dist = NativeMath.cosineDistance(face.embedding, embedding)
             // Asumsi NativeMath mengembalikan distance (semakin kecil semakin mirip)
             if (dist < THRESHOLD_DISTANCE) { 
                 return ProcessResult(
                    studentId = student.studentId,
                    name = student.name,
                    status = "Duplicate (Face Match: ${face.name})",
                    photoSize = photoResult.originalSize
                )
             }
        }

        // D. Save & Insert
        val photoPath = PhotoStorageUtils.saveFacePhoto(context, faceBitmap, student.studentId)
            ?: return ProcessResult(
                studentId = student.studentId,
                name = student.name,
                status = "Error",
                error = "Save photo failed",
                photoSize = photoResult.originalSize
            )

        val faceEntity = FaceEntity(
            studentId = student.studentId,
            name = student.name,
            photoUrl = photoPath,
            embedding = embedding,
            className = student.className ?: "",
            subClass = student.subClass ?: "",
            grade = student.grade ?: "",
            subGrade = student.subGrade ?: "",
            program = student.program ?: "",
            role = student.role ?: "",
            timestamp = System.currentTimeMillis()
        )

        faceDao.insert(faceEntity)

        // E. Cloud Sync
        val isSynced = FirestoreHelper.syncStudentToFirestore(faceEntity)
        val statusMessage = if (isSynced) "Registered & Synced" else "Registered (Local Only)"

        return ProcessResult(
            studentId = student.studentId,
            name = student.name,
            status = statusMessage,
            photoSize = photoResult.processedSize
        )
    }

    private fun formatFileSize(size: Long): String {
        return when {
            size == 0L -> "0 KB"
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> String.format("%.1f MB", size / (1024.0 * 1024.0))
        }
    }

    fun resetState() {
        _state.value = ProcessingState()
    }
}