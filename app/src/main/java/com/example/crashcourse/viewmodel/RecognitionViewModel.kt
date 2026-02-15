package com.example.crashcourse.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crashcourse.db.*
import com.example.crashcourse.ml.FaceRecognitionEngine
import com.example.crashcourse.ml.MatchResult
import com.example.crashcourse.repository.AttendanceRepository
import com.example.crashcourse.repository.UserRepository
import com.example.crashcourse.utils.Constants
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
    data class WaitingBlink(val name: String) : AttendanceResult() 
}

data class FaceDebugInfo(
    val bestDist: Float = 0f,
    val label: String = "Standby",
    val blinkStatus: String = "MENUNGGU...",
    val stability: Int = 0
)

/**
 * 👁️ RecognitionViewModel V.16.0 - Precision Engine
 * Sinkronisasi penuh dengan FaceAnalyzer (Padding 25%) dan Converters (ByteArray).
 */
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
    
    // Control States
    private var blinkDetected = false
    private var postBlinkRetryCounter = 0 
    private var currentTrackingId: String = ""
    private var stabilityCounter = 0

    @Volatile
    private var isProcessingFrame = false

    companion object {
        // ✅ THRESHOLD EMAS: Jarak di bawah 0.32 dianggap valid untuk pendaftaran biometrik L2.
        private const val STRICT_THRESHOLD = 0.32f 
        
        // ✅ STABILITY: Butuh 4 frame berturut-turut untuk meyakinkan AI.
        private const val REQUIRED_STABILITY = 4 
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
        if (state is AttendanceResult.Success || state is AttendanceResult.Cooldown || state is AttendanceResult.Error) return
        
        if (isProcessingFrame) return 
        isProcessingFrame = true

        viewModelScope.launch(Dispatchers.Default) {
            try {
                if (!FaceCache.isReady()) {
                    updateHUD("DATABASE KOSONG", "❌")
                    return@launch
                }

                // 1. SENSOR KEDIP
                if (!blinkDetected && leftEyeProb != null && rightEyeProb != null) {
                    if (leftEyeProb < BLINK_CLOSED_THRESHOLD || rightEyeProb < BLINK_CLOSED_THRESHOLD) {
                        blinkDetected = true
                        stabilityCounter = 0 
                    }
                }

                // 2. RECOGNITION (Meminta Data dari Engine V.15.8)
                val result = recognitionEngine.recognize(embedding)
                
                // Ambil info terbaik untuk HUD (X-Ray Mode)
                val bestGuessName = if (result is MatchResult.Success) result.face.name else "Unknown"
                val bestDist = if (result is MatchResult.Success) result.dist else 9.99f
                
                // Validasi Kecocokan (Strict)
                val isMatch = result is MatchResult.Success && result.dist <= STRICT_THRESHOLD

                if (isMatch && result is MatchResult.Success) {
                    val detectedFace = result.face

                    // ANTI-CROSSOVER RESET (Jika wajah berganti orang saat proses)
                    if (currentTrackingId.isNotEmpty() && currentTrackingId != detectedFace.studentId) {
                        forceCleanupInternal() 
                        return@launch
                    } else if (currentTrackingId.isEmpty()) {
                        currentTrackingId = detectedFace.studentId
                    }

                    // CEK COOLDOWN
                    val now = System.currentTimeMillis()
                    val lastCheck = checkInTimestamps[detectedFace.studentId] ?: 0L
                    if (now - lastCheck < COOLDOWN_MS) {
                        val remaining = ((COOLDOWN_MS - (now - lastCheck)) / 1000).toInt()
                        _attendanceState.value = AttendanceResult.Cooldown(detectedFace.name, remaining)
                        delay(2500)
                        forceCleanupInternal()
                        return@launch
                    }

                    // 3. LOGIKA HYBRID UX (Kedip + Stabilitas)
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
                    // ❌ WAJAH TIDAK LULUS (Jarak > 0.32 atau Unknown/Obama)
                    
                    if (currentTrackingId.isNotEmpty()) {
                        // Jika sebelumnya sempat 'Lock' nama tapi wajah menjauh atau berubah
                        postBlinkRetryCounter++
                        updateHUD("FOKUS: $bestGuessName?", "🔍", bestDist)
                        
                        if (postBlinkRetryCounter > MAX_POST_BLINK_RETRIES) {
                            forceCleanupInternal()
                        }
                    } else {
                        // Kembali ke IDLE
                        if (_attendanceState.value !is AttendanceResult.Idle) {
                            _attendanceState.value = AttendanceResult.Idle
                        }

                        // ✅ PROTEKSI HUD: Jika jarak sangat jauh (> 0.40), jangan sebut nama siapapun.
                        // Ini yang menyelesaikan masalah Obama dibilang Maksum.
                        val displayLabel = if (bestDist > 0.40f) "Unknown" else "Mirip: $bestGuessName"
                        updateHUD(displayLabel, "❌", bestDist)
                    }
                }
            } finally {
                isProcessingFrame = false
            }
        }
    }

    private suspend fun handleSuccess(face: FaceEntity, dist: Float) {
        updateHUD(face.name, "✅ TERVERIFIKASI", dist)
        
        val newRecord = CheckInRecord(
            studentId = face.studentId,
            name = face.name,
            timestamp = LocalDateTime.now(),
            status = Constants.STATUS_PRESENT,
            className = activeSessionClass,
            gradeName = face.grade,
            role = "STUDENT",
            syncStatus = "PENDING"
        )

        val res = attendanceRepo.saveAttendance(newRecord, adminSchoolId ?: face.schoolId)
        if (res == "SUCCESS") {
            checkInTimestamps[face.studentId] = System.currentTimeMillis()
            _attendanceState.value = AttendanceResult.Success(face.name)
            delay(3500)
            forceCleanupInternal() 
        } else {
            _attendanceState.value = AttendanceResult.Error("Gagal menyimpan data")
            delay(2000)
            forceCleanupInternal()
        }
    }

    fun forceCleanup() {
        forceCleanupInternal()
    }

    private fun forceCleanupInternal() {
        blinkDetected = false
        postBlinkRetryCounter = 0
        currentTrackingId = ""
        stabilityCounter = 0
        isProcessingFrame = false
        _attendanceState.value = AttendanceResult.Idle
        _debugFlow.value = FaceDebugInfo()
    }

    fun onFaceLost() {
        forceCleanupInternal()
    }

    private fun updateHUD(label: String, blink: String, dist: Float = 0f) {
        _debugFlow.value = _debugFlow.value.copy(
            label = label, blinkStatus = blink, bestDist = dist, stability = stabilityCounter
        )
    }
}