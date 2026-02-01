package com.example.crashcourse.ui

import android.graphics.Rect
import android.speech.tts.TextToSpeech
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.crashcourse.db.FaceCache
import com.example.crashcourse.scanner.FaceScanner
import com.example.crashcourse.utils.NativeMath
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
    var secondsRemaining by remember { mutableIntStateOf(0) }
    
    // LIVENESS STATE
    var livenessState by remember { mutableStateOf(0) } 
    var livenessMessage by remember { mutableStateOf("Position face") }
    var lastFaceDetectedTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var faceBounds by remember { mutableStateOf<List<Rect>>(emptyList()) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    var imageRotation by remember { mutableStateOf(0) }

    val checkInTimestamps = remember { mutableStateMapOf<String, Long>() }
    val lastWaitSpokenTime = remember { mutableStateMapOf<String, Long>() } 
    val tts = remember { mutableStateOf<TextToSpeech?>(null) }

    LaunchedEffect(Unit) {
        TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.value?.language = Locale.US
                tts.value?.setSpeechRate(1.1f)
            }
        }.also { tts.value = it }
    }

    fun speak(msg: String) = tts.value?.speak(msg, TextToSpeech.QUEUE_FLUSH, null, null)

    LaunchedEffect(Unit) {
        gallery = FaceCache.load(context)
        loading = false
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (loading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        } else {
            FaceScanner(useBackCamera = currentCameraIsBack) { result ->
                val now = System.currentTimeMillis()

                // 1. INSTANT RESET LOGIC
                if (result.embeddings.isEmpty()) {
                    if (matchName != null) { matchName = null }
                    if (now - lastFaceDetectedTime > 1500) {
                        livenessState = 0
                        livenessMessage = "Position face"
                        secondsRemaining = 0
                    }
                } else {
                    lastFaceDetectedTime = now 
                }
                
                // 2. LIVENESS
                val leftEye = result.leftEyeOpenProb ?: -1f
                val rightEye = result.rightEyeOpenProb ?: -1f
                val eyesOpen = (leftEye > 0.80f && rightEye > 0.80f)
                val eyesClosed = (leftEye < 0.15f && rightEye < 0.15f) 

                var nextLivenessState = livenessState
                var nextLivenessMsg = livenessMessage

                if (livenessState != 2 && result.embeddings.isNotEmpty()) {
                    if (eyesClosed) {
                        nextLivenessState = 1 
                        nextLivenessMsg = "Blink detected..."
                    } else if (livenessState == 1 && eyesOpen) {
                        nextLivenessState = 2 
                        nextLivenessMsg = "Verified ✅"
                        speak("Verified")
                    } else if (livenessState == 0) {
                        nextLivenessMsg = "Blink to verify"
                    }
                }

                // 3. RECOGNITION (NATIVE C++)
                var calculatedBestName: String? = null
                var shouldGreet = false
                
                if (result.embeddings.isNotEmpty() && nextLivenessState == 2) {
                    val (_, embedding) = result.embeddings.first()
                    var bestDist = Float.MAX_VALUE
                    var secondBestDist = Float.MAX_VALUE
                    var bestName: String? = null

                    for ((name, dbEmbedding) in gallery) {
                        val dist = NativeMath.cosineDistance(dbEmbedding, embedding)
                        if (dist < bestDist) {
                            secondBestDist = bestDist
                            bestDist = dist
                            bestName = name
                        } else if (dist < secondBestDist) {
                            secondBestDist = dist
                        }
                    }

                    if (bestDist < 0.32f && (secondBestDist - bestDist) > 0.12f) {    
                        calculatedBestName = bestName
                        shouldGreet = true
                    }
                }

                // 4. ATOMIC UI UPDATE
                androidx.compose.runtime.snapshots.Snapshot.withMutableSnapshot {
                    faceBounds = result.bounds
                    imageSize = result.imageSize
                    imageRotation = result.rotation
                    livenessState = nextLivenessState
                    livenessMessage = nextLivenessMsg

                    if (shouldGreet && calculatedBestName != null) {
                        val name = calculatedBestName!!
                        val lastCheck = checkInTimestamps[name] ?: 0L
                        val diff = now - lastCheck
                        val COOLDOWN_MS = 20_000L 

                        if (diff > COOLDOWN_MS) {
                            checkInTimestamps[name] = now
                            viewModel.saveCheckIn(name) 
                            matchName = name
                            isRegistered = true
                            alreadyCheckedIn = false
                            speak("Welcome $name")
                            livenessState = 0 
                        } else {
                            matchName = name
                            alreadyCheckedIn = true
                            secondsRemaining = ((COOLDOWN_MS - diff) / 1000).toInt().coerceAtLeast(0)
                            val lastSpoken = lastWaitSpokenTime[name] ?: 0L
                            if (now - lastSpoken > 10_000) {
                                speak("Please wait")
                                lastWaitSpokenTime[name] = now
                            }
                        }
                    } else if (livenessState == 2 && calculatedBestName == null) {
                        isRegistered = false
                    }
                }
            }

            // --- UI LAYERS ---

            if (imageSize != IntSize.Zero) {
                FaceOverlay(faceBounds, imageSize, imageRotation, !currentCameraIsBack, Modifier.fillMaxSize())
            }

            // 🟢 UNIFIED TOP BAR
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 12.dp, end = 12.dp)
                    .align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(0.5f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo
                    Text(
                        text = "AZURA",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Black, 
                            letterSpacing = 2.sp,
                            color = Color.White
                        )
                    )

                    // Liveness Status Badge
                    Text(
                        text = livenessMessage.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (livenessState == 2) Color.Green else Color.Yellow
                        )
                    )

                    // Camera Toggle Button
                    IconButton(
                        onClick = { currentCameraIsBack = !currentCameraIsBack },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, "Switch", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 🟢 ANIMATED PILL NOTIFICATION (Success / Cooldown)
                AnimatedVisibility(
                    visible = matchName != null,
                    enter = slideInVertically(initialOffsetY = { -50 }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -50 }) + fadeOut()
                ) {
                    val isWarning = alreadyCheckedIn
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isWarning) Color(0xFFFFC107) else Color(0xFF00BCD4),
                        shadowElevation = 6.dp
                    ) {
                        Text(
                            text = if (isWarning) "COOLDOWN: ${secondsRemaining}s" else "SUCCESS: $matchName",
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Black
                            )
                        )
                    }
                }
            }

            // UNREGISTERED ALERT
            if (!isRegistered && livenessState == 2 && faceBounds.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 120.dp)
                        .background(Color.Red.copy(0.8f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("USER NOT REGISTERED", color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}