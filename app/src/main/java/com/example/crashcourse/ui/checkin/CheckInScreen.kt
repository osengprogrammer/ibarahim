package com.example.crashcourse.ui.checkin

import android.graphics.Rect
import android.media.AudioManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.crashcourse.scanner.FaceScanner
import com.example.crashcourse.ui.components.FaceOverlay
import com.example.crashcourse.viewmodel.*
import com.example.crashcourse.ui.checkin.components.AttendanceResultCard
import com.example.crashcourse.ui.checkin.components.CheckInHUD
import com.example.crashcourse.ui.checkin.components.CheckInLoadingView
import com.example.crashcourse.ui.theme.AzuraAccent
import kotlinx.coroutines.delay
import java.util.*

/**
 * 👁️ Azura Tech AI Check-In Screen (V.17.6 - Final Stable)
 * Fix: Unresolved reference 'ttsInstance' in TTS initialization.
 */
@Composable
fun CheckInScreen(
    useBackCamera: Boolean,
    recognitionViewModel: RecognitionViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    
    val attendanceResult by recognitionViewModel.attendanceState.collectAsStateWithLifecycle()
    val debugInfo by recognitionViewModel.debugFlow.collectAsStateWithLifecycle()

    var currentCameraIsBack by remember { mutableStateOf(useBackCamera) }
    var isLightBoostOn by remember { mutableStateOf(false) }

    var faceBounds by remember { mutableStateOf<List<Rect>>(emptyList()) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    var imageRotation by remember { mutableIntStateOf(0) }

    val toneGen = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    // --- 🎙️ TTS INITIALIZATION (FIXED SCOPE) ---
    DisposableEffect(Unit) {
        val ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // ✅ FIX: Set language via the local variable that is now ready
                // atau langsung gunakan scope listener
            }
        }
        // Set language setelah instansi dibuat
        ttsInstance.language = Locale("id", "ID")
        tts = ttsInstance

        onDispose { 
            tts?.stop()
            tts?.shutdown() 
        }
    }

    // --- ⏲️ MULTI-MODAL FEEDBACK ---
    LaunchedEffect(attendanceResult) {
        when (val current = attendanceResult) {
            is AttendanceResult.Success -> {
                toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 200)
                tts?.speak("Berhasil. Selamat datang, ${current.name}", TextToSpeech.QUEUE_FLUSH, null, null)
                delay(3500L)
                recognitionViewModel.forceCleanup()
            }
            is AttendanceResult.WaitingBlink -> {
                val firstName = current.name.split(" ").firstOrNull() ?: "Siswa"
                tts?.speak("Halo $firstName, silakan berkedip", TextToSpeech.QUEUE_FLUSH, null, null)
            }
            is AttendanceResult.SecurityBreach -> {
                toneGen.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 1000)
                tts?.speak("Peringatan! Pelanggaran keamanan terdeteksi.", TextToSpeech.QUEUE_FLUSH, null, null)
            }
            is AttendanceResult.Cooldown -> {
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
                tts?.speak("${current.name}, Anda sudah absen.", TextToSpeech.QUEUE_FLUSH, null, null)
                delay(2500L)
                recognitionViewModel.forceCleanup()
            }
            is AttendanceResult.Error -> {
                delay(2000L)
                recognitionViewModel.forceCleanup()
            }
            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        val currentState = authState
        
        if (currentState !is AuthState.Active) {
            CheckInLoadingView(Modifier.align(Alignment.Center))
        } else {
            if (attendanceResult !is AttendanceResult.SecurityBreach) {
                FaceScanner(
                    useBackCamera = currentCameraIsBack,
                    enableLightBoost = isLightBoostOn
                ) { result ->
                    faceBounds = result.bounds
                    imageSize = result.imageSize
                    imageRotation = result.rotation

                    if (result.bounds.isEmpty()) {
                        recognitionViewModel.onFaceLost()
                    }

                    if (result.embeddings.isNotEmpty()) {
                        val (_, embedding) = result.embeddings.first()
                        recognitionViewModel.processEmbedding(
                            embedding = embedding,
                            leftEyeProb = result.leftEyeOpenProb,
                            rightEyeProb = result.rightEyeOpenProb
                        )
                    }
                }
            }

            FaceOverlay(
                faceBounds = faceBounds,
                imageSize = imageSize,
                imageRotation = imageRotation,
                isFrontCamera = !currentCameraIsBack,
                modifier = Modifier.fillMaxSize()
            )

            CheckInHUD(
                schoolName = currentState.schoolName,
                statusMessage = getStatusMessage(attendanceResult),
                isFlashOn = isLightBoostOn,
                onFlashToggle = { isLightBoostOn = !isLightBoostOn },
                onCameraFlip = { currentCameraIsBack = !currentCameraIsBack }
            )

            // AI RADAR
            Column(
                modifier = Modifier
                    .padding(top = 110.dp, start = 16.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text("AZURA AI RADAR V5", color = AzuraAccent, fontSize = 10.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(6.dp))
                DebugRow(label = "Label", value = debugInfo.label, valueColor = Color.Cyan)
                DebugRow(label = "Stability", value = "${debugInfo.stability}/4")
                if (debugInfo.bestDist > 0f) {
                    DebugRow(label = "Dist", value = "%.5f".format(debugInfo.bestDist))
                }
            }

            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                AnimatedVisibility(
                    visible = attendanceResult is AttendanceResult.WaitingBlink,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                    modifier = Modifier.padding(bottom = 220.dp)
                ) {
                    val userName = (attendanceResult as? AttendanceResult.WaitingBlink)?.name ?: "Siswa"
                    BlinkInstructionOverlay(userName = userName)
                }

                AttendanceResultCard(
                    result = attendanceResult,
                    activeSession = recognitionViewModel.activeSessionClass,
                    modifier = Modifier.padding(bottom = 80.dp)
                )
            }

            AnimatedVisibility(
                visible = attendanceResult is AttendanceResult.SecurityBreach,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut()
            ) {
                SecurityBreachOverlay()
            }
        }
    }
}

@Composable
private fun SecurityBreachOverlay() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Red.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Surface(modifier = Modifier.size(100.dp), shape = CircleShape, color = Color.White.copy(alpha = 0.2f)) {
                Icon(Icons.Default.GppBad, null, tint = Color.White, modifier = Modifier.padding(20.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text("SECURITY BREACH", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text("Sistem mendeteksi upaya manipulasi kode. Aplikasi dikunci.", color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun BlinkInstructionOverlay(userName: String) {
    Surface(
        color = Color.Black.copy(alpha = 0.85f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(2.dp, Color.Cyan),
        modifier = Modifier.padding(horizontal = 32.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "👁️", fontSize = 40.sp)
            Text("HALO, ${userName.uppercase()}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("SILAKAN BERKEDIP", color = Color.Cyan, fontWeight = FontWeight.Black, fontSize = 24.sp)
        }
    }
}

@Composable
private fun DebugRow(label: String, value: String, valueColor: Color = Color.White) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$label: ", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.labelSmall, color = valueColor, fontWeight = FontWeight.Bold)
    }
}

private fun getStatusMessage(result: AttendanceResult): String {
    return when(result) {
        is AttendanceResult.Idle -> "Mencari Wajah..."
        is AttendanceResult.WaitingBlink -> "VERIFIKASI KEHIDUPAN"
        is AttendanceResult.Loading -> "Mengidentifikasi..."
        is AttendanceResult.Success -> "Check-In Berhasil"
        is AttendanceResult.Unauthorized -> "Akses Ditolak"
        is AttendanceResult.Cooldown -> "Sudah Absen"
        is AttendanceResult.SecurityBreach -> "SISTEM TERKUNCI"
        is AttendanceResult.Error -> "Gagal Dikenali"
    }
}