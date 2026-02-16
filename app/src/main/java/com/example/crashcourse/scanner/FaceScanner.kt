package com.example.crashcourse.scanner

import android.Manifest
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.crashcourse.ml.FaceAnalyzer
import com.example.crashcourse.ui.PermissionsHandler
import com.example.crashcourse.ui.theme.* import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted
import java.util.concurrent.Executors

/**
 * 👁️ FaceScanner V.18.6 - Final Eagle Eye Mode
 * Fix: Unresolved reference 'close' & optimized for long-distance (50cm).
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun FaceScanner(
    useBackCamera: Boolean = false,
    enableLightBoost: Boolean = false,
    onResult: (FaceAnalyzer.FaceResult) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    var showLowLightWarning by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }

    // 🚀 CONTROL ISO/EXPOSURE
    LaunchedEffect(camera, enableLightBoost) {
        camera?.cameraControl?.let { control ->
            val exposureState = camera?.cameraInfo?.exposureState
            if (exposureState != null && exposureState.isExposureCompensationSupported) {
                val range = exposureState.exposureCompensationRange
                val targetIndex = if (enableLightBoost) range.upper else 0
                control.setExposureCompensationIndex(targetIndex)
            }
        }
    }

    PermissionsHandler(permissionState = cameraPermissionState) {
        val executor = remember { Executors.newSingleThreadExecutor() }
        val mainHandler = remember { Handler(Looper.getMainLooper()) }

        // ✅ PEMBERSIHAN EXECUTOR
        DisposableEffect(Unit) { 
            onDispose { 
                executor.shutdown() 
            } 
        }

        // ✅ ANALYZER INITIALIZATION
        // Kita gunakan FaceAnalyzer spesifik agar fungsi .close() terlihat oleh compiler
        val analyzer = remember {
            FaceAnalyzer { result ->
                mainHandler.post { 
                    showLowLightWarning = result.isLowLight 
                    onResult(result) 
                }
            }
        }

        // ✅ PEMBERSIHAN ANALYZER (Fix: Unresolved reference close)
        // Fungsi ini penting agar ML Kit detector tertutup saat user pindah layar
        DisposableEffect(analyzer) {
            onDispose {
                analyzer.close()
            }
        }

        val previewView = remember { 
            PreviewView(context).apply { 
                scaleType = PreviewView.ScaleType.FILL_CENTER 
            } 
        }

        LaunchedEffect(cameraPermissionState.status, useBackCamera) {
            if (cameraPermissionState.status.isGranted) {
                try {
                    val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                    
                    // 🚀 EAGLE EYE RESOLUTION: 720p
                    val preview = Preview.Builder()
                        .setTargetResolution(Size(720, 1280)) 
                        .build()
                        .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                    
                    val analysis = ImageAnalysis.Builder()
                        .setTargetResolution(Size(720, 1280)) 
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { it.setAnalyzer(executor, analyzer) }

                    val selector = if (useBackCamera) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
                    
                    cameraProvider.unbindAll()
                    camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner, 
                        selector, 
                        preview, 
                        analysis
                    )
                    
                } catch (e: Exception) {
                    Log.e("FaceScanner", "Eagle Eye binding failed", e)
                }
            }
        }

        Box(Modifier.fillMaxSize()) { 
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize()) 
            
            // 🚀 AZURA STYLE: Low Light Warning
            AnimatedVisibility(
                visible = showLowLightWarning && !enableLightBoost,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier.align(Alignment.Center)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, AzuraAccent.copy(alpha = 0.5f)),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Low Light",
                            tint = Color.Yellow,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "CAHAYA MINIM",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Gunakan Light Boost agar AI melihat lebih jelas",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.7f)
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}