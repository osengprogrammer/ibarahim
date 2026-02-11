package com.example.crashcourse.ui.edit

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.crashcourse.db.MasterClassWithNames
import com.example.crashcourse.ui.add.CaptureMode
import com.example.crashcourse.ui.add.FaceCaptureScreen
import com.example.crashcourse.ui.components.MasterClassDropdown
import com.example.crashcourse.ui.theme.AzuraPrimary
import com.example.crashcourse.utils.PhotoStorageUtils
import com.example.crashcourse.viewmodel.FaceViewModel
import com.example.crashcourse.viewmodel.MasterClassViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ✏️ Azura Tech Edit User Screen (FIXED)
 * Mengelola pembaruan profil biometrik, foto, dan perpindahan kelas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditUserScreen(
    studentId: String,
    onNavigateBack: () -> Unit,
    onUpdateSuccess: () -> Unit,
    faceViewModel: FaceViewModel = viewModel(),
    masterClassViewModel: MasterClassViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 1. Data Observation
    val allFacesState by faceViewModel.faceList.collectAsStateWithLifecycle()
    val masterClasses by masterClassViewModel.masterClassesWithNames.collectAsStateWithLifecycle(initialValue = emptyList())
    
    // 2. Cari User berdasarkan ID
    val user = remember(allFacesState, studentId) { 
        allFacesState.find { it.studentId == studentId } 
    }

    // 3. State Holders
    // Menggunakan 'derivedStateOf' atau inisialisasi awal agar data tidak hilang saat recompose
    var name by remember(user) { mutableStateOf(user?.name ?: "") }
    
    // Auto-select kelas saat ini dari daftar MasterClass
    var selectedMasterClass by remember(user, masterClasses) { 
        mutableStateOf(masterClasses.find { it.className == user?.className }) 
    }
    
    var embedding by remember { mutableStateOf<FloatArray?>(null) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var currentPhotoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var showFaceCapture by remember { mutableStateOf(false) }

    // 4. Load Foto Lama di Background
    LaunchedEffect(user?.photoUrl) {
        if (user?.photoUrl != null) {
            currentPhotoBitmap = withContext(Dispatchers.IO) { 
                PhotoStorageUtils.loadFacePhoto(user.photoUrl) 
            }
        }
    }

    // 5. Handle User Tidak Ditemukan (Misal baru dihapus)
    if (user == null) {
        ErrorStateScreen(
            title = "Siswa Tidak Ditemukan", 
            message = "Data ID '$studentId' tidak ditemukan di database lokal.", 
            onNavigateBack = onNavigateBack
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profil Siswa", fontWeight = FontWeight.Bold) },
                navigationIcon = { 
                    IconButton(onClick = onNavigateBack) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null) 
                    } 
                },
                actions = {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).padding(end = 16.dp),
                            strokeWidth = 2.dp,
                            color = AzuraPrimary
                        )
                    } else {
                        IconButton(onClick = {
                            if (name.isNotBlank()) {
                                isProcessing = true
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        // A. Simpan foto baru jika ada
                                        val finalPhotoPath = if (capturedBitmap != null) {
                                            PhotoStorageUtils.saveFacePhoto(context, capturedBitmap!!, studentId) 
                                        } else {
                                            user.photoUrl // Pakai foto lama
                                        }

                                        // B. Update ke ViewModel (Pastikan fungsi ini ada di FaceViewModel)
                                        faceViewModel.updateFaceWithPhoto(
                                            originalFace = user,
                                            newName = name.trim(),
                                            newClass = selectedMasterClass, // Kirim object MasterClass
                                            newPhotoPath = finalPhotoPath,
                                            newEmbedding = embedding,
                                            onSuccess = {
                                                isProcessing = false
                                                scope.launch(Dispatchers.Main) {
                                                    Toast.makeText(context, "Profil diperbarui!", Toast.LENGTH_SHORT).show()
                                                    onUpdateSuccess() 
                                                }
                                            }
                                        )
                                    } catch (e: Exception) {
                                        isProcessing = false
                                        e.printStackTrace()
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Nama tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(Icons.Default.Check, "Simpan", tint = AzuraPrimary)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // --- PHOTO SECTION ---
            Box(contentAlignment = Alignment.Center) {
                val img = capturedBitmap ?: currentPhotoBitmap
                if (img != null) {
                    Image(
                        bitmap = img.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(160.dp)
                            .clip(CircleShape)
                            .shadow(4.dp, CircleShape)
                    )
                } else {
                    Icon(
                        Icons.Default.AccountCircle, 
                        contentDescription = null, 
                        modifier = Modifier.size(160.dp), 
                        tint = Color.LightGray
                    )
                }
                
                SmallFloatingActionButton(
                    onClick = { showFaceCapture = true },
                    containerColor = AzuraPrimary,
                    contentColor = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-4).dp, y = (-4).dp)
                ) {
                    Icon(Icons.Default.PhotoCamera, null, modifier = Modifier.size(20.dp))
                }
            }

            // --- FORM INPUTS ---
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nama Lengkap") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Dropdown Pilihan Kelas
            MasterClassDropdown(
                options = masterClasses,
                selected = selectedMasterClass,
                onSelect = { selectedMasterClass = it },
                label = "Pilih Rombel / Kelas"
            )

            // Info Read-only
            OutlinedTextField(
                value = studentId,
                onValueChange = {},
                readOnly = true,
                enabled = false,
                label = { Text("ID Siswa (Terkunci)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = Color.Gray,
                    disabledBorderColor = Color.LightGray
                )
            )
            
            if (embedding != null) {
                Text("✅ Biometrik Wajah Diperbarui", color = Color.Green, style = MaterialTheme.typography.bodySmall)
            }
        }

        if (showFaceCapture) {
            FaceCaptureScreen(
                mode = CaptureMode.EMBEDDING, // Mengambil Foto + Embedding sekaligus
                onClose = { showFaceCapture = false },
                onEmbeddingCaptured = { 
                    embedding = it
                    showFaceCapture = false 
                },
                onPhotoCaptured = { capturedBitmap = it }
            )
        }
    }
}

@Composable
fun ErrorStateScreen(title: String, message: String, onNavigateBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.SearchOff, null, tint = Color.Gray, modifier = Modifier.size(80.dp))
            Spacer(Modifier.height(16.dp))
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(message, textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = Color.Gray)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onNavigateBack) { Text("KEMBALI") }
        }
    }
}