package com.example.crashcourse.ui.checkin

import android.graphics.Rect
import android.media.AudioManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.crashcourse.db.FaceCache
import com.example.crashcourse.scanner.FaceScanner
import com.example.crashcourse.ui.components.FaceOverlay
import com.example.crashcourse.ui.theme.*
import com.example.crashcourse.utils.NativeMath
import com.example.crashcourse.viewmodel.*
import kotlinx.coroutines.delay
import java.util.*

/**
 * 👁️ Azura Tech AI Check-In Screen (V.7.1 - Final Fixes)
 * - TTS Scope Fixed
 * - Face Box Visibility Fixed (Z-Ordering)
 * - Threshold Strict (0.55f)
 * - Blink Detection Disabled
 */
@Composable
fun CheckInScreen(
    useBackCamera: Boolean,
    activeSession: String,
    recognitionViewModel: RecognitionViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val attendanceResult by recognitionViewModel.attendanceState.collectAsStateWithLifecycle()

    // --- UI & CAMERA STATES ---
    var currentCameraIsBack by remember { mutableStateOf(useBackCamera) }
    var gallery by remember { mutableStateOf<List<Pair<String, FloatArray>>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var isLightBoostOn by remember { mutableStateOf(false) }

    // --- SCANNER VISUAL STATES ---
    var faceBounds by remember { mutableStateOf<List<Rect>>(emptyList()) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    var imageRotation by remember { mutableIntStateOf(0) }

    // --- STABILITY CONTROL ---
    var candidateName by remember { mutableStateOf<String?>(null) }
    var stabilityCounter by remember { mutableIntStateOf(0) }
    // Require 3 consistent frames to avoid flickering/false positives
    val REQUIRED_STABILITY = 3 

    val toneGen = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    // --- 🎙️ INITIALIZE TTS (FIXED SCOPE) ---
    DisposableEffect(Unit) {
        var ttsRef: TextToSpeech? = null
        
        // Initialize
        ttsRef = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Safe call using the local reference
                ttsRef?.language = Locale("id", "ID")
            }
        }
        
        // Assign to state
        tts = ttsRef

        // Cleanup
        onDispose {
            ttsRef?.stop()
            ttsRef?.shutdown()
        }
    }

    // --- 📦 LOAD DATA (Strict School Filter) ---
    LaunchedEffect(authState) {
        if (authState is AuthState.Active) {
            val user = authState as AuthState.Active
            loading = true
            FaceCache.refresh(context)
            gallery = FaceCache.getFaces()
                .filter { it.sekolahId == user.sekolahId }
                .map { it.name to it.embedding }
            loading = false
        }
    }

    // --- ⏲️ UI AUTO-RESET & AUDIO FEEDBACK ---
    LaunchedEffect(attendanceResult) {
        when (attendanceResult) {
            is AttendanceResult.Success -> {
                toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 200)
                val name = (attendanceResult as AttendanceResult.Success).name
                tts?.speak("Selamat datang, $name", TextToSpeech.QUEUE_FLUSH, null, null)
                delay(3000L)
                recognitionViewModel.resetState()
                stabilityCounter = 0
                candidateName = null
            }
            is AttendanceResult.Error, is AttendanceResult.Unauthorized, is AttendanceResult.Cooldown -> {
                // Error tone
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
                delay(3000L)
                recognitionViewModel.resetState()
                stabilityCounter = 0
                candidateName = null
            }
            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (loading || authState !is AuthState.Active) {
            LoadingView(Modifier.align(Alignment.Center))
        } else {
            val activeUser = authState as AuthState.Active

            // ==========================================
            // 1. SCANNER ENGINE (Layer Paling Bawah)
            // ==========================================
            FaceScanner(
                useBackCamera = currentCameraIsBack,
                enableLightBoost = isLightBoostOn
            ) { result ->
                // Visual update (Face Box) harus jalan terus
                faceBounds = result.bounds
                imageSize = result.imageSize
                imageRotation = result.rotation

                // Stop logic processing if showing result
                if (attendanceResult !is AttendanceResult.Idle) return@FaceScanner

                val currentFace = result.bounds.firstOrNull()
                if (currentFace == null) {
                    stabilityCounter = 0
                    candidateName = null
                    return@FaceScanner
                }

                if (result.embeddings.isNotEmpty()) {
                    val currentEmb = result.embeddings.first().second
                    var bestDist = 1.0f
                    var bestName: String? = null

                    for ((name, dbEmb) in gallery) {
                        val dist = NativeMath.cosineDistance(dbEmb, currentEmb)
                        if (dist < bestDist) {
                            bestDist = dist
                            bestName = name
                        }
                    }

                    // 🔥 THRESHOLD DIPERKETAT (0.55f)
                    val THRESHOLD = 0.55f 

                    if (bestDist < THRESHOLD && bestName != null) {
                        if (bestName == candidateName) {
                            stabilityCounter++
                        } else {
                            candidateName = bestName
                            stabilityCounter = 1
                        }

                        if (stabilityCounter >= REQUIRED_STABILITY) {
                            recognitionViewModel.processRecognition(bestName, activeSession)
                        }
                    } else {
                        // Reset if face is detected but not recognized within threshold
                        stabilityCounter = 0
                    }
                }
            }

            // ==========================================
            // 2. OVERLAY LAYER (Layer Tengah - Wajib di sini)
            // ==========================================
            FaceOverlay(
                faceBounds = faceBounds,
                imageSize = imageSize,
                imageRotation = imageRotation,
                isFrontCamera = !currentCameraIsBack,
                modifier = Modifier.fillMaxSize()
            )

            // ==========================================
            // 3. UI HUD & RESULTS (Layer Paling Atas)
            // ==========================================
            
            // Top Bar
            CheckInHUD(
                schoolName = activeUser.schoolName,
                statusMessage = getStatusMessage(attendanceResult),
                isFlashOn = isLightBoostOn,
                onFlashToggle = { isLightBoostOn = !isLightBoostOn },
                onCameraFlip = { currentCameraIsBack = !currentCameraIsBack }
            )

            // Bottom Result Card
            AttendanceResultCard(
                result = attendanceResult,
                activeSession = activeSession,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            )
        }
    }
}

// --- 🏗️ MODULAR UI COMPONENTS ---

@Composable
fun LoadingView(modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = AzuraPrimary)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Menyiapkan AI Scanner...", color = Color.White)
    }
}

@Composable
fun CheckInHUD(
    schoolName: String,
    statusMessage: String,
    isFlashOn: Boolean,
    onFlashToggle: () -> Unit,
    onCameraFlip: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(0.6f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = schoolName.uppercase(), 
                    style = MaterialTheme.typography.labelSmall, 
                    color = AzuraAccent, 
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = statusMessage, 
                    color = Color.White, 
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Row {
            IconButton(
                onClick = onFlashToggle, 
                modifier = Modifier.background(
                    if (isFlashOn) Color.Yellow.copy(0.3f) else Color.Black.copy(0.6f), 
                    RoundedCornerShape(50)
                )
            ) {
                Icon(Icons.Outlined.WbSunny, null, tint = if (isFlashOn) Color.Yellow else Color.White)
            }
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = onCameraFlip, 
                modifier = Modifier.background(Color.Black.copy(0.6f), RoundedCornerShape(50))
            ) {
                Icon(Icons.Default.FlipCameraAndroid, null, tint = Color.White)
            }
        }
    }
}

@Composable
fun AttendanceResultCard(result: AttendanceResult, activeSession: String, modifier: Modifier) {
    AnimatedVisibility(
        visible = result !is AttendanceResult.Idle,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier
    ) {
        val containerColor = when(result) {
            is AttendanceResult.Success -> AzuraPrimary
            is AttendanceResult.Unauthorized -> Color(0xFFD32F2F)
            is AttendanceResult.Cooldown -> Color(0xFF454545)
            is AttendanceResult.Error -> Color.DarkGray
            else -> Color.Gray
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = containerColor),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .shadow(16.dp, RoundedCornerShape(24.dp))
        ) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when(result) {
                        is AttendanceResult.Success -> Icons.Default.CheckCircle
                        is AttendanceResult.Unauthorized -> Icons.Default.Block
                        is AttendanceResult.Cooldown -> Icons.Default.Timer
                        else -> Icons.Default.Info
                    },
                    contentDescription = null, 
                    tint = Color.White, 
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    val title = when(result) {
                        is AttendanceResult.Success -> "ABSENSI BERHASIL"
                        is AttendanceResult.Unauthorized -> "AKSES DITOLAK"
                        is AttendanceResult.Cooldown -> "SUDAH ABSEN"
                        is AttendanceResult.Error -> "SYSTEM ERROR"
                        else -> ""
                    }
                    val name = when(result) {
                        is AttendanceResult.Success -> result.name
                        is AttendanceResult.Unauthorized -> result.name
                        is AttendanceResult.Cooldown -> result.name
                        is AttendanceResult.Error -> result.message
                        else -> ""
                    }
                    Text(title, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.8f))
                    Text(name, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(activeSession, style = MaterialTheme.typography.bodyMedium, color = AzuraAccent)
                    if (result is AttendanceResult.Unauthorized) {
                        Text("Bukan siswa kelas ini", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }
                }
            }
        }
    }
}

private fun getStatusMessage(result: AttendanceResult): String {
    return when(result) {
        is AttendanceResult.Idle -> "Mencari Wajah..."
        is AttendanceResult.Loading -> "Mengidentifikasi..."
        is AttendanceResult.Success -> "Berhasil!"
        is AttendanceResult.Unauthorized -> "Salah Kelas!"
        is AttendanceResult.Cooldown -> "Sudah Absen"
        is AttendanceResult.Error -> "Gagal"
    }
}