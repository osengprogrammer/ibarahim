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
import androidx.compose.material.icons.filled.Security
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
    
    // 🛡️ PHASE-3: LIVENESS STATE MACHINE
    var livenessState by remember { mutableStateOf(0) } 
    var livenessMessage by remember { mutableStateOf("Position your face") }
    var lastFaceDetectedTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var currentDistance by remember { mutableStateOf(1.0f) }
    var faceBounds by remember { mutableStateOf<List<Rect>>(emptyList()) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    var imageRotation by remember { mutableStateOf(0) }

    val checkInTimestamps = remember { mutableStateMapOf<String, Long>() }
    val tts = remember { mutableStateOf<TextToSpeech?>(null) }

    LaunchedEffect(Unit) {
        TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.value?.language = Locale.US
                tts.value?.setSpeechRate(1.0f)
            }
        }.also { tts.value = it }
    }

    fun speak(msg: String) {
        tts.value?.speak(msg, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    LaunchedEffect(Unit) {
        gallery = FaceCache.load(context)
        loading = false
    }

    Box(Modifier.fillMaxSize()) {
        if (loading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        } else {
            FaceScanner(useBackCamera = currentCameraIsBack) { result ->
                val now = System.currentTimeMillis()

                // 1. TIMEOUT LOGIC: If no face seen for 2 seconds, reset everything
                if (result.embeddings.isEmpty()) {
                    if (now - lastFaceDetectedTime > 2000) {
                        livenessState = 0
                        livenessMessage = "Position your face"
                        matchName = null
                    }
                } else {
                    lastFaceDetectedTime = now // Face found, update timer
                }
                
                // 2. LIVENESS SENSOR DATA
                val leftEye = result.leftEyeOpenProb ?: -1f
                val rightEye = result.rightEyeOpenProb ?: -1f
                val eyesOpen = (leftEye > 0.80f && rightEye > 0.80f)
                val eyesClosed = (leftEye < 0.15f && rightEye < 0.15f) // Blink Threshold

                var nextLivenessState = livenessState
                var nextLivenessMsg = livenessMessage

                if (livenessState != 2 && result.embeddings.isNotEmpty()) {
                    if (eyesClosed) {
                        nextLivenessState = 1 // Blink Detected
                        nextLivenessMsg = "Blink Detected..."
                    } else if (livenessState == 1 && eyesOpen) {
                        nextLivenessState = 2 // Verified
                        nextLivenessMsg = "Liveness Verified! ✅"
                        speak("Verified")
                    } else if (livenessState == 0) {
                        nextLivenessMsg = "Please Blink to Verify"
                    }
                }

                // 3. RECOGNITION (Only if Verified)
                var calculatedBestName: String? = null
                var calculatedBestDist = 1.0f
                var shouldGreet = false
                
                if (result.embeddings.isNotEmpty() && nextLivenessState == 2) {
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

                    calculatedBestDist = bestDist
                    if (bestDist < 0.40f && (secondBestDist - bestDist) > 0.08f) {
                        calculatedBestName = bestName
                        shouldGreet = true
                    }
                }

                // 4. UI UPDATE
                androidx.compose.runtime.snapshots.Snapshot.withMutableSnapshot {
                    faceBounds = result.bounds
                    imageSize = result.imageSize
                    imageRotation = result.rotation
                    currentDistance = calculatedBestDist
                    livenessState = nextLivenessState
                    livenessMessage = nextLivenessMsg

                    if (shouldGreet && calculatedBestName != null) {
                        val name = calculatedBestName!!
                        val lastCheckIn = checkInTimestamps[name] ?: 0L

                        if (now - lastCheckIn > 60_000) {
                            checkInTimestamps[name] = now
                            matchName = name
                            isRegistered = true
                            alreadyCheckedIn = false
                            speak("Welcome $name")
                            // Auto-reset state for the next scan after success
                            livenessState = 0
                        } else {
                            matchName = name
                            isRegistered = true
                            alreadyCheckedIn = true
                        }
                    } else if (livenessState == 2 && calculatedBestName == null) {
                        isRegistered = false
                    }
                }
            }

            // --- UI LAYERS ---

            // 1. Camera Preview & Overlays
            if (imageSize != IntSize.Zero) {
                FaceOverlay(faceBounds, imageSize, imageRotation, !currentCameraIsBack, Modifier.fillMaxSize())
            }

            // 2. HEADER: Azura & Camera Switch (Restored)
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp).align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Azura",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold, color = Color.White
                    ),
                    modifier = Modifier.background(Color.Black.copy(0.6f), CircleShape).padding(horizontal = 16.dp, vertical = 8.dp)
                )

                IconButton(
                    onClick = { currentCameraIsBack = !currentCameraIsBack },
                    modifier = Modifier.background(Color.Black.copy(0.6f), CircleShape)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Switch", tint = Color.White)
                }
            }

            // 3. LIVENESS MESSAGE (Below Header)
            Text(
                text = livenessMessage,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 90.dp)
                    .background(Color.Black.copy(alpha = 0.7f), CircleShape).padding(horizontal = 24.dp, vertical = 12.dp),
                style = MaterialTheme.typography.titleMedium,
                color = if (livenessState == 2) Color.Green else Color.Yellow,
                fontWeight = FontWeight.Bold
            )

            // 4. CHECK-IN SUCCESS MESSAGE (Center)
            matchName?.let { name ->
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(top = 80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (alreadyCheckedIn) "$name Already Checkin" else "Welcome $name",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = Color.Cyan, fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.background(Color.Black.copy(0.8f), CircleShape).padding(24.dp)
                    )
                }
            }

            // 5. UNREGISTERED ALERT
            if (!isRegistered && livenessState == 2 && faceBounds.isNotEmpty()) {
                Text(
                    text = "Not Registered",
                    modifier = Modifier.align(Alignment.Center).background(Color.Black.copy(0.7f), CircleShape).padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge.copy(color = Color.Red)
                )
            }
        }
    }
}