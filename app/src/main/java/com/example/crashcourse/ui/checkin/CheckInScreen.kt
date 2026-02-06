package com.example.crashcourse.ui

import android.graphics.Rect
import android.media.AudioManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.WbSunny // ☀️ Sun Icon (Filled)
import androidx.compose.material.icons.outlined.WbSunny // ☀️ Sun Icon (Outlined)
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.crashcourse.db.FaceCache
import com.example.crashcourse.scanner.FaceScanner
import com.example.crashcourse.ui.components.FaceOverlay
import com.example.crashcourse.utils.NativeMath
import com.example.crashcourse.viewmodel.FaceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
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

    // 🚀 NEW STATE: Light Boost (ISO)
    var isLightBoostOn by remember { mutableStateOf(false) }

    // --- ⏱️ COOLDOWN STATE ---
    var successCountdown by remember { mutableIntStateOf(0) } 
    val isCoolingDown = successCountdown > 0

    // Status State
    var matchName by remember { mutableStateOf<String?>(null) }
    var alreadyCheckedIn by remember { mutableStateOf(false) }
    var secondsRemaining by remember { mutableIntStateOf(0) }
    
    // --- 🛡️ SECURITY STATE ---
    // 1. Liveness
    var livenessState by remember { mutableIntStateOf(0) } 
    var livenessMessage by remember { mutableStateOf("Position face") }
    var lastFaceDetectedTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var verifiedEmbedding by remember { mutableStateOf<FloatArray?>(null) }

    // 2. 🛡️ CONSENSUS (ANTI-FALSE POSITIVE)
    // We need to see the SAME person 3 times in a row to trust it.
    var candidateName by remember { mutableStateOf<String?>(null) }
    var stabilityCounter by remember { mutableIntStateOf(0) }
    val REQUIRED_STABILITY = 3 

    // UI Drawing State
    var faceBounds by remember { mutableStateOf<List<Rect>>(emptyList()) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    var imageRotation by remember { mutableIntStateOf(0) }

    val checkInTimestamps = remember { mutableStateMapOf<String, Long>() }
    val lastWaitSpokenTime = remember { mutableStateMapOf<String, Long>() } 
    
    val tts = remember { mutableStateOf<TextToSpeech?>(null) }
    val toneGen = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }

    // Initialize TTS
    LaunchedEffect(Unit) {
        val ttsObj = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.value?.language = Locale.US
                tts.value?.setSpeechRate(1.1f)
            }
        }
        tts.value = ttsObj
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
            }
        }
    }

    fun playSuccessSound() = toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 150)
    fun speak(msg: String) = tts.value?.speak(msg, TextToSpeech.QUEUE_FLUSH, null, null)

    // Load Gallery
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val loadedGallery = FaceCache.load(context)
            withContext(Dispatchers.Main) {
                gallery = loadedGallery
                loading = false
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (loading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        } else {
            // 1. CAMERA LAYER (With Light Boost)
            FaceScanner(
                useBackCamera = currentCameraIsBack,
                enableLightBoost = isLightBoostOn // 🚀 Pass the ISO state
            ) { result ->
                
                if (isCoolingDown) return@FaceScanner

                val now = System.currentTimeMillis()
                val currentFace = result.bounds.firstOrNull()

                // RESET IF FACE LOST
                if (currentFace == null) {
                    if (now - lastFaceDetectedTime > 1200) {
                        livenessState = 0
                        livenessMessage = "Position face"
                        verifiedEmbedding = null
                        // Reset stability
                        candidateName = null
                        stabilityCounter = 0
                    }
                } else {
                    lastFaceDetectedTime = now 
                }
                
                // LIVENESS DETECTION
                val eyesClosed = (result.leftEyeOpenProb ?: 1f) < 0.15f && (result.rightEyeOpenProb ?: 1f) < 0.15f
                val eyesOpen = (result.leftEyeOpenProb ?: 0f) > 0.80f && (result.rightEyeOpenProb ?: 0f) > 0.80f

                var nextLivenessState = livenessState
                var nextLivenessMsg = livenessMessage

                if (livenessState != 2 && currentFace != null && result.embeddings.isNotEmpty()) {
                    if (eyesClosed) {
                        nextLivenessState = 1 
                        nextLivenessMsg = "Blink detected..."
                    } else if (livenessState == 1 && eyesOpen) {
                        nextLivenessState = 2 
                        nextLivenessMsg = "Verified ✅"
                        verifiedEmbedding = result.embeddings.first().second
                        playSuccessSound()
                    } else if (livenessState == 0) {
                        nextLivenessMsg = "Blink to verify"
                    }
                }

                // RECOGNITION LOGIC
                var confirmedName: String? = null
                var shouldGreet = false
                
                if (result.embeddings.isNotEmpty() && nextLivenessState == 2 && verifiedEmbedding != null) {
                    val (_, currentEmbedding) = result.embeddings.first()

                    // 1. Identity Check
                    val identityMatch = NativeMath.cosineDistance(verifiedEmbedding!!, currentEmbedding)
                    
                    if (identityMatch > 0.30f) { 
                        nextLivenessState = 0
                        nextLivenessMsg = "Identity mismatch! Re-blink"
                        verifiedEmbedding = null
                        stabilityCounter = 0
                    } else {
                        // 2. DB Search
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

                        // 3. 🛡️ STRICT MATH CHECKS
                        val THRESHOLD = 0.75f 
                        val AMBIGUITY_GAP = 0.05f 
                        
                        if (bestDist < THRESHOLD && (secondBestDist - bestDist) > AMBIGUITY_GAP) {
                            
                            // 4. 🛡️ STABILITY CHECK
                            if (bestName == candidateName) {
                                stabilityCounter++
                            } else {
                                candidateName = bestName
                                stabilityCounter = 1 
                            }

                            if (stabilityCounter >= REQUIRED_STABILITY) {
                                confirmedName = bestName
                                shouldGreet = true
                            }
                        } else {
                            stabilityCounter = 0
                        }
                    }
                }

                // STATE UPDATE
                androidx.compose.runtime.snapshots.Snapshot.withMutableSnapshot {
                    faceBounds = result.bounds
                    imageSize = result.imageSize
                    imageRotation = result.rotation
                    livenessState = nextLivenessState
                    livenessMessage = nextLivenessMsg

                    if (shouldGreet && confirmedName != null) {
                        val name = confirmedName!!
                        val lastCheck = checkInTimestamps[name] ?: 0L
                        val diff = now - lastCheck
                        val COOLDOWN_MS = 20_000L 

                        if (diff > COOLDOWN_MS) {
                            // ✅ SUCCESS
                            checkInTimestamps[name] = now
                            viewModel.saveCheckIn(name) 
                            
                            matchName = name
                            alreadyCheckedIn = false
                            
                            // 🚀 Start Cooldown
                            successCountdown = 3 
                            
                            speak("Welcome $name")
                            
                            livenessState = 0 
                            verifiedEmbedding = null
                            stabilityCounter = 0
                            candidateName = null
                        } else {
                            // ALREADY CHECKED IN
                            matchName = name
                            alreadyCheckedIn = true
                            secondsRemaining = ((COOLDOWN_MS - diff) / 1000).toInt().coerceAtLeast(0)
                            
                            val lastSpoken = lastWaitSpokenTime[name] ?: 0L
                            if (now - lastSpoken > 10_000) {
                                speak("Please wait")
                                lastWaitSpokenTime[name] = now
                            }
                        }
                    }
                }
            }

            // 2. FACE BOX OVERLAY
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
            Box(Modifier.fillMaxSize().padding(16.dp)) {
                
                // TOP BAR
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(0.5f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("AZURA AI", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black, color = Color.White))
                    
                    val statusText = if(isCoolingDown) "NEXT IN $successCountdown..." else livenessMessage.uppercase()
                    val statusColor = if(isCoolingDown) Color.Cyan else if(livenessState == 2) Color.Green else Color.Yellow
                    
                    Text(statusText, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = statusColor))
                    
                    // RIGHT: Controls (ISO & Switch)
                    Row {
                        // ☀️ ISO BUTTON
                        IconButton(onClick = { isLightBoostOn = !isLightBoostOn }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = if (isLightBoostOn) Icons.Filled.WbSunny else Icons.Outlined.WbSunny,
                                contentDescription = "Boost Light",
                                tint = if (isLightBoostOn) Color.Yellow else Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        IconButton(onClick = { currentCameraIsBack = !currentCameraIsBack }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.CameraAlt, "Switch", tint = Color.White)
                        }
                    }
                }

                // BOTTOM CARD (Notification)
                AnimatedVisibility(
                    visible = matchName != null,
                    enter = slideInVertically(initialOffsetY = { 100 }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { 100 }) + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    val isWarning = alreadyCheckedIn
                    val cardColor = if (isWarning) Color(0xFFFFF3E0) else Color(0xFFE8F5E9)
                    val borderColor = if (isWarning) Color(0xFFFF9800) else Color(0xFF4CAF50)

                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp))
                            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if(isWarning) Icons.Default.Face else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = borderColor,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column {
                                Text(
                                    text = if(isWarning) "ALREADY CHECKED IN" else "VERIFIED",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color.Gray)
                                )
                                Text(
                                    text = matchName ?: "Unknown",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, color = Color.Black)
                                )
                                if (isWarning) {
                                    Text("Wait ${secondsRemaining}s", style = MaterialTheme.typography.bodySmall.copy(color = Color.Red))
                                } else if (isCoolingDown) {
                                    Text("Next scan ready in ${successCountdown}s", style = MaterialTheme.typography.bodySmall.copy(color = borderColor))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}