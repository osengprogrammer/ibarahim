package com.example.crashcourse.ui.add

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.crashcourse.db.MasterClassWithNames // 🚀 FIX: Pastikan import dari DB
import com.example.crashcourse.ui.components.*
import com.example.crashcourse.ui.theme.*
import com.example.crashcourse.utils.PhotoStorageUtils
import com.example.crashcourse.utils.showToast
import com.example.crashcourse.viewmodel.FaceViewModel
import com.example.crashcourse.viewmodel.MasterClassViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 📝 Azura Tech Add User Screen
 * Versi 6-Pilar: Mengintegrasikan unit rakitan ke dalam pendaftaran biometrik.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUserScreen(
    onNavigateBack: () -> Unit,      
    onUpdateSuccess: () -> Unit,    
    faceViewModel: FaceViewModel = viewModel(),
    masterClassVM: MasterClassViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // --- 📊 DATA OBSERVATION ---
    val masterClasses by masterClassVM.masterClassesWithNames.collectAsStateWithLifecycle(emptyList())
    // 🚀 FIX: Berikan tipe eksplisit agar tidak "Cannot infer type"
    var selectedClass by remember { mutableStateOf<MasterClassWithNames?>(null) }

    // --- FORM STATES ---
    var name by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var embedding by remember { mutableStateOf<FloatArray?>(null) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    var isSubmitting by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    
    // --- OVERLAY STATES ---
    var showFaceCapture by remember { mutableStateOf(false) }
    var captureMode by remember { mutableStateOf(CaptureMode.EMBEDDING) }

    fun resetForm() {
        name = ""
        studentId = ""
        selectedClass = null
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
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Top
            ) {
                // --- HEADER ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AzuraTitle("Registrasi Personel")
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, "Close", tint = AzuraText)
                    }
                }
                
                Spacer(Modifier.height(16.dp))

                // --- INPUT SECTION ---
                Text("Identitas Pribadi", style = MaterialTheme.typography.labelMedium, color = AzuraText.copy(alpha = 0.6f))
                Spacer(Modifier.height(8.dp))
                
                AzuraInput(value = name, onValueChange = { name = it }, label = "Nama Lengkap")
                Spacer(Modifier.height(12.dp))
                AzuraInput(value = studentId, onValueChange = { studentId = it }, label = "Nomor Induk (ID/NIK)")
                
                Spacer(Modifier.height(16.dp))

                // 🚀 UNIT DROPDOWN (Integrasi 6-Pilar)
                Text("Penempatan / Unit", style = MaterialTheme.typography.labelMedium, color = AzuraText.copy(alpha = 0.6f))
                Spacer(Modifier.height(8.dp))
                
                MasterClassDropdown(
                    options = masterClasses,
                    selected = selectedClass,
                    onSelect = { selectedClass = it },
                    label = "Pilih Unit Rakitan"
                )
                
                Spacer(Modifier.height(24.dp))

                // --- BIOMETRIC & PHOTO SECTION ---
                Text("Data Biometrik", style = MaterialTheme.typography.labelMedium, color = AzuraText.copy(alpha = 0.6f))
                Spacer(Modifier.height(12.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Scan Wajah (Embedding)
                        Button(
                            onClick = { captureMode = CaptureMode.EMBEDDING; showFaceCapture = true },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (embedding == null) Color(0xFFF8F8F8) else AzuraSuccess,
                                contentColor = if (embedding == null) AzuraText else Color.White
                            )
                        ) {
                            Icon(Icons.Default.Fingerprint, null)
                            Spacer(Modifier.width(12.dp))
                            Text(if (embedding == null) "Ambil Biometrik Wajah" else "Wajah Terverifikasi ✅")
                        }

                        // Ambil Foto Profil
                        Button(
                            onClick = { captureMode = CaptureMode.PHOTO; showFaceCapture = true },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (capturedBitmap == null) Color(0xFFF8F8F8) else AzuraSecondary,
                                contentColor = if (capturedBitmap == null) AzuraText else Color.White
                            )
                        ) {
                            Icon(Icons.Default.PhotoCamera, null)
                            Spacer(Modifier.width(12.dp))
                            Text(if (capturedBitmap == null) "Ambil Foto Profil" else "Foto Profil Tersimpan ✅")
                        }

                        if (capturedBitmap != null) {
                            Surface(
                                modifier = Modifier.size(110.dp).padding(top = 8.dp),
                                shape = CircleShape,
                                border = BorderStroke(3.dp, AzuraAccent)
                            ) {
                                Image(
                                    bitmap = capturedBitmap!!.asImageBitmap(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(40.dp))

                // --- SUBMIT ACTION ---
                AzuraButton(
                    text = "Simpan & Daftarkan",
                    onClick = {
                        val finalName = name.trim()
                        val finalId = studentId.trim()
                        
                        // Validation logic
                        if (selectedClass == null || embedding == null || capturedBitmap == null || finalName.isBlank() || finalId.isBlank()) {
                            scope.launch { snackbarHostState.showSnackbar("Harap lengkapi semua data dan biometrik!") }
                            return@AzuraButton
                        }

                        isSubmitting = true
                        scope.launch(Dispatchers.IO) {
                            try {
                                // 1. Save photo locally
                                val photoPath = PhotoStorageUtils.saveFacePhoto(context, capturedBitmap!!, finalId)
                                
                                if (photoPath != null) {
                                    // 2. Register Face with Unit support
                                    faceViewModel.registerFaceWithUnit(
                                        studentId = finalId,
                                        name = finalName,
                                        embedding = embedding!!,
                                        photoUrl = photoPath,
                                        unit = selectedClass!!, 
                                        onSuccess = {
                                            scope.launch(Dispatchers.Main) {
                                                isSubmitting = false
                                                showSuccessDialog = true 
                                            }
                                        },
                                        onDuplicate = { id ->
                                            scope.launch(Dispatchers.Main) {
                                                isSubmitting = false
                                                snackbarHostState.showSnackbar("ID $id sudah terdaftar!") 
                                            }
                                        }
                                    )
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isSubmitting = false
                                    snackbarHostState.showSnackbar("Gagal menyimpan: ${e.message}")
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
                    title = { Text("Registrasi Berhasil", fontWeight = FontWeight.Bold) },
                    text = { Text("$name telah terdaftar pada unit ${selectedClass?.className}.") },
                    confirmButton = {
                        AzuraButton(
                            text = "Selesai",
                            onClick = {
                                showSuccessDialog = false
                                resetForm()
                                onUpdateSuccess() 
                            }
                        )
                    },
                    shape = RoundedCornerShape(28.dp),
                    containerColor = Color.White
                )
            }

            // --- CAMERA OVERLAY ---
            if (showFaceCapture) {
                FaceCaptureScreen(
                    mode = captureMode,
                    onClose = { showFaceCapture = false },
                    onEmbeddingCaptured = { 
                        embedding = it
                        showFaceCapture = false 
                    },
                    onPhotoCaptured = { 
                        capturedBitmap = it 
                        showFaceCapture = false 
                    }
                )
            }
        }
    }
}