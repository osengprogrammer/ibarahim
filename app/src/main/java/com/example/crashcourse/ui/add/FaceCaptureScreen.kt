package com.example.crashcourse.ui.add

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import android.util.Size // 🚀 Diperlukan untuk setTargetResolution
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.crashcourse.ml.FaceAnalyzer
import com.example.crashcourse.ui.components.*
import com.example.crashcourse.ui.PermissionsHandler
import com.example.crashcourse.ui.theme.*
import com.example.crashcourse.utils.toBitmap
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * 📸 FaceCaptureScreen V.18.0 - Eagle Eye Registration
 * Update: Sinkronisasi resolusi 720p & perbaikan 'Unresolved reference close'.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun FaceCaptureScreen(
    mode: CaptureMode,
    onClose: () -> Unit,
    onEmbeddingCaptured: (FloatArray) -> Unit = {},
    onPhotoCaptured: (Bitmap) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val executor = remember { Executors.newSingleThreadExecutor() }
    val coroutineScope = rememberCoroutineScope()

    // --- Biometric States ---
    var faceBounds by remember { mutableStateOf<List<Rect>>(emptyList()) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    var imageRotation by remember { mutableIntStateOf(0) }
    var lastEmbedding by remember { mutableStateOf<FloatArray?>(null) }

    // --- UI Logic States ---
    var isProcessing by remember { mutableStateOf(false) }
    var showCaptureFeedback by remember { mutableStateOf(false) }
    var captureSuccess by remember { mutableStateOf(false) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val flashAlpha = remember { Animatable(0f) }
    val checkmarkScale = remember { Animatable(0.5f) }
    
    // 🚀 ImageCapture juga kita set agar hasilnya tajam
    val imageCapture = remember { 
        ImageCapture.Builder()
            .setTargetResolution(Size(720, 1280))
            .build() 
    }
    
    var useFrontCamera by remember { mutableStateOf(true) }

    // ✅ Integrasi FaceAnalyzer (Sekarang sudah punya fungsi close() di dalamnya)
    val analyzer = remember {
        FaceAnalyzer { result ->
            androidx.compose.runtime.snapshots.Snapshot.withMutableSnapshot {
                faceBounds = result.bounds
                imageSize = result.imageSize
                imageRotation = result.rotation
                lastEmbedding = result.embeddings.firstOrNull()?.second
            }
        }
    }

    // ✅ PEMBERSIHAN (Mencegah Memory Leak)
    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
            analyzer.close() // Sekarang sudah aman karena FaceAnalyzer.kt sudah kita perbaiki
        }
    }

    LaunchedEffect(showCaptureFeedback) {
        if (showCaptureFeedback) {
            flashAlpha.animateTo(1f, tween(100))
            delay(50)
            flashAlpha.animateTo(0f, tween(300))
            checkmarkScale.animateTo(1.2f, spring(Spring.DampingRatioLowBouncy))
            checkmarkScale.animateTo(1f, tween(300))
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        PermissionsHandler(permissionState = cameraPermissionState) {
            val previewView = remember { 
                PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } 
            }

            LaunchedEffect(cameraPermissionState.status.isGranted, useFrontCamera) {
                if (cameraPermissionState.status.isGranted) {
                    val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                    
                    // 🚀 EAGLE EYE RESOLUTION: Sinkronisasi 720p dengan Scanner
                    val preview = Preview.Builder()
                        .setTargetResolution(Size(720, 1280))
                        .build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        
                    val analysis = ImageAnalysis.Builder()
                        .setTargetResolution(Size(720, 1280))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build().also {
                            it.setAnalyzer(executor, analyzer)
                        }

                    val selector = if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA 
                                   else CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, analysis, imageCapture)
                    } catch (e: Exception) {
                        Log.e("FaceCapture", "Eagle Eye Binding failed", e)
                    }
                }
            }

            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        }

        // Overlay Deteksi Wajah
        if (imageSize != IntSize.Zero) {
            FaceOverlay(
                faceBounds = faceBounds,
                imageSize = imageSize,
                imageRotation = imageRotation,
                isFrontCamera = useFrontCamera,
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = flashAlpha.value)))

        // --- TOP HUD STATUS ---
        Surface(
            modifier = Modifier.align(Alignment.TopCenter).padding(24.dp).fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color.Black.copy(alpha = 0.6f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
        ) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                val statusText = if (faceBounds.isEmpty()) "MENCARI WAJAH..." else "WAJAH TERDETEKSI"
                val statusColor = if (faceBounds.isEmpty()) Color.Yellow else AzuraSuccess
                
                Box(Modifier.size(8.dp).background(statusColor, CircleShape))
                Spacer(Modifier.width(12.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelLarge.copy(color = Color.White, letterSpacing = 2.sp)
                )
            }
        }

        // --- SUCCESS FEEDBACK ---
        AnimatedVisibility(visible = showCaptureFeedback, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (captureSuccess) {
                        Surface(
                            modifier = Modifier.size(160.dp),
                            shape = CircleShape,
                            border = BorderStroke(4.dp, AzuraAccent),
                            color = Color.DarkGray
                        ) {
                            if (mode == CaptureMode.PHOTO && capturedBitmap != null) {
                                Image(
                                    bitmap = capturedBitmap!!.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Default.Check, null, tint = AzuraAccent, modifier = Modifier.size(80.dp).scale(checkmarkScale.value))
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        Text(
                            text = if (mode == CaptureMode.EMBEDDING) "BIOMETRIK TERSIMPAN" else "FOTO TERSIMPAN", 
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black, color = Color.White)
                        )
                    } else {
                        CircularProgressIndicator(color = AzuraAccent, strokeWidth = 6.dp)
                        Spacer(Modifier.height(16.dp))
                        Text("MENGEKSTRAK FITUR...", color = Color.White)
                    }
                }
            }
        }

        // --- ACTION BUTTONS ---
        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 60.dp).fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onClose,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("Batal")
            }

            AzuraButton(
                text = if (mode == CaptureMode.EMBEDDING) "Daftarkan" else "Ambil Foto",
                onClick = {
                    if (isProcessing) return@AzuraButton
                    if (mode == CaptureMode.EMBEDDING && faceBounds.isEmpty()) return@AzuraButton

                    isProcessing = true
                    showCaptureFeedback = true
                    captureSuccess = false

                    coroutineScope.launch {
                        when (mode) {
                            CaptureMode.EMBEDDING -> {
                                delay(800)
                                lastEmbedding?.let { 
                                    onEmbeddingCaptured(it)
                                    captureSuccess = true
                                }
                                delay(1200)
                                isProcessing = false
                                if (captureSuccess) onClose()
                            }
                            CaptureMode.PHOTO -> {
                                imageCapture.takePicture(
                                    executor,
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(image: ImageProxy) {
                                            val bitmap = image.toBitmap()
                                            capturedBitmap = bitmap
                                            onPhotoCaptured(bitmap)
                                            image.close()
                                            coroutineScope.launch { 
                                                captureSuccess = true
                                                delay(1500)
                                                isProcessing = false
                                                onClose() 
                                            }
                                        }
                                        override fun onError(exc: ImageCaptureException) { 
                                            Log.e("FaceCapture", "Capture failed", exc)
                                            isProcessing = false
                                            showCaptureFeedback = false 
                                        }
                                    }
                                )
                            }
                        }
                    }
                },
                modifier = Modifier.weight(1.5f),
                isLoading = isProcessing
            )
        }

        // Camera Flip Toggle
        IconButton(
            onClick = { useFrontCamera = !useFrontCamera },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp)
                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
        ) {
            Icon(Icons.Default.FlipCameraAndroid, null, tint = Color.White)
        }
    }
}