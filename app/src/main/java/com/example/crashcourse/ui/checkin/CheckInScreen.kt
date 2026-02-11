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
import com.example.crashcourse.viewmodel.AuthState
import com.example.crashcourse.viewmodel.AuthViewModel
import com.example.crashcourse.viewmodel.FaceViewModel
import kotlinx.coroutines.delay
import java.util.*

/**
 * 👁️ Azura Tech AI Check-In Screen
 * Fitur:
 * 1. Liveness Detection (Cegah foto HP)
 * 2. Scoped Recognition (Hanya siswa sekolah tsb)
 * 3. Anti-Spam (Cooldown 30 detik per siswa)
 */
@Composable
fun CheckInScreen(
    useBackCamera: Boolean,
    faceViewModel: FaceViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    
    // --- 🔐 AUTH & SESSION CONTEXT ---
    val authState by authViewModel.authState.collectAsStateWithLifecycle()

    // Camera & UI States
    var currentCameraIsBack by remember { mutableStateOf(useBackCamera) }
    var gallery by remember { mutableStateOf<List<Pair<String, FloatArray>>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var isLightBoostOn by remember { mutableStateOf(false) }

    // Logic States
    var matchName by remember { mutableStateOf<String?>(null) }
    var alreadyCheckedIn by remember { mutableStateOf(false) }
    var cooldownSeconds by remember { mutableIntStateOf(0) }
    
    // Liveness (Anti-Photo) States
    // 0: Cari Wajah, 1: Wajah Ditemukan (Minta Kedip), 2: Kedip Terdeteksi (Verifikasi), 3: Selesai
    var livenessState by remember { mutableIntStateOf(0) }
    var livenessMessage by remember { mutableStateOf("Cari Wajah...") }

    // Consensus (Agar tidak flicker)
    var candidateName by remember { mutableStateOf<String?>(null) }
    var stabilityCounter by remember { mutableIntStateOf(0) }
    val REQUIRED_STABILITY = 2 // Butuh 2 frame berturut-turut yang sama

    // Face Drawing States
    var faceBounds by remember { mutableStateOf<List<Rect>>(emptyList()) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    var imageRotation by remember { mutableIntStateOf(0) }

    // Anti-Spam & Audio
    // Map lokal untuk mencegah spam request ke DB/Firestore (Cache session)
    val checkInTimestamps = remember { mutableMapOf<String, Long>() }
    val toneGen = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    // --- 🎙️ INITIALIZE TTS ---
    DisposableEffect(Unit) {
        val ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale("id", "ID"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Bahasa Indonesia tidak didukung")
                }
            }
        }
        tts = ttsInstance
        onDispose {
            ttsInstance.stop()
            ttsInstance.shutdown()
        }
    }

    // --- 📦 LOAD DATA SISWA (Scoped per Sekolah) ---
    LaunchedEffect(authState) {
        if (authState is AuthState.Active) {
            val user = authState as AuthState.Active
            loading = true
            
            // 🔄 Refresh RAM Cache dari DB Lokal
            FaceCache.refresh(context)
            
            // 🛡️ SECURITY FILTER: Hanya load embedding milik sekolah ini
            val filteredFaces = FaceCache.getFaces().filter { face ->
                val isSameSchool = face.sekolahId == user.sekolahId
                // Jika Guru biasa (bukan Admin), filter berdasarkan kelas yg diajar (Opsional)
                // val isScoped = if (user.role == "ADMIN") true else user.assignedClasses.contains(face.className)
                
                isSameSchool // && isScoped (Aktifkan jika ingin strict per kelas)
            }

            gallery = filteredFaces.map { it.name to it.embedding }
            loading = false
            Log.d("CheckInScreen", "Loaded ${gallery.size} faces for School: ${user.schoolName}")
        }
    }

    // --- ⏲️ RESET TIMER UI ---
    // Menghilangkan popup sukses setelah 3 detik
    LaunchedEffect(matchName) {
        if (matchName != null && !alreadyCheckedIn) {
            delay(3000L)
            matchName = null
            livenessState = 0
            stabilityCounter = 0
            livenessMessage = "Cari Wajah..."
        } else if (matchName != null && alreadyCheckedIn) {
            // Jika cooldown, hilangkan lebih cepat
            delay(2000L)
            matchName = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (loading || authState !is AuthState.Active) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = AzuraPrimary)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Memuat Data Wajah...", color = Color.White)
            }
        } else {
            val activeUser = authState as AuthState.Active

            // =================================================
            // 1. SCANNER ENGINE (CAMERA PREVIEW)
            // =================================================
            FaceScanner(
                useBackCamera = currentCameraIsBack,
                enableLightBoost = isLightBoostOn
            ) { result ->
                // Jika sedang menampilkan hasil sukses, pause scanning sebentar
                if (matchName != null && !alreadyCheckedIn) return@FaceScanner

                faceBounds = result.bounds
                imageSize = result.imageSize
                imageRotation = result.rotation

                val currentFace = result.bounds.firstOrNull()
                
                // --- KONDISI: TIDAK ADA WAJAH ---
                if (currentFace == null) {
                    stabilityCounter = 0
                    livenessState = 0
                    livenessMessage = "Cari Wajah..."
                    return@FaceScanner
                }

                // --- DETEKSI KEDIP (LIVENESS) ---
                // Ambang batas probabilitas mata terbuka (0.0 - 1.0)
                val leftOpen = result.leftEyeOpenProb ?: 1.0f
                val rightOpen = result.rightEyeOpenProb ?: 1.0f
                
                val eyesClosed = leftOpen < 0.25f && rightOpen < 0.25f
                val eyesOpen = leftOpen > 0.85f && rightOpen > 0.85f

                if (livenessState == 0) {
                    livenessMessage = "Silakan Kedip 😉"
                    if (eyesClosed) livenessState = 1 // Mata tertutup terdeteksi
                } else if (livenessState == 1) {
                    if (eyesOpen) {
                        livenessState = 2 // Mata terbuka kembali -> VALID BLINK
                        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                        livenessMessage = "Mengidentifikasi..."
                    }
                }

                // --- PENGENALAN WAJAH (Hanya jika Liveness Lolos) ---
                if (livenessState == 2 && result.embeddings.isNotEmpty()) {
                    val currentEmb = result.embeddings.first().second
                    
                    var bestDist = 1.0f
                    var bestName: String? = null

                    // 🔍 Linear Search di Gallery (Cukup cepat untuk < 500 siswa)
                    for ((name, dbEmb) in gallery) {
                        val dist = NativeMath.cosineDistance(dbEmb, currentEmb)
                        if (dist < bestDist) {
                            bestDist = dist
                            bestName = name
                        }
                    }

                    // Ambang Batas Kemiripan (0.4 - 0.6 biasanya optimal untuk MobileFaceNet)
                    // Semakin kecil = semakin ketat.
                    val THRESHOLD = 0.65f 

                    if (bestDist < THRESHOLD && bestName != null) {
                        if (bestName == candidateName) {
                            stabilityCounter++
                        } else {
                            candidateName = bestName
                            stabilityCounter = 1
                        }

                        // Butuh konsistensi beberapa frame agar tidak salah baca
                        if (stabilityCounter >= REQUIRED_STABILITY) {
                            val now = System.currentTimeMillis()
                            val lastCheck = checkInTimestamps[bestName] ?: 0L
                            val diff = now - lastCheck
                            val COOLDOWN_MS = 30_000L // 30 Detik Cooldown

                            if (diff > COOLDOWN_MS) {
                                // ✅ SUKSES CHECK-IN
                                checkInTimestamps[bestName] = now
                                
                                // Panggil ViewModel untuk simpan ke Room & Firestore
                                faceViewModel.saveCheckInByName(bestName) 
                                
                                matchName = bestName
                                alreadyCheckedIn = false
                                toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 200)
                                tts?.speak("Selamat datang, $bestName", TextToSpeech.QUEUE_FLUSH, null, null)
                            } else {
                                // ⏳ SPAM DETECTED
                                matchName = bestName
                                alreadyCheckedIn = true
                                cooldownSeconds = ((COOLDOWN_MS - diff) / 1000).toInt()
                            }
                            
                            // Reset state scanning setelah match ditemukan
                            livenessState = 0 
                            stabilityCounter = 0
                        }
                    } else if (bestDist > 0.80f) {
                        livenessMessage = "Wajah Tidak Dikenal"
                    }
                }
            }

            // =================================================
            // 2. OVERLAY LAYER (KOTAK WAJAH)
            // =================================================
            FaceOverlay(
                faceBounds = faceBounds,
                imageSize = imageSize,
                imageRotation = imageRotation,
                isFrontCamera = !currentCameraIsBack,
                modifier = Modifier.fillMaxSize()
            )

            // =================================================
            // 3. UI HUD & FEEDBACK
            // =================================================
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                
                // --- TOP BAR ---
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Info Sekolah & Status
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Text(
                                text = activeUser.schoolName.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = AzuraAccent,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = livenessMessage,
                                color = when(livenessState) {
                                    0 -> Color.White
                                    1 -> Color.Yellow
                                    2 -> Color.Green
                                    else -> Color.White
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Tombol Kontrol (Flash & Camera Switch)
                    Row {
                        IconButton(
                            onClick = { isLightBoostOn = !isLightBoostOn },
                            modifier = Modifier
                                .background(
                                    if (isLightBoostOn) Color.Yellow.copy(0.3f) else Color.Black.copy(0.6f), 
                                    shape = RoundedCornerShape(50)
                                )
                        ) {
                            Icon(
                                Icons.Outlined.WbSunny, 
                                contentDescription = "Flash", 
                                tint = if (isLightBoostOn) Color.Yellow else Color.White
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = { currentCameraIsBack = !currentCameraIsBack },
                            modifier = Modifier
                                .background(Color.Black.copy(0.6f), shape = RoundedCornerShape(50))
                        ) {
                            Icon(
                                Icons.Default.FlipCameraAndroid, 
                                contentDescription = "Switch Cam", 
                                tint = Color.White
                            )
                        }
                    }
                }

                // --- BOTTOM RESULT CARD ---
                AnimatedVisibility(
                    visible = matchName != null,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (alreadyCheckedIn) Color(0xFF333333) else AzuraPrimary
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(16.dp, RoundedCornerShape(24.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (alreadyCheckedIn) Icons.Default.Timer else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (alreadyCheckedIn) Color.Yellow else Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = if (alreadyCheckedIn) "SUDAH ABSEN HARI INI" else "ABSENSI BERHASIL",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (alreadyCheckedIn) Color.Yellow else Color.White.copy(0.8f)
                                )
                                Text(
                                    text = matchName ?: "",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                if (alreadyCheckedIn) {
                                    Text(
                                        text = "Dapat absen lagi dalam ${cooldownSeconds}s",
                                        color = Color.White.copy(0.6f),
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
}