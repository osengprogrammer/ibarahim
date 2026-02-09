package com.example.crashcourse.ui.add

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.crashcourse.ui.components.*
import com.example.crashcourse.ui.theme.*
import com.example.crashcourse.utils.PhotoProcessingUtils
import com.example.crashcourse.utils.PhotoStorageUtils
import com.example.crashcourse.viewmodel.FaceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleUploadScreen(
    onNavigateBack: () -> Unit,
    onUserAdded: () -> Unit,
    viewModel: FaceViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var name by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var embedding by remember { mutableStateOf<FloatArray?>(null) }
    
    var isProcessing by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            isProcessing = true
            scope.launch {
                val bmp = withContext(Dispatchers.IO) { PhotoStorageUtils.loadBitmapFromUri(context, it) }
                if (bmp != null) {
                    val result = withContext(Dispatchers.IO) { PhotoProcessingUtils.processBitmapForFaceEmbedding(context, bmp) }
                    if (result != null) {
                        capturedBitmap = result.first
                        embedding = result.second
                    } else {
                        snackbarHostState.showSnackbar("Wajah tidak terdeteksi.")
                    }
                }
                isProcessing = false
            }
        }
    }

    Scaffold(
        containerColor = AzuraBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Upload Galeri", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AzuraText)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            AzuraTitle("Registrasi Foto")
            Spacer(Modifier.height(32.dp))

            // Photo Box
            Box(
                Modifier.size(200.dp).clip(RoundedCornerShape(32.dp)).background(Color.White)
                    .clickable { if(!isProcessing) galleryLauncher.launch("image/*") }
                    .border(2.dp, if (capturedBitmap != null) AzuraSuccess else AzuraPrimary.copy(0.2f), RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (capturedBitmap != null) {
                    Image(capturedBitmap!!.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    Icon(Icons.Default.CheckCircle, null, Modifier.size(48.dp), tint = AzuraSuccess)
                } else {
                    if (isProcessing) CircularProgressIndicator(color = AzuraPrimary)
                    else Icon(Icons.Default.AddPhotoAlternate, null, Modifier.size(48.dp), tint = AzuraPrimary)
                }
            }

            Spacer(Modifier.height(32.dp))
            AzuraInput(value = name, onValueChange = { name = it }, label = "Nama Lengkap")
            Spacer(Modifier.height(12.dp))
            AzuraInput(value = studentId, onValueChange = { studentId = it }, label = "Nomor Induk")

            Spacer(Modifier.weight(1f))

            AzuraButton(
                text = "Simpan ke Database",
                isLoading = isProcessing,
                onClick = {
                    if (embedding != null && name.isNotBlank() && studentId.isNotBlank()) {
                        isProcessing = true
                        scope.launch {
                            val path = withContext(Dispatchers.IO) { PhotoStorageUtils.saveFacePhoto(context, capturedBitmap!!, studentId) }
                            if (path != null) {
                                viewModel.registerFace(studentId, name, embedding!!, path,
                                    onSuccess = { isProcessing = false; showSuccessDialog = true },
                                    onDuplicate = { isProcessing = false; scope.launch { snackbarHostState.showSnackbar("ID Duplikat!") } }
                                )
                            }
                        }
                    }
                },
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        if (showSuccessDialog) {
            AlertDialog(
                onDismissRequest = { },
                confirmButton = { AzuraButton("Kembali", onClick = { showSuccessDialog = false; onUserAdded() }) },
                title = { Text("Berhasil!") },
                text = { Text("Murid $name telah terdaftar.") },
                shape = RoundedCornerShape(28.dp)
            )
        }
    }
}