package com.example.crashcourse.scanner

import android.Manifest
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.crashcourse.ml.FaceAnalyzer
import com.example.crashcourse.ui.PermissionsHandler
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun FaceScanner(
    useBackCamera: Boolean = false,
    enableLightBoost: Boolean = false, // 🚀 NEW: ISO/Brightness Control
    onResult: (FaceAnalyzer.FaceResult) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    var showLowLightWarning by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) } // 🚀 Capture Camera Ref

    // 🚀 CONTROL ISO/EXPOSURE
    LaunchedEffect(camera, enableLightBoost) {
        camera?.cameraControl?.let { control ->
            val exposureState = camera?.cameraInfo?.exposureState
            if (exposureState != null && exposureState.isExposureCompensationSupported) {
                val range = exposureState.exposureCompensationRange
                // If boost is ON, set to Max Brightness. If OFF, set to 0 (Normal).
                val targetIndex = if (enableLightBoost) range.upper else 0
                control.setExposureCompensationIndex(targetIndex)
            }
        }
    }

    PermissionsHandler(permissionState = cameraPermissionState) {
        val executor = remember { Executors.newSingleThreadExecutor() }
        val mainHandler = remember { Handler(Looper.getMainLooper()) }

        DisposableEffect(Unit) { onDispose { executor.shutdown() } }

        val analyzer = remember {
            FaceAnalyzer { result ->
                mainHandler.post { 
                    showLowLightWarning = result.isLowLight 
                    onResult(result) 
                }
            }
        }
        DisposableEffect(Unit) { onDispose { analyzer.close() } }

        val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }

        LaunchedEffect(cameraPermissionState.status, useBackCamera) {
            if (cameraPermissionState.status.isGranted) {
                try {
                    val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                    
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { it.setAnalyzer(executor, analyzer) }

                    val selector = if (useBackCamera) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
                    cameraProvider.unbindAll()
                    
                    // 🚀 Save Camera Reference
                    camera = cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
                    
                } catch (e: Exception) {
                    Log.e("FaceScanner", "Camera binding failed", e)
                }
            }
        }

        Box(Modifier.fillMaxSize()) { 
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize()) 
            
            // Only show warning if Boost is OFF. If Boost is ON, we assume user knows.
            AnimatedVisibility(
                visible = showLowLightWarning && !enableLightBoost,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Low Light",
                            tint = Color.Yellow,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Too Dark", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Try the Light Boost button", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}