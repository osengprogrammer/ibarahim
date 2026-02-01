package com.example.crashcourse.ui

import android.content.Context
import android.graphics.Rect
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.crashcourse.db.FaceCache
import com.example.crashcourse.scanner.FaceScanner
import com.example.crashcourse.utils.cosineDistance
import com.example.crashcourse.viewmodel.FaceViewModel
import java.util.*

@Composable
fun CheckInScreen(
    useBackCamera: Boolean,
    viewModel: FaceViewModel = viewModel()
) {
    val context = LocalContext.current
    var currentCameraIsBack by remember { mutableStateOf(useBackCamera) }
    var gallery by remember { mutableStateOf<List<Pair<String, FloatArray>>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    // Status State
    var matchName by remember { mutableStateOf<String?>(null) }
    var isRegistered by remember { mutableStateOf(true) }
    var alreadyCheckedIn by remember { mutableStateOf(false) }

    // Phase-2: Precision Tuning State
    var currentDistance by remember { mutableStateOf(1.0f) }

    // 🛡️ PHASE-3: LIVENESS STATE MACHINE
    // 0 = Waiting for face, 1 = Face detected (Ask for blink), 2 = Blink detected (Verified)
    var livenessState by remember { mutableStateOf(0) }
    var livenessMessage by remember { mutableStateOf("Position your face") }

    // Face Overlay State
    var faceBounds by remember { mutableStateOf<List<Rect>>(emptyList()) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    var imageRotation by remember { mutableStateOf(0) }

    val checkInTimestamps = remember { mutableStateMapOf<String, Long>() }
    val tts = remember { mutableStateOf<TextToSpeech?>(null) }

    // TTS Init
    LaunchedEffect(Unit) {
        TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.value?.language = Locale.US
                tts.value?.setSpeechRate(1.0f)
            }
        }.also { tts.value = it }

        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.setStreamVolume(AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0)
    }

    fun speak(msg: String) {
        tts.value?.speak(msg, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    DisposableEffect(Unit) {
        onDispose { tts.value?.shutdown() }
    }

    LaunchedEffect(Unit) {
        gallery = FaceCache.load(context)
        loading = false
    }

    Box(Modifier.fillMaxSize()) {
        if (loading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        } else {
            // ==========================================
            // PHASE-3 LIVENESS & RECOGNITION SCANNER
            // ==========================================
            FaceScanner(useBackCamera = currentCameraIsBack) { result ->
                
                // --- 1. PRE-CALCULATION (OUTSIDE SNAPSHOT) ---
                var calculatedBestName: String? = null
                var calculatedBestDist = 1.0f
                var shouldGreet = false
                
                // Liveness Data
                val leftEye = result.leftEyeOpenProb ?: -1f
                val rightEye = result.rightEyeOpenProb ?: -1f
                val isBlinking = leftEye < 0.15f && rightEye < 0.15f && leftEye != -1f
                val eyesFullyOpen = leftEye > 0.85f && rightEye > 0.85f

                var nextLivenessState = livenessState
                var nextLivenessMsg = livenessMessage

                if (result.embeddings.isNotEmpty()) {
                    // Logic State Machine
                    when (livenessState) {
                        0 -> {
                            nextLivenessState = 1
                            nextLivenessMsg = "Blink your eyes"
                        }
                        1 -> {
                            if (isBlinking) {
                                nextLivenessState = 2 // Move to Verified
                                nextLivenessMsg = "Verified! ✅"
                            }
                        }
                    }

                    // Only perform recognition if Liveness is VERIFIED (State 2)
                    if (nextLivenessState == 2) {
                        val (_, embedding) = result.embeddings.first()
                        var bestDist = Float.MAX_VALUE
                        var secondBestDist = Float.MAX_VALUE
                        var bestName: String? = null

                        for ((name, dbEmbedding) in gallery) {
                            val dist = cosineDistance(dbEmbedding, embedding)
                            if (dist < bestDist) {
                                secondBestDist = bestDist
                                bestDist = dist
                                bestName = name
                            } else if (dist < secondBestDist) {
                                secondBestDist = dist
                            }
                        }

                        val RECOGNITION_THRESHOLD = 0.40f 
                        val AMBIGUITY_MARGIN = 0.08f      

                        val isAmbiguous = (secondBestDist - bestDist) < AMBIGUITY_MARGIN
                        val isCloseEnough = bestDist < RECOGNITION_THRESHOLD

                        calculatedBestDist = bestDist
                        
                        if (bestName != null && isCloseEnough && !isAmbiguous) {
                            calculatedBestName = bestName
                            shouldGreet = true
                        }
                    }
                } else {
                    // Reset if face leaves the frame
                    nextLivenessState = 0
                    nextLivenessMsg = "Position your face"
                }

                // --- 2. ATOMIC UI UPDATE ---
                androidx.compose.runtime.snapshots.Snapshot.withMutableSnapshot {
                    faceBounds = result.bounds
                    imageSize = result.imageSize
                    imageRotation = result.rotation
                    currentDistance = calculatedBestDist
                    livenessState = nextLivenessState
                    livenessMessage = nextLivenessMsg

                    if (shouldGreet && calculatedBestName != null) {
                        val name = calculatedBestName!!
                        val now = System.currentTimeMillis()
                        val lastCheckIn = checkInTimestamps[name] ?: 0L

                        if (now - lastCheckIn > 60_000) {
                            checkInTimestamps[name] = now
                            matchName = name
                            isRegistered = true
                            alreadyCheckedIn = false

                            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                            val greeting = when (hour) {
                                in 5..11 -> "Good morning"
                                in 12..17 -> "Good afternoon"
                                else -> "Good evening"
                            }
                            speak("Thanks $name. $greeting")
                            
                            // 🛡️ RESET LIVENESS FOR NEXT PERSON
                            livenessState = 0 
                        } else {
                            matchName = name
                            isRegistered = true
                            alreadyCheckedIn = true
                        }
                    } else {
                        // Reset recognition labels if not currently greeting/success
                        if (shouldGreet == false) {
                            matchName = null
                            isRegistered = result.embeddings.isNotEmpty()
                            alreadyCheckedIn = false
                        }
                    }
                }
            }

            // ==========================================
            // UI OVERLAYS
            // ==========================================
            if (imageSize != IntSize.Zero) {
                FaceOverlay(
                    faceBounds = faceBounds,
                    imageSize = imageSize,
                    imageRotation = imageRotation,
                    isFrontCamera = !currentCameraIsBack,
                    modifier = Modifier.fillMaxSize(),
                    paddingFactor = 0.1f
                )
            }

            // 🛡️ LIVENESS MESSAGE BAR
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(bottom = 200.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = livenessMessage,
                    color = if (livenessState == 2) Color.Green else Color.Yellow,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }

            if (faceBounds.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 100.dp)
                        .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Score: ${"%.4f".format(currentDistance)}",
                        color = if (currentDistance < 0.40f) Color.Green else Color.Yellow,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Azura",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                IconButton(
                    onClick = { currentCameraIsBack = !currentCameraIsBack },
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Switch Camera",
                        tint = Color.White
                    )
                }
            }

            matchName?.let { name ->
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                Text(
                    text = if (alreadyCheckedIn) "$name Already Checkin" else "$name Checkin at $hour:00",
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 80.dp)
                        .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = Color(0xFF008080),
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}