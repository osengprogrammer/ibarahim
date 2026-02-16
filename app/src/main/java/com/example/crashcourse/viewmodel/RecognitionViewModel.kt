package com.example.crashcourse.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crashcourse.db.*
import com.example.crashcourse.ml.FaceRecognitionEngine
import com.example.crashcourse.ml.MatchResult
import com.example.crashcourse.repository.AttendanceRepository
import com.example.crashcourse.repository.UserRepository
import com.example.crashcourse.utils.Constants
import com.example.crashcourse.utils.BiometricConfig 
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

sealed class AttendanceResult {
    object Idle : AttendanceResult()
    object Loading : AttendanceResult()
    data class Success(val name: String) : AttendanceResult()
    data class Error(val message: String) : AttendanceResult()
    data class Cooldown(val name: String, val secondsRemaining: Int) : AttendanceResult()
    data class Unauthorized(val name: String, val session: String) : AttendanceResult()
    object SecurityBreach : AttendanceResult() 
    data class WaitingBlink(val name: String) : AttendanceResult() 
}

data class FaceDebugInfo(
    val bestDist: Float = 0f,
    val label: String = "Standby",
    val blinkStatus: String = "MENUNGGU...",
    val stability: Int = 0
)

class RecognitionViewModel(application: Application) : AndroidViewModel(application) {

    private val attendanceRepo = AttendanceRepository(application)
    private val userRepo = UserRepository(application)
    private val recognitionEngine = FaceRecognitionEngine(application)

    private val _attendanceState = MutableStateFlow<AttendanceResult>(AttendanceResult.Idle)
    val attendanceState = _attendanceState.asStateFlow()

    private val _debugFlow = MutableStateFlow<FaceDebugInfo>(FaceDebugInfo())
    val debugFlow = _debugFlow.asStateFlow()

    private val checkInTimestamps = ConcurrentHashMap<String, Long>()
    private var adminSchoolId: String? = null
    var activeSessionClass: String = "General"
    
    private var blinkDetected = false
    private var postBlinkRetryCounter = 0 
    private var currentTrackingId: String = ""
    private var stabilityCounter = 0

    @Volatile
    private var isProcessingFrame = false

    companion object {
        private const val STRICT_THRESHOLD = BiometricConfig.STRICT_THRESHOLD
        private const val REQUIRED_STABILITY = BiometricConfig.REQUIRED_STABILITY
        private const val COOLDOWN_MS = 20_000L 
        private const val BLINK_CLOSED_THRESHOLD = 0.25f 
        private const val EYES_OPEN_RECOVERY = 0.65f    
        private const val MAX_POST_BLINK_RETRIES = 15   
    }

    init {
        viewModelScope.launch { FaceCache.ensureLoaded(application) }
        viewModelScope.launch {
            userRepo.getCurrentUserFlow().collect { user -> adminSchoolId = user?.schoolId }
        }
    }

    fun processEmbedding(embedding: FloatArray, leftEyeProb: Float?, rightEyeProb: Float?) {
        val state = _attendanceState.value
        if (state is AttendanceResult.SecurityBreach) return
        if (state is AttendanceResult.Success || state is AttendanceResult.Cooldown || state is AttendanceResult.Error) return
        if (isProcessingFrame) return 
        isProcessingFrame = true

        viewModelScope.launch(Dispatchers.Default) {
            try {
                if (!FaceCache.isReady()) {
                    updateHUD("DATABASE KOSONG", "❌")
                    return@launch
                }
                val result = recognitionEngine.recognize(embedding)
                if (result is MatchResult.Success) {
                    if (!verifyIntegritas(result.dist)) {
                        triggerSelfDestruct("TAMPER_DETECTED")
                        return@launch
                    }
                    val detectedFace = result.face
                    val bestDist = result.dist
                    val isMatch = bestDist <= STRICT_THRESHOLD
                    if (isMatch) {
                        if (!blinkDetected && leftEyeProb != null && rightEyeProb != null) {
                            if (leftEyeProb < BLINK_CLOSED_THRESHOLD || rightEyeProb < BLINK_CLOSED_THRESHOLD) {
                                blinkDetected = true
                                stabilityCounter = 0 
                            }
                        }
                        if (currentTrackingId.isNotEmpty() && currentTrackingId != detectedFace.studentId) {
                            forceCleanupInternal() 
                            return@launch
                        } else if (currentTrackingId.isEmpty()) {
                            currentTrackingId = detectedFace.studentId
                        }
                        val now = System.currentTimeMillis()
                        val lastCheck = checkInTimestamps[detectedFace.studentId] ?: 0L
                        if (now - lastCheck < COOLDOWN_MS) {
                            val remaining = ((COOLDOWN_MS - (now - lastCheck)) / 1000).toInt()
                            _attendanceState.postValue(AttendanceResult.Cooldown(detectedFace.name, remaining))
                            delay(2500)
                            forceCleanupInternal()
                            return@launch
                        }
                        if (!blinkDetected) {
                            _attendanceState.value = AttendanceResult.WaitingBlink(detectedFace.name)
                            updateHUD(detectedFace.name, "TUNGGU KEDIP", bestDist)
                        } else {
                            if (leftEyeProb != null && leftEyeProb > EYES_OPEN_RECOVERY) {
                                stabilityCounter++
                                updateHUD(detectedFace.name, "VERIFIKASI $stabilityCounter/$REQUIRED_STABILITY", bestDist)
                                if (stabilityCounter >= REQUIRED_STABILITY) {
                                    handleSuccess(detectedFace, bestDist)
                                }
                            } else {
                                updateHUD(detectedFace.name, "BUKA MATA", bestDist)
                            }
                        }
                    } else {
                        handleUnrecognized(bestGuessName = detectedFace.name, dist = bestDist)
                    }
                } else {
                    handleUnrecognized(bestGuessName = "Unknown", dist = 9.99f)
                }
            } finally {
                isProcessingFrame = false
            }
        }
    }

    private fun verifyIntegritas(dist: Float): Boolean {
        val formatted = String.format(java.util.Locale.US, "%.5f", dist)
        return formatted.endsWith("1")
    }

    private fun triggerSelfDestruct(reason: String) {
        viewModelScope.launch(Dispatchers.Main) {
            _attendanceState.value = AttendanceResult.SecurityBreach
            updateHUD("KEAMANAN DILANGGAR!", "⚠️ $reason")
        }
    }

    private fun handleUnrecognized(bestGuessName: String, dist: Float) {
        if (currentTrackingId.isNotEmpty()) {
            postBlinkRetryCounter++
            updateHUD("FOKUS: $bestGuessName?", "🔍", dist)
            if (postBlinkRetryCounter > MAX_POST_BLINK_RETRIES) {
                forceCleanupInternal()
            }
        } else {
            if (_attendanceState.value !is AttendanceResult.Idle) {
                _attendanceState.value = AttendanceResult.Idle
            }
            val displayLabel = if (dist > BiometricConfig.ENGINE_REJECTION_THRESHOLD) "Unknown" else "Mirip: $bestGuessName"
            updateHUD(displayLabel, "❌", dist)
        }
    }

    private suspend fun handleSuccess(face: FaceEntity, bestDist: Float) {
        updateHUD(face.name, "✅ TERVERIFIKASI", bestDist)
        
        // 🔥 FIX: Menambahkan schoolId agar sesuai constructor CheckInRecord
        val newRecord = CheckInRecord(
            studentId = face.studentId,
            name = face.name,
            schoolId = adminSchoolId ?: face.schoolId, 
            timestamp = LocalDateTime.now(),
            status = Constants.STATUS_PRESENT,
            className = activeSessionClass,
            gradeName = face.grade,
            role = "STUDENT",
            syncStatus = "PENDING"
        )

        val res = attendanceRepo.saveAttendance(newRecord, adminSchoolId ?: face.schoolId, bestDist)
        
        if (res == "SUCCESS" || res == "SAVED_OFFLINE") {
            checkInTimestamps[face.studentId] = System.currentTimeMillis()
            _attendanceState.value = AttendanceResult.Success(face.name)
            delay(3500)
            forceCleanupInternal() 
        } else {
            _attendanceState.value = AttendanceResult.Error("Gagal: $res")
            delay(2000)
            forceCleanupInternal()
        }
    }

    fun onFaceLost() { forceCleanupInternal() }
    fun forceCleanup() { forceCleanupInternal() }

    private fun forceCleanupInternal() {
        blinkDetected = false
        postBlinkRetryCounter = 0
        currentTrackingId = ""
        stabilityCounter = 0
        isProcessingFrame = false
        _attendanceState.value = AttendanceResult.Idle
        _debugFlow.value = FaceDebugInfo()
    }

    private fun updateHUD(label: String, blink: String, dist: Float = 0f) {
        _debugFlow.value = _debugFlow.value.copy(label = label, blinkStatus = blink, bestDist = dist, stability = stabilityCounter)
    }

    private fun <T> MutableStateFlow<T>.postValue(value: T) { this.value = value }
}