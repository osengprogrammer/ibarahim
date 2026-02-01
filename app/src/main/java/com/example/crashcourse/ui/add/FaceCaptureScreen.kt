package com.example.crashcourse.ui.add

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.crashcourse.ml.FaceAnalyzer
import com.example.crashcourse.ui.FaceOverlay
import com.example.crashcourse.ui.PermissionsHandler
import com.example.crashcourse.utils.toBitmap
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

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

    // =============================
    // Phase-1 ATOMIC STATE
    // =============================
    var faceBounds by remember { mutableStateOf<List<Rect>>(emptyList()) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    var imageRotation by remember { mutableStateOf(0) }
    var lastEmbedding by remember { mutableStateOf<FloatArray?>(null) }

    // UI Feedback states
    var isProcessing by remember { mutableStateOf(false) }
    var showCaptureFeedback by remember { mutableStateOf(false) }
    var captureSuccess by remember { mutableStateOf(false) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Animations
    val flashAlpha = remember { Animatable(0f) }
    val checkmarkScale = remember { Animatable(0.5f) }

    val imageCapture = remember { ImageCapture.Builder().build() }
    var useFrontCamera by remember { mutableStateOf(true) }

    // =============================
    // PHASE-1 COMPLIANT ANALYZER
    // =============================
    val analyzer = remember {
        FaceAnalyzer { result ->
            // Law L3: Update facts atomically to prevent UI "jank" or crashes
            androidx.compose.runtime.snapshots.Snapshot.withMutableSnapshot {
                faceBounds = result.bounds
                imageSize = result.imageSize
                imageRotation = result.rotation

                // Registration focus: capture the primary embedding
                val first = result.embeddings.firstOrNull()
                lastEmbedding = first?.second
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
            analyzer.close()
        }
    }

    // Capture Feedback Animation Loop
    LaunchedEffect(showCaptureFeedback) {
        if (showCaptureFeedback) {
            flashAlpha.animateTo(1f, tween(100))
            delay(50)
            flashAlpha.animateTo(0f, tween(300))
            checkmarkScale.animateTo(1.2f, spring(Spring.DampingRatioLowBouncy))
            checkmarkScale.animateTo(1f, tween(300))
            delay(1500)
            showCaptureFeedback = false
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
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val analysis = ImageAnalysis.Builder()
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
                        Log.e("FaceCapture", "Binding failed", e)
                    }
                }
            }

            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        }

        // =============================
        // FACE OVERLAY (Physics Guarded)
        // =============================
        if (imageSize != IntSize.Zero) {
            FaceOverlay(
                faceBounds = faceBounds,
                imageSize = imageSize,
                imageRotation = imageRotation,
                isFrontCamera = useFrontCamera,
                modifier = Modifier.fillMaxSize(),
                paddingFactor = 0.15f
            )
        }

        // Flash layer
        Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = flashAlpha.value)))

        // Capture feedback overlay
        if (showCaptureFeedback) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (captureSuccess) {
                        if (mode == CaptureMode.PHOTO && capturedBitmap != null) {
                            Image(
                                bitmap = capturedBitmap!!.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.size(160.dp).clip(CircleShape)
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                        Icon(
                            Icons.Default.Check, null, 
                            tint = Color.Green, 
                            modifier = Modifier.size(100.dp).scale(checkmarkScale.value)
                        )
                        Text("Captured!", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                    } else {
                        CircularProgressIndicator(color = Color.Green)
                        Text("Processing...", color = Color.White, modifier = Modifier.padding(top = 16.dp))
                    }
                }
            }
        }

        // Status Header
        Text(
            text = if (faceBounds.isEmpty()) "🔍 Position face in frame" else "✅ Ready to capture",
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 64.dp)
                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                .padding(horizontal = 24.dp, vertical = 12.dp)
        )

        // Action controls
        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 60.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) {
                Text("Cancel")
            }

            Button(
                onClick = {
                    if (isProcessing) return@Button
                    isProcessing = true
                    showCaptureFeedback = true
                    captureSuccess = false

                    coroutineScope.launch {
                        when (mode) {
                            CaptureMode.EMBEDDING -> {
                                delay(500) // UX delay to show "Processing"
                                lastEmbedding?.let { 
                                    onEmbeddingCaptured(it)
                                    captureSuccess = true
                                }
                                delay(1000)
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
                                            isProcessing = false
                                            showCaptureFeedback = false
                                        }
                                    }
                                )
                            }
                        }
                    }
                },
                enabled = (mode == CaptureMode.PHOTO || lastEmbedding != null) && !isProcessing,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF008080))
            ) {
                Text(if (mode == CaptureMode.EMBEDDING) "Register Face" else "Take Photo")
            }
        }

        // Switch Camera
        IconButton(
            onClick = { useFrontCamera = !useFrontCamera },
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape)
        ) {
            Icon(Icons.Default.CameraAlt, null, tint = Color.White)
        }
    }
}