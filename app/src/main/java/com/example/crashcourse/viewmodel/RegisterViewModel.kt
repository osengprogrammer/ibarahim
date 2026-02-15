package com.example.crashcourse.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crashcourse.repository.RegisterRepository
import com.example.crashcourse.utils.BulkPhotoProcessor
import com.example.crashcourse.utils.CsvImportUtils
import com.example.crashcourse.utils.ProcessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 📦 State untuk memantau progres registrasi massal (Bulk Registration).
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

/**
 * 👨‍💻 RegisterViewModel
 * Menangani pendaftaran massal. Memastikan sinkronisasi antara CSV, 
 * pemrosesan gambar AI, dan database Multi-Tenant.
 */
class RegisterViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = RegisterRepository(application)
    
    private val _state = MutableStateFlow(ProcessingState())
    val state = _state.asStateFlow()

    /**
     * ⏱️ PREPARE PROCESSING
     * Menghitung estimasi waktu pengerjaan AI.
     */
    fun prepareProcessing(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val estimateStr = withContext(Dispatchers.IO) {
                    val csvResult = CsvImportUtils.parseCsvFile(context, uri)
                    // 🔥 FIXED: Menggunakan nama variabel eksplisit 'student'
                    val photoSources = csvResult.students.map { student -> student.photoUrl }
                    val seconds = BulkPhotoProcessor.estimateProcessingTime(photoSources)
                    
                    when {
                        seconds > 120 -> "${seconds / 60} menit"
                        seconds > 60 -> "1 menit ${seconds % 60} detik"
                        else -> "$seconds detik"
                    }
                }
                _state.value = _state.value.copy(estimatedTime = "Estimasi waktu: $estimateStr")
            } catch (e: Exception) {
                Log.e("RegisterVM", "Gagal hitung estimasi", e)
                _state.value = _state.value.copy(estimatedTime = "Estimasi tidak tersedia")
            }
        }
    }

    /**
     * 🚀 MAIN BULK PROCESSOR
     * Alur: CSV -> Face Embedding -> Room -> Firestore.
     */
    fun processCsvFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isProcessing = true, 
                status = "Menyiapkan Database...",
                results = emptyList() 
            )
            
            try {
                val schoolId = repo.getCurrentSchoolId() ?: throw Exception("Sekolah tidak aktif")
                val csvResult = withContext(Dispatchers.IO) { CsvImportUtils.parseCsvFile(context, uri) }
                val existingFaces = repo.getAllLocalFaces(schoolId) 
                
                val resultsList = mutableListOf<ProcessResult>()
                var success = 0
                var dupes = 0
                var errors = 0
                val total = csvResult.students.size

                // Loop pendaftaran
                csvResult.students.forEachIndexed { index, student ->
                    val currentProgress = (index + 1).toFloat() / total
                    
                    // Update UI Progress
                    _state.value = _state.value.copy(
                        progress = currentProgress,
                        status = "Memproses ${index + 1} dari $total: ${student.name}"
                    )

                    // Jalankan ekstraksi biometrik di Repository
                    val result = repo.processAndRegisterStudent(student, schoolId, existingFaces)
                    
                    // 🔥 FIXED: Pastikan status di-convert ke String secara aman
                    val statusString: String = result.status?.toString() ?: "Unknown"
                    val sizeString: String = result.photoSize?.toString() ?: "N/A"
                    
                    // Update metadata real-time ke UI
                    _state.value = _state.value.copy(
                        currentPhotoType = statusString, 
                        currentPhotoSize = sizeString
                    )

                    // Logika klasifikasi hasil
                    if (result.isSuccess) {
                        success++
                    } else if (statusString.contains("Duplicate", ignoreCase = true)) {
                        dupes++
                    } else {
                        errors++
                    }
                    
                    resultsList.add(result)
                }

                // Sync ulang cache wajah di RAM agar scanner langsung mengenali siswa baru
                repo.refreshFaceCache()

                _state.value = _state.value.copy(
                    isProcessing = false, 
                    results = resultsList, 
                    successCount = success, 
                    duplicateCount = dupes, 
                    errorCount = errors, 
                    status = "Selesai: $success Berhasil"
                )
            } catch (e: Exception) {
                Log.e("RegisterVM", "Proses Massal Gagal", e)
                _state.value = _state.value.copy(
                    isProcessing = false, 
                    status = "Error: ${e.message}"
                )
            }
        }
    }

    fun resetState() {
        _state.value = ProcessingState()
    }
}