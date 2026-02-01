package com.example.crashcourse.ui.edit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import android.graphics.Bitmap
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crashcourse.db.FaceEntity
import com.example.crashcourse.utils.showToast
import com.example.crashcourse.viewmodel.OptionsViewModel
import com.example.crashcourse.viewmodel.FaceViewModel
import com.example.crashcourse.utils.PhotoStorageUtils
import com.example.crashcourse.ui.add.FaceCaptureScreen
import com.example.crashcourse.ui.add.CaptureMode
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditUserScreen(
    studentId: String,
    useBackCamera: Boolean = false,
    onNavigateBack: () -> Unit = {},
    onUserUpdated: () -> Unit = {},
    faceViewModel: FaceViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val allFaces by faceViewModel.faceList.collectAsStateWithLifecycle(emptyList())
    val user = allFaces.find { it.studentId == studentId }

    if (user == null) {
        ErrorStateScreen(
            title = "User Not Found",
            message = "The user with ID '$studentId' could not be found.",
            onNavigateBack = onNavigateBack
        )
        return
    }

    var name by remember(user) { mutableStateOf(user.name) }
    var embedding by remember { mutableStateOf<FloatArray?>(null) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    var showFaceCapture by remember { mutableStateOf(false) }
    var captureMode by remember { mutableStateOf(CaptureMode.EMBEDDING) }
    var currentPhotoBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(name) {
        nameError = when {
            name.isBlank() -> "Name is required"
            name.length < 2 -> "Name must be at least 2 characters"
            name.length > 50 -> "Name must be less than 50 characters"
            else -> null
        }
    }

    LaunchedEffect(name, embedding, capturedBitmap) {
        // mark unsaved whenever name changed or new embedding/photo captured
        hasUnsavedChanges = name != user.name || embedding != null || capturedBitmap != null
    }

    // Load existing photo once
    LaunchedEffect(user.photoUrl) {
        scope.launch {
            user.photoUrl?.let { path ->
                val bmp = withContext(Dispatchers.IO) {
                    PhotoStorageUtils.loadFacePhoto(path)
                }
                currentPhotoBitmap = bmp
            }
        }
    }

    val saveUser = {
        if (nameError == null && name.isNotBlank()) {
            isProcessing = true
            val updatedUser = user.copy(name = name.trim())
            // If embedding or a new photo was provided, use updateFaceWithPhoto to save both
            val finalEmbedding = embedding ?: user.embedding
            if (embedding != null || capturedBitmap != null) {
                // Resize photo before saving to reduce disk and memory usage
                val resizedBitmap = capturedBitmap?.let { PhotoStorageUtils.resizeBitmap(it, 800) }
                faceViewModel.updateFaceWithPhoto(
                    face = updatedUser,
                    photoBitmap = resizedBitmap,
                    embedding = finalEmbedding,
                    onComplete = {
                        isProcessing = false
                        context.showToast("User updated successfully!")
                        onUserUpdated()
                        onNavigateBack()
                    },
                    onError = { msg ->
                        isProcessing = false
                        context.showToast(msg)
                    }
                )
            } else {
                faceViewModel.updateFace(updatedUser) {
                    isProcessing = false
                    context.showToast("User updated successfully!")
                    onUserUpdated()
                    onNavigateBack()
                }
            }
        } else {
            context.showToast("Please fix the errors before saving")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Edit User")
                        if (hasUnsavedChanges) {
                            Text(
                                text = "Unsaved changes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = saveUser,
                        enabled = !isProcessing && nameError == null && name.isNotBlank() && hasUnsavedChanges
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Save")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // User ID (Read-only)
            OutlinedTextField(
                value = user.studentId,
                onValueChange = { },
                label = { Text("Student ID") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                supportingText = {
                    Text(
                        text = "Student ID cannot be changed",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            )

            // Name field with validation
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name *") },
                modifier = Modifier.fillMaxWidth(),
                isError = nameError != null,
                supportingText = nameError?.let { error ->
                    {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null)
                }
            )

            // Face & Photo card (allow embedding scan and photo capture)
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Face & Photo", style = MaterialTheme.typography.titleMedium)

                    Button(
                        onClick = {
                            captureMode = CaptureMode.EMBEDDING
                            showFaceCapture = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (embedding == null) "Scan Face for Embedding" else "Embedding Captured! \u2705")
                    }

                    Button(
                        onClick = {
                            captureMode = CaptureMode.PHOTO
                            showFaceCapture = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (capturedBitmap == null) "Capture Photo" else "Photo Captured! \u2705")
                    }

                    // Show current or newly captured photo preview
                    val displayBmp = capturedBitmap ?: currentPhotoBitmap
                    if (displayBmp != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                bitmap = displayBmp.asImageBitmap(),
                                contentDescription = "User photo",
                                modifier = Modifier
                                    .size(80.dp)
                                    .padding(end = 12.dp)
                                    .clip(MaterialTheme.shapes.medium)
                            )
                            Text(
                                text = if (capturedBitmap != null) "\u2705 New photo captured" else "Current photo",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
        // Face capture overlay (embedding or photo)
        if (showFaceCapture) {
            FaceCaptureScreen(
                mode = captureMode,
                onClose = { showFaceCapture = false },
                onEmbeddingCaptured = { embeddingArray ->
                    embedding = embeddingArray
                    showFaceCapture = false
                },
                onPhotoCaptured = { bitmap ->
                    capturedBitmap = bitmap
                    showFaceCapture = false
                }
            )
        }
    }
}

@Composable
fun ErrorStateScreen(
    title: String,
    message: String,
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onNavigateBack,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Go Back")
                }
            }
        }
    }
}
