package com.example.crashcourse.ui.add

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.crashcourse.viewmodel.FaceViewModel
import com.example.crashcourse.utils.PhotoStorageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * PHASE-1 STABILIZED ADD USER SCREEN
 * Ensures facts (embeddings/bitmaps) are stored correctly in the DB.
 */
@Composable
fun AddUserScreen(
    onNavigateBack: () -> Unit = {},
    onUserAdded: () -> Unit = {},
    viewModel: FaceViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // UI State
    var name by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var embedding by remember { mutableStateOf<FloatArray?>(null) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    // Process State
    var isSubmitting by remember { mutableStateOf(false) }
    var isSaved by remember { mutableStateOf(false) }
    
    // Overlay State
    var showFaceCapture by remember { mutableStateOf(false) }
    var captureMode by remember { mutableStateOf(CaptureMode.EMBEDDING) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .padding(paddingValues),
                verticalArrangement = Arrangement.Top
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Add New User", style = MaterialTheme.typography.headlineSmall)
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, "Close")
                    }
                }
                
                Spacer(Modifier.height(24.dp))

                // Input Fields
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; isSaved = false },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = studentId,
                    onValueChange = { studentId = it; isSaved = false },
                    label = { Text("Student ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(Modifier.height(24.dp))

                // Action Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Biometric Data", style = MaterialTheme.typography.titleMedium)
                        
                        // Embedding Capture
                        Button(
                            onClick = { 
                                captureMode = CaptureMode.EMBEDDING
                                showFaceCapture = true 
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (embedding == null) MaterialTheme.colorScheme.primary 
                                                else Color(0xFF008080)
                            )
                        ) {
                            Text(if (embedding == null) "Scan Face (Embedding)" else "Face Scanned ✅")
                        }

                        // Photo Capture
                        Button(
                            onClick = { 
                                captureMode = CaptureMode.PHOTO
                                showFaceCapture = true 
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (capturedBitmap == null) MaterialTheme.colorScheme.primary 
                                                else Color(0xFF008080)
                            )
                        ) {
                            Text(if (capturedBitmap == null) "Take Profile Photo" else "Photo Taken ✅")
                        }

                        if (capturedBitmap != null) {
                            Image(
                                bitmap = capturedBitmap!!.asImageBitmap(),
                                contentDescription = "Preview",
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                // Registration Action
                Button(
                    onClick = {
                        val finalName = name.trim()
                        val finalId = studentId.trim()
                        
                        if (embedding != null && capturedBitmap != null && finalName.isNotBlank() && finalId.isNotBlank()) {
                            isSubmitting = true
                            
                            scope.launch(Dispatchers.IO) {
                                try {
                                    // 1. Save File to Storage
                                    val photoUrl = PhotoStorageUtils.saveFacePhoto(context, capturedBitmap!!, finalId)
                                    
                                    if (photoUrl != null) {
                                        // 2. Register to Database
                                        viewModel.registerFace(
                                            studentId = finalId,
                                            name = finalName,
                                            embedding = embedding!!, // Law L1: already a cloned value
                                            photoUrl = photoUrl,
                                            onSuccess = {
                                                scope.launch(Dispatchers.Main) {
                                                    isSaved = true
                                                    isSubmitting = false
                                                    embedding = null
                                                    capturedBitmap = null
                                                    name = ""
                                                    studentId = ""
                                                    onUserAdded()
                                                }
                                            },
                                            onDuplicate = { existing ->
                                                scope.launch(Dispatchers.Main) {
                                                    isSubmitting = false
                                                    snackbarHostState.showSnackbar("ID already exists: $existing")
                                                }
                                            }
                                        )
                                    } else {
                                        withContext(Dispatchers.Main) {
                                            isSubmitting = false
                                            snackbarHostState.showSnackbar("Storage error: Could not save photo")
                                        }
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        isSubmitting = false
                                        snackbarHostState.showSnackbar("Registration failed: ${e.message}")
                                    }
                                }
                            }
                        }
                    },
                    enabled = !isSubmitting && embedding != null && capturedBitmap != null && name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Finish Registration")
                    }
                }
            }

            // Phase-1 Safe Capture Overlay
            if (showFaceCapture) {
                FaceCaptureScreen(
                    mode = captureMode,
                    onClose = { showFaceCapture = false },
                    onEmbeddingCaptured = { embedding = it },
                    onPhotoCaptured = { capturedBitmap = it }
                )
            }
        }
    }
}

enum class CaptureMode { EMBEDDING, PHOTO }