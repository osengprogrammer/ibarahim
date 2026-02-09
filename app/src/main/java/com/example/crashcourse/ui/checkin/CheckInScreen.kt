// 🚀 INI WAJIB: Harus .ui.checkin agar MainScreen bisa menemukannya
package com.example.crashcourse.ui.checkin

import android.graphics.Rect
import android.media.AudioManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.crashcourse.db.FaceCache
import com.example.crashcourse.scanner.FaceScanner
import com.example.crashcourse.ui.components.FaceOverlay
import com.example.crashcourse.ui.theme.*
import com.example.crashcourse.utils.NativeMath
import com.example.crashcourse.viewmodel.FaceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CheckInScreen(
    useBackCamera: Boolean,
    viewModel: FaceViewModel = viewModel()
) {
    // ... (SISA KODE SAMA SEPERTI SEBELUMNYA, TIDAK PERLU DIUBAH) ...
    // ... (PASTIKAN ISI KODENYA LENGKAP) ...
    val context = LocalContext.current
    var currentCameraIsBack by remember { mutableStateOf(useBackCamera) }
    var gallery by remember { mutableStateOf<List<Pair<String, FloatArray>>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    // 🚀 ISO / Light Boost State
    var isLightBoostOn by remember { mutableStateOf(false) }

    // --- ⏱️ COOLDOWN STATE ---
    var successCountdown by remember { mutableIntStateOf(0) }
    val isCoolingDown = successCountdown > 0

    // Status State
    var matchName by remember { mutableStateOf<String?>(null) }
    var alreadyCheckedIn by remember { mutableStateOf(false) }
    var secondsRemaining by remember { mutableIntStateOf(0) }

    // --- 🛡️ SECURITY & LIVENESS STATE ---
    var livenessState by remember { mutableIntStateOf(0) } // 0: Init, 1: Blinked, 2: Verified
    var livenessMessage by remember { mutableStateOf("Position face") }
    var lastFaceDetectedTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var verifiedEmbedding by remember { mutableStateOf<FloatArray?>(null) }

    // --- 🛡️ CONSENSUS (ANTI-FALSE POSITIVE) ---
    var candidateName by remember { mutableStateOf<String?>(null) }
    var stabilityCounter by remember { mutableIntStateOf(0) }
    val REQUIRED_STABILITY = 3

    // UI Drawing State
    var faceBounds by remember { mutableStateOf<List<Rect>>(emptyList()) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    var imageRotation by remember { mutableIntStateOf(0) }

    // Timestamps
    val checkInTimestamps = remember { mutableStateMapOf<String, Long>() }
    val lastWaitSpokenTime = remember { mutableStateMapOf<String, Long>() }

    // Audio & TTS
    val toneGen = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    // Initialize TTS
    DisposableEffect(Unit) {
        val ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setSpeechRate(1.1f)
            }
        }
        tts = ttsInstance
        onDispose {
            ttsInstance.stop()
            ttsInstance.shutdown()
        }
    }

    // Helper Functions
    fun playSuccessSound() = toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 150)
    fun speak(msg: String) {
        tts?.speak(msg, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    // 🔄 COUNTDOWN LOGIC
    LaunchedEffect(successCountdown) {
        if (successCountdown > 0) {
            delay(1000L)
            successCountdown -= 1
            if (successCountdown == 0) {
                matchName = null
                candidateName = null
                stabilityCounter = 0
                livenessState = 0
                verifiedEmbedding = null
            }
        }
    }

    // Load Local Face Gallery
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val loadedGallery = FaceCache.load(context)
            withContext(Dispatchers.Main) {
                gallery = loadedGallery
                loading = false
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = AzuraPrimary)
        } else {
            // 1. CAMERA LAYER
            FaceScanner(
                useBackCamera = currentCameraIsBack,
                enableLightBoost = isLightBoostOn
            ) { result ->
                // This callback runs on every frame analysis
                if (isCoolingDown) return@FaceScanner

                val now = System.currentTimeMillis()
                val currentFace = result.bounds.firstOrNull()

                // Update UI State for Overlay
                faceBounds = result.bounds
                imageSize = result.imageSize
                imageRotation = result.rotation

                // Face Loss Handling
                if (currentFace == null) {
                    if (now - lastFaceDetectedTime > 1200) {
                        livenessState = 0
                        livenessMessage = "Position face"
                        verifiedEmbedding = null
                        candidateName = null
                        stabilityCounter = 0
                    }
                    return@FaceScanner
                } else {
                    lastFaceDetectedTime = now
                }

                // Blink Logic
                // Note: result.leftEyeOpenProb is nullable, default to 1f (open) if null
                val leftEye = result.leftEyeOpenProb ?: 1f
                val rightEye = result.rightEyeOpenProb ?: 1f
                
                val eyesClosed = leftEye < 0.15f && rightEye < 0.15f
                val eyesOpen = leftEye > 0.80f && rightEye > 0.80f

                if (livenessState != 2 && result.embeddings.isNotEmpty()) {
                    if (eyesClosed) {
                        livenessState = 1
                        livenessMessage = "Blink detected..."
                    } else if (livenessState == 1 && eyesOpen) {
                        livenessState = 2
                        livenessMessage = "Verified ✅"
                        verifiedEmbedding = result.embeddings.first().second
                        playSuccessSound()
                    } else if (livenessState == 0) {
                        livenessMessage = "Blink to verify"
                    }
                }

                // AI Recognition Logic
                if (result.embeddings.isNotEmpty() && livenessState == 2 && verifiedEmbedding != null) {
                    val currentEmbedding = result.embeddings.first().second
                    
                    // Verify if the current face still matches the one that blinked
                    val identityMatch = NativeMath.cosineDistance(verifiedEmbedding!!, currentEmbedding)

                    if (identityMatch > 0.30f) { // Threshold for "same person" continuity
                        livenessState = 0
                        livenessMessage = "Identity mismatch! Re-blink"
                        verifiedEmbedding = null
                        stabilityCounter = 0
                    } else {
                        // RECOGNITION AGAINST GALLERY
                        var bestDist = 10f
                        var secondBestDist = 10f
                        var bestName: String? = null

                        for ((name, dbEmbedding) in gallery) {
                            val dist = NativeMath.cosineDistance(dbEmbedding, currentEmbedding)
                            if (dist < bestDist) {
                                secondBestDist = bestDist
                                bestDist = dist
                                bestName = name
                            } else if (dist < secondBestDist) {
                                secondBestDist = dist
                            }
                        }

                        val MATCH_THRESHOLD = 0.75f // Adjust based on your model
                        val AMBIGUITY_GAP = 0.05f

                        if (bestDist < MATCH_THRESHOLD && (secondBestDist - bestDist) > AMBIGUITY_GAP) {
                            if (bestName == candidateName) {
                                stabilityCounter++
                            } else {
                                candidateName = bestName
                                stabilityCounter = 1
                            }

                            if (stabilityCounter >= REQUIRED_STABILITY && bestName != null) {
                                // CONFIRMED MATCH
                                val name = bestName
                                val lastCheck = checkInTimestamps[name] ?: 0L
                                val diff = now - lastCheck
                                val COOLDOWN_MS = 20_000L // 20 seconds cooldown

                                if (diff > COOLDOWN_MS) {
                                    // New Check-in
                                    checkInTimestamps[name] = now
                                    viewModel.saveCheckIn(name)
                                    
                                    matchName = name
                                    alreadyCheckedIn = false
                                    successCountdown = 3 // Start UI cooldown
                                    speak("Welcome $name")
                                    
                                    // Reset Liveness for next user
                                    livenessState = 0
                                    verifiedEmbedding = null
                                    stabilityCounter = 0
                                    candidateName = null
                                } else {
                                    // Already checked in recently
                                    matchName = name
                                    alreadyCheckedIn = true
                                    secondsRemaining = ((COOLDOWN_MS - diff) / 1000).toInt().coerceAtLeast(0)

                                    val lastSpoken = lastWaitSpokenTime[name] ?: 0L
                                    if (now - lastSpoken > 5_000) { // Speak warning every 5s
                                        speak("Please wait")
                                        lastWaitSpokenTime[name] = now
                                    }
                                }
                            }
                        } else {
                            stabilityCounter = 0
                        }
                    }
                }
            }

            // 2. FACE OVERLAY
            if (imageSize != IntSize.Zero) {
                FaceOverlay(
                    faceBounds = faceBounds,
                    imageSize = imageSize,
                    imageRotation = imageRotation,
                    isFrontCamera = !currentCameraIsBack,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 3. UI OVERLAYS
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {

                // --- TOP BAR ---
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "AZURA AI",
                                style = MaterialTheme.typography.labelLarge.copy(color = AzuraAccent)
                            )
                            val statusColor = if (isCoolingDown) AzuraAccent else if (livenessState == 2) Color.Green else Color.Yellow
                            Text(
                                text = if (isCoolingDown) "COOLDOWN: $successCountdown" else livenessMessage.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(color = statusColor)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Light Boost Toggle
                            IconButton(
                                onClick = { isLightBoostOn = !isLightBoostOn },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = if (isLightBoostOn) AzuraAccent.copy(0.2f) else Color.Transparent
                                )
                            ) {
                                Icon(
                                    imageVector = if (isLightBoostOn) Icons.Filled.WbSunny else Icons.Outlined.WbSunny,
                                    contentDescription = "Light Boost",
                                    tint = if (isLightBoostOn) Color.Yellow else Color.White
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            // Camera Switch
                            IconButton(onClick = { currentCameraIsBack = !currentCameraIsBack }) {
                                Icon(Icons.Default.CameraAlt, "Switch Camera", tint = Color.White)
                            }
                        }
                    }
                }

                // --- BOTTOM NOTIFICATION CARD ---
                AnimatedVisibility(
                    visible = matchName != null,
                    enter = slideInVertically(initialOffsetY = { 100 }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { 100 }) + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    val isWarning = alreadyCheckedIn

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isWarning) Color(0xFF1E1E1E) else AzuraPrimary
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                            .shadow(20.dp, RoundedCornerShape(24.dp)),
                        border = BorderStroke(2.dp, if (isWarning) AzuraError else AzuraAccent)
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar Icon Background
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    imageVector = if (isWarning) Icons.Default.Face else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (isWarning) Color.White else AzuraAccent,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text(
                                    text = if (isWarning) "DUPLICATE SCAN" else "SUCCESS VERIFIED",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isWarning) AzuraError else AzuraAccent
                                    )
                                )
                                Text(
                                    text = matchName ?: "Unknown",
                                    style = MaterialTheme.typography.headlineMedium.copy(color = Color.White)
                                )
                                val time = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()) }
                                Text(
                                    text = if (isWarning) "Please wait ${secondsRemaining}s" else "Check-in at $time",
                                    color = Color.LightGray,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}