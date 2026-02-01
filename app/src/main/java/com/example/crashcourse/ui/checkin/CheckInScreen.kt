package com.example.crashcourse.ui

import android.graphics.Rect
import android.media.AudioManager
import android.media.ToneGenerator
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.math.abs

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

    // 🛡️ SECURITY: Identity Lock (Identity + Location)
    var verifiedEmbedding by remember { mutableStateOf<FloatArray?>(null) }
    var lastVerifiedBounds by remember { mutableStateOf<Rect?>(null) }

    var faceBounds by remember { mutableStateOf<List<Rect>>(emptyList()) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    var imageRotation by remember { mutableStateOf(0) }

    val checkInTimestamps = remember { mutableStateMapOf<String, Long>() }
    val lastWaitSpokenTime = remember { mutableStateMapOf<String, Long>() } 
    
    val tts = remember { mutableStateOf<TextToSpeech?>(null) }
    val toneGen = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }

    LaunchedEffect(Unit) {
        TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.value?.language = Locale.US
                tts.value?.setSpeechRate(1.1f)
            }
        }.also { tts.value = it }
    }

    fun playSuccessSound() = toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 150)
    fun speak(msg: String) = tts.value?.speak(msg, TextToSpeech.QUEUE_FLUSH, null, null)

    // ✅ OPTIMIZATION: Load 500+ Students in Background Thread
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            // Heavy DB operation happens here (Background)
            val loadedGallery = FaceCache.load(context)
            
            // UI Update happens here (Main Thread)
            withContext(Dispatchers.Main) {
                gallery = loadedGallery
                loading = false
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (loading) {
            // While loading in background, show this spinner
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        } else {
            FaceScanner(useBackCamera = currentCameraIsBack) { result ->
                val now = System.currentTimeMillis()
                val currentFace = result.bounds.firstOrNull()

                // 1. TIMEOUT & RESET
                if (currentFace == null) {
                    if (matchName != null) { matchName = null }
                    if (now - lastFaceDetectedTime > 1200) {
                        livenessState = 0
                        livenessMessage = "Position face"
                        verifiedEmbedding = null
                        lastVerifiedBounds = null
                    }
                } else {
                    lastFaceDetectedTime = now 
                }
                
                // 2. LIVENESS DETECTION
                val eyesClosed = (result.leftEyeOpenProb ?: 1f) < 0.15f && (result.rightEyeOpenProb ?: 1f) < 0.15f
                val eyesOpen = (result.leftEyeOpenProb ?: 0f) > 0.80f && (result.rightEyeOpenProb ?: 0f) > 0.80f

                var nextLivenessState = livenessState
                var nextLivenessMsg = livenessMessage

                if (livenessState != 2 && currentFace != null && result.embeddings.isNotEmpty()) {
                    if (eyesClosed) {
                        nextLivenessState = 1 
                        nextLivenessMsg = "Blink detected..."
                    } else if (livenessState == 1 && eyesOpen) {
                        // 🛡️ LOCK IDENTITY
                        nextLivenessState = 2 
                        nextLivenessMsg = "Verified ✅"
                        verifiedEmbedding = result.embeddings.first().second
                        lastVerifiedBounds = currentFace
                        playSuccessSound()
                        speak("Verified")
                    } else if (livenessState == 0) {
                        nextLivenessMsg = "Blink to verify"
                    }
                }

                // 3. RECOGNITION MATH
                var calculatedBestName: String? = null
                var shouldGreet = false
                
                if (result.embeddings.isNotEmpty() && nextLivenessState == 2 && verifiedEmbedding != null) {
                    val (_, currentEmbedding) = result.embeddings.first()

                    // 🛡️ SECURITY CHECK 1: Identity Consistency
                    val identityMatch = NativeMath.cosineDistance(verifiedEmbedding!!, currentEmbedding)
                    
                    if (identityMatch > 0.25f) {
                        nextLivenessState = 0
                        nextLivenessMsg = "Identity mismatch! Re-blink"
                        verifiedEmbedding = null
                    } else {
                        // 🛡️ SECURITY CHECK 2: Search Database
                        var bestDist = Float.MAX_VALUE
                        var secondBestDist = Float.MAX_VALUE
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

                        if (bestDist < 0.30f && (secondBestDist - bestDist) > 0.10f) {    
                            calculatedBestName = bestName
                            shouldGreet = true
                        }
                    }
                }

                // 4. UI & DB UPDATE
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
                            // Reset for next person
                            livenessState = 0 
                            verifiedEmbedding = null
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
                    } else if (livenessState == 2 && calculatedBestName == null && result.embeddings.isNotEmpty()) {
                        isRegistered = false
                    }
                }
            }

            // --- UI LAYERS ---
            if (imageSize != IntSize.Zero) {
                FaceOverlay(faceBounds, imageSize, imageRotation, !currentCameraIsBack, Modifier.fillMaxSize())
            }

            // TOP BAR UI
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, start = 12.dp, end = 12.dp).align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color.Black.copy(0.5f), RoundedCornerShape(24.dp)).padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("AZURA", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Color.White))
                    Text(livenessMessage.uppercase(), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = if (livenessState == 2) Color.Green else Color.Yellow))
                    IconButton(onClick = { currentCameraIsBack = !currentCameraIsBack }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.CameraAlt, "Switch", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                AnimatedVisibility(
                    visible = matchName != null,
                    enter = slideInVertically(initialOffsetY = { -50 }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -50 }) + fadeOut()
                ) {
                    val isWarning = alreadyCheckedIn
                    Surface(shape = RoundedCornerShape(20.dp), color = if (isWarning) Color(0xFFFFC107) else Color(0xFF00BCD4), shadowElevation = 6.dp) {
                        Text(text = if (isWarning) "COOLDOWN: ${secondsRemaining}s" else "SUCCESS: $matchName", modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold, color = Color.Black))
                    }
                }
            }
        }
    }
}