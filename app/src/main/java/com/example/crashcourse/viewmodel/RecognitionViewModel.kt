package com.example.crashcourse.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crashcourse.db.CheckInRecord
import com.example.crashcourse.repository.AttendanceRepository
import com.example.crashcourse.repository.FaceRepository
import com.example.crashcourse.repository.UserRepository
import com.example.crashcourse.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/**
 * 🎯 RecognitionViewModel (V.1.0)
 * Otak utama untuk memproses hasil Face Recognition dan validasi kehadiran.
 */
class RecognitionViewModel(application: Application) : AndroidViewModel(application) {

    private val attendanceRepo = AttendanceRepository(application)
    private val faceRepo = FaceRepository(application)
    private val userRepo = UserRepository(application)

    private val _attendanceState = MutableStateFlow<AttendanceResult>(AttendanceResult.Idle)
    val attendanceState = _attendanceState.asStateFlow()

    companion object {
        private const val TAG = "RecognitionVM"
        private const val COOLDOWN_SECONDS = 30L
    }

    /**
     * 👁️ PROSES PENGENALAN & ABSENSI (Double Validation)
     */
    fun processRecognition(detectedName: String, activeSession: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Ambil konteks sekolah aktif
                val user = userRepo.getCurrentUser() ?: return@launch
                val sid = user.sekolahId ?: return@launch

                // 2. TAHAP IDENTIFIKASI (Verifikasi kepemilikan data biometrik)
                val matches = faceRepo.getFaceByName(detectedName)
                val face = matches.firstOrNull { it.sekolahId == sid }

                if (face == null) {
                    _attendanceState.value = AttendanceResult.Error("Wajah tidak terdaftar di instansi ini")
                    return@launch
                }

                // 3. TAHAP OTORISASI (Cek apakah siswa terdaftar di sesi/unit ini)
                val isAuthorized = face.className.contains(activeSession, ignoreCase = true)
                if (!isAuthorized) {
                    _attendanceState.value = AttendanceResult.Unauthorized(face.name, activeSession)
                    return@launch
                }

                // 4. CEK ANTI-SPAM (Cooldown per Sesi)
                val lastRecord = attendanceRepo.getLastRecordForClass(face.studentId, activeSession)
                if (lastRecord != null && lastRecord.timestamp.isAfter(LocalDateTime.now().minusSeconds(COOLDOWN_SECONDS))) {
                    _attendanceState.value = AttendanceResult.Cooldown(face.name)
                    return@launch
                }

                // 5. EKSEKUSI PENCATATAN (Simpan ke Local & Cloud)
                val record = CheckInRecord(
                    id = 0,
                    studentId = face.studentId,
                    name = face.name,
                    timestamp = LocalDateTime.now(),
                    status = Constants.STATUS_PRESENT,
                    verified = true,
                    syncStatus = "PENDING",
                    photoPath = "",
                    className = activeSession,
                    gradeName = face.grade,
                    role = face.role
                )

                attendanceRepo.saveAttendance(record, sid)
                _attendanceState.value = AttendanceResult.Success(face.name)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Recognition processing failed", e)
                _attendanceState.value = AttendanceResult.Error("Kesalahan sistem saat memproses wajah")
            }
        }
    }

    fun resetState() {
        _attendanceState.value = AttendanceResult.Idle
    }
}

/**
 * Representasi State untuk memberikan feedback instan ke UI Scanner
 */
sealed class AttendanceResult {
    object Idle : AttendanceResult()
    object Loading : AttendanceResult()
    data class Success(val name: String) : AttendanceResult()
    data class Error(val message: String) : AttendanceResult()
    data class Cooldown(val name: String) : AttendanceResult()
    data class Unauthorized(val name: String, val session: String) : AttendanceResult()
}