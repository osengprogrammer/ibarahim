package com.example.crashcourse.ui.add

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.PhotoCamera
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
import com.example.crashcourse.viewmodel.FaceViewModel
import com.example.crashcourse.utils.PhotoStorageUtils
import com.example.crashcourse.ui.components.* import com.example.crashcourse.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AddUserScreen(
    onNavigateBack: () -> Unit = {},
    onUserAdded: () -> Unit = {},
    viewModel: FaceViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Form States
    var name by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var embedding by remember { mutableStateOf<FloatArray?>(null) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    var isSubmitting by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) } // 🚀 New: Success Dialog
    
    // Overlay States
    var showFaceCapture by remember { mutableStateOf(false) }
    var captureMode by remember { mutableStateOf(CaptureMode.EMBEDDING) }

    // Fungsi untuk Reset Form setelah Sukses
    fun resetForm() {
        name = ""
        studentId = ""
        embedding = null
        capturedBitmap = null
        isSubmitting = false
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = AzuraBg
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Top
            ) {
                // --- HEADER ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AzuraTitle("Tambah Siswa")
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, "Close", tint = AzuraText)
                    }
                }
                
                Spacer(Modifier.height(16.dp))

                // --- INPUT SECTION ---
                Text("Informasi Dasar", style = MaterialTheme.typography.labelMedium, color = AzuraText.copy(alpha = 0.6f))
                Spacer(Modifier.height(8.dp))
                
                AzuraInput(
                    value = name,
                    onValueChange = { name = it },
                    label = "Nama Lengkap Siswa"
                )
                Spacer(Modifier.height(12.dp))
                AzuraInput(
                    value = studentId,
                    onValueChange = { studentId = it },
                    label = "Nomor Induk (NIK/NISN)"
                )
                
                Spacer(Modifier.height(24.dp))

                // --- BIOMETRIC CARD ---
                Text(
                    "Data Biometrik", 
                    style = MaterialTheme.typography.titleMedium,
                    color = AzuraText, // 🚀 Pastikan Hitam Tajam
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Scan Embedding Button
                        Button(
                            onClick = { 
                                captureMode = CaptureMode.EMBEDDING
                                showFaceCapture = true 
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (embedding == null) Color(0xFFF0F0F0) else AzuraSuccess,
                                contentColor = if (embedding == null) AzuraText else Color.White
                            )
                        ) {
                            Icon(Icons.Default.Fingerprint, null)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                if (embedding == null) "Scan Biometrik Wajah" else "Wajah Terverifikasi ✅",
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Take Photo Button
                        Button(
                            onClick = { 
                                captureMode = CaptureMode.PHOTO
                                showFaceCapture = true 
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (capturedBitmap == null) Color(0xFFF0F0F0) else AzuraSecondary,
                                contentColor = if (capturedBitmap == null) AzuraText else Color.White
                            )
                        ) {
                            Icon(Icons.Default.PhotoCamera, null)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                if (capturedBitmap == null) "Ambil Foto Profil" else "Foto Profil Tersimpan ✅",
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Preview Circle
                        if (capturedBitmap != null) {
                            Spacer(Modifier.height(8.dp))
                            Surface(
                                modifier = Modifier.size(110.dp),
                                shape = CircleShape,
                                border = BorderStroke(3.dp, AzuraAccent),
                                shadowElevation = 8.dp
                            ) {
                                Image(
                                    bitmap = capturedBitmap!!.asImageBitmap(),
                                    contentDescription = "Preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // --- SUBMIT ACTION ---
                AzuraButton(
                    text = "Simpan Data Siswa",
                    onClick = {
                        val finalName = name.trim()
                        val finalId = studentId.trim()
                        
                        if (embedding != null && capturedBitmap != null && finalName.isNotBlank() && finalId.isNotBlank()) {
                            isSubmitting = true
                            
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val photoUrl = PhotoStorageUtils.saveFacePhoto(context, capturedBitmap!!, finalId)
                                    
                                    if (photoUrl != null) {
                                        viewModel.registerFace(
                                            studentId = finalId,
                                            name = finalName,
                                            embedding = embedding!!,
                                            photoUrl = photoUrl,
                                            onSuccess = {
                                                scope.launch(Dispatchers.Main) {
                                                    showSuccessDialog = true // 🚀 Show confirmation dialog
                                                }
                                            },
                                            onDuplicate = { existing ->
                                                scope.launch(Dispatchers.Main) {
                                                    isSubmitting = false
                                                    snackbarHostState.showSnackbar("ID $existing sudah terdaftar!")
                                                }
                                            }
                                        )
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        isSubmitting = false
                                        snackbarHostState.showSnackbar("Gagal: ${e.message}")
                                    }
                                }
                            }
                        }
                    },
                    isLoading = isSubmitting,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // --- SUCCESS DIALOG ---
            if (showSuccessDialog) {
                AlertDialog(
                    onDismissRequest = { },
                    icon = { Icon(Icons.Default.CheckCircle, null, tint = AzuraSuccess, modifier = Modifier.size(64.dp)) },
                    title = { Text("Registrasi Berhasil", color = AzuraText, fontWeight = FontWeight.Bold) },
                    text = { Text("Data siswa atas nama $name telah berhasil disimpan ke database Azura AI.", textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
                    confirmButton = {
                        AzuraButton(
                            text = "Selesai",
                            onClick = {
                                showSuccessDialog = false
                                resetForm() // 🚀 BERSIHKAN SEMUA DATA (TERMASUK FOTO)
                                onUserAdded()
                            }
                        )
                    },
                    shape = RoundedCornerShape(28.dp),
                    containerColor = Color.White
                )
            }

            // Capture Overlay
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