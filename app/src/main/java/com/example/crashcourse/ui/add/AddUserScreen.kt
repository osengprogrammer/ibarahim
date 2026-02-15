package com.example.crashcourse.ui.add

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.crashcourse.db.MasterClassWithNames
import com.example.crashcourse.ui.components.*
import com.example.crashcourse.ui.theme.*
import com.example.crashcourse.utils.PhotoStorageUtils
import com.example.crashcourse.viewmodel.FaceViewModel
import com.example.crashcourse.viewmodel.MasterClassViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 📝 Azura Tech Add User Screen (V.15.6 - Eagle Eye Synced)
 * Filosofi: Registrasi yang Presisi adalah Kunci Absensi yang Akurat.
 * Perubahan: Sinkronisasi dengan FaceCaptureScreen V.15.6 (Normalization Ready).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    
    // --- 🚀 MULTI-SELECT STATE ---
    val selectedRombels = remember { mutableStateListOf<MasterClassWithNames>() }
    var showRombelDialog by remember { mutableStateOf(false) }

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
        selectedRombels.clear()
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

                // --- 🚀 MULTI-SELECT ROMBEL SECTION ---
                Text("Daftar Mata Kuliah / Rombel", style = MaterialTheme.typography.labelMedium, color = AzuraText.copy(alpha = 0.6f))
                Spacer(Modifier.height(8.dp))
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = { showRombelDialog = true },
                        label = { Text("Tambah Matkul") },
                        leadingIcon = { Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp)) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = AzuraPrimary.copy(alpha = 0.1f))
                    )

                    selectedRombels.forEach { rombel ->
                        InputChip(
                            selected = true,
                            onClick = { },
                            label = { Text(rombel.className) },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close, 
                                    null, 
                                    Modifier.size(16.dp).clickable { selectedRombels.remove(rombel) }
                                )
                            }
                        )
                    }
                }
                
                if (selectedRombels.isEmpty()) {
                    Text(
                        "* Pilih minimal satu mata kuliah", 
                        style = MaterialTheme.typography.bodySmall, 
                        color = Color.Red.copy(alpha = 0.7f)
                    )
                }
                
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
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
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
                        
                        if (selectedRombels.isEmpty() || embedding == null || capturedBitmap == null || finalName.isBlank() || finalId.isBlank()) {
                            scope.launch { snackbarHostState.showSnackbar("Harap lengkapi identitas, matkul, dan biometrik!") }
                            return@AzuraButton
                        }

                        isSubmitting = true
                        scope.launch(Dispatchers.IO) {
                            try {
                                val photoPath = PhotoStorageUtils.saveFacePhoto(context, capturedBitmap!!, finalId)
                                
                                if (photoPath != null) {
                                    faceViewModel.registerFaceWithMultiUnit(
                                        studentId = finalId,
                                        name = finalName,
                                        embedding = embedding!!,
                                        units = selectedRombels.toList(),
                                        photoUrl = photoPath,
                                        onSuccess = {
                                            scope.launch(Dispatchers.Main) {
                                                isSubmitting = false
                                                showSuccessDialog = true 
                                            }
                                        },
                                        onDuplicateId = { id ->
                                            scope.launch(Dispatchers.Main) {
                                                isSubmitting = false
                                                snackbarHostState.showSnackbar("Gagal: ID $id sudah digunakan!") 
                                            }
                                        },
                                        onSimilarFace = { existingName ->
                                            scope.launch(Dispatchers.Main) {
                                                isSubmitting = false
                                                snackbarHostState.showSnackbar("Wajah sangat mirip dengan $existingName di database!")
                                            }
                                        },
                                        onError = { errorMessage ->
                                            scope.launch(Dispatchers.Main) {
                                                isSubmitting = false
                                                snackbarHostState.showSnackbar("Sistem: $errorMessage")
                                            }
                                        }
                                    )
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isSubmitting = false
                                    snackbarHostState.showSnackbar("Gagal Simpan: ${e.message}")
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
                    text = { Text("$name berhasil terdaftar pada ${selectedRombels.size} mata kuliah.") },
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

            if (showRombelDialog) {
                RombelSelectionDialog(
                    allClasses = masterClasses,
                    alreadySelected = selectedRombels,
                    onDismiss = { showRombelDialog = false },
                    onSave = { newSelection ->
                        selectedRombels.clear()
                        selectedRombels.addAll(newSelection)
                        showRombelDialog = false
                    }
                )
            }

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