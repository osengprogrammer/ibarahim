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
 * 📦 State untuk memantau progres registrasi massal.
 * Pastikan data class ini berada di level package agar bisa di-import oleh Screen.
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
 * Menangani alur pendaftaran siswa massal via CSV & AI Photo Processing.
 */
class RegisterViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = RegisterRepository(application)
    
    private val _state = MutableStateFlow(ProcessingState())
    val state = _state.asStateFlow()

    /**
     * ⏱️ PREPARE PROCESSING
     * Menghitung estimasi waktu sebelum eksekusi dimulai.
     * Memperbaiki error "Unresolved reference" di BulkRegistrationScreen.
     */
    fun prepareProcessing(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                // Jalankan proses berat di IO Thread
                val estimateStr = withContext(Dispatchers.IO) {
                    val csvResult = CsvImportUtils.parseCsvFile(context, uri)
                    val photoSources = csvResult.students.map { it.photoUrl }
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
     * Mengorkestrasi pendaftaran dari CSV ke Local Database & Cloud.
     */
    fun processCsvFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isProcessing = true, 
                status = "Inisialisasi...",
                results = emptyList() 
            )
            
            try {
                // 1. Ambil data awal dari Repository
                val sid = repo.getCurrentSekolahId() ?: throw Exception("Sesi tidak ditemukan")
                val csvResult = withContext(Dispatchers.IO) { CsvImportUtils.parseCsvFile(context, uri) }
                val existingFaces = repo.getAllLocalFaces()
                val resultsList = mutableListOf<ProcessResult>()
                
                var success = 0
                var dupes = 0
                var errors = 0
                val total = csvResult.students.size

                // 2. Loop proses siswa satu per satu
                csvResult.students.forEachIndexed { index, student ->
                    _state.value = _state.value.copy(
                        progress = (index + 1).toFloat() / total,
                        status = "Memproses ${index + 1}/$total: ${student.name}"
                    )

                    // Panggil fungsi suspend di Repository
                    val result = repo.processAndRegisterStudent(student, sid, existingFaces)
                    
                    if (result.isSuccess) success++ 
                    else if (result.status.contains("Duplicate")) dupes++ 
                    else errors++
                    
                    resultsList.add(result)
                }

                // 3. Refresh AI Cache agar scanner langsung mengenali wajah baru
                repo.refreshFaceCache()

                _state.value = _state.value.copy(
                    isProcessing = false, 
                    results = resultsList, 
                    successCount = success, 
                    duplicateCount = dupes, 
                    errorCount = errors, 
                    status = "Selesai: $success Sukses"
                )
            } catch (e: Exception) {
                Log.e("RegisterVM", "Bulk Process Failed", e)
                _state.value = _state.value.copy(
                    isProcessing = false, 
                    status = "Error: ${e.message}"
                )
            }
        }
    }

    /**
     * 🧹 Reset state saat menutup layar atau memulai ulang.
     */
    fun resetState() {
        _state.value = ProcessingState()
    }
}