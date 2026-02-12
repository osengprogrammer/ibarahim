package com.example.crashcourse.ui.add

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.crashcourse.db.MasterClassWithNames
import com.example.crashcourse.ui.components.*
import com.example.crashcourse.ui.theme.*
import com.example.crashcourse.utils.PhotoProcessingUtils
import com.example.crashcourse.utils.PhotoStorageUtils
import com.example.crashcourse.viewmodel.FaceViewModel
import com.example.crashcourse.viewmodel.MasterClassViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 🖼️ Azura Tech Gallery Upload Screen (FIXED)
 * Mendukung pendaftaran biometrik via galeri dengan multi-rombel/mata kuliah.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SingleUploadScreen(
    onNavigateBack: () -> Unit,
    onUpdateSuccess: () -> Unit, 
    viewModel: FaceViewModel = viewModel(),
    masterClassVM: MasterClassViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // --- 📊 MASTER DATA ---
    val masterClasses by masterClassVM.masterClassesWithNames.collectAsStateWithLifecycle(emptyList())
    
    // --- 🚀 MULTI-SELECT STATE ---
    val selectedRombels = remember { mutableStateListOf<MasterClassWithNames>() }
    var showRombelDialog by remember { mutableStateOf(false) }

    // --- 📝 FORM STATES ---
    var name by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var embedding by remember { mutableStateOf<FloatArray?>(null) }
    
    var isProcessing by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // 🖼️ GALLERY LAUNCHER
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            isProcessing = true
            scope.launch(Dispatchers.IO) {
                try {
                    val bmp = if (Build.VERSION.SDK_INT < 28) {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                    } else {
                        val source = ImageDecoder.createSource(context.contentResolver, it)
                        ImageDecoder.decodeBitmap(source).copy(Bitmap.Config.ARGB_8888, true)
                    }

                    val result = PhotoProcessingUtils.processBitmapForFaceEmbedding(context, bmp)
                    
                    withContext(Dispatchers.Main) {
                        if (result != null) {
                            capturedBitmap = result.first
                            embedding = result.second
                            snackbarHostState.showSnackbar("Wajah terdeteksi & biometrik berhasil diekstrak!")
                        } else {
                            snackbarHostState.showSnackbar("Wajah tidak ditemukan! Gunakan foto yang lebih jelas.")
                        }
                        isProcessing = false
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        isProcessing = false
                        snackbarHostState.showSnackbar("Gagal memproses foto: ${e.message}")
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = AzuraBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Gallery Registration", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AzuraText)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AzuraTitle("Registrasi Gallery")
            Text(
                "Daftarkan personel baru menggunakan foto dari penyimpanan perangkat.",
                style = MaterialTheme.typography.bodyMedium,
                color = AzuraText.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
            
            Spacer(Modifier.height(32.dp))

            // --- 📸 GALLERY SELECTION BOX ---
            Box(
                Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White)
                    .clickable { if (!isProcessing) galleryLauncher.launch("image/*") }
                    .border(
                        width = 2.dp, 
                        color = if (capturedBitmap != null) AzuraSuccess else AzuraPrimary.copy(alpha = 0.1f), 
                        shape = RoundedCornerShape(32.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (capturedBitmap != null) {
                    Image(
                        bitmap = capturedBitmap!!.asImageBitmap(), 
                        contentDescription = null, 
                        modifier = Modifier.fillMaxSize(), 
                        contentScale = ContentScale.Crop
                    )
                } else {
                    if (isProcessing) {
                        CircularProgressIndicator(color = AzuraPrimary)
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddPhotoAlternate, null, Modifier.size(48.dp), tint = AzuraPrimary)
                            Text("Pilih Foto", color = AzuraPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // --- 🖋️ INPUT FORM ---
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AzuraInput(value = name, onValueChange = { name = it }, label = "Nama Lengkap")
                AzuraInput(value = studentId, onValueChange = { studentId = it }, label = "Nomor Induk / ID")

                // 🚀 MULTI-SELECT ROMBEL SECTION
                Text("Daftar Mata Kuliah / Rombel", style = MaterialTheme.typography.labelMedium, color = AzuraText.copy(alpha = 0.6f))
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = { showRombelDialog = true },
                        label = { Text("Tambah Matkul") },
                        leadingIcon = { Icon(Icons.Default.Add, null, Modifier.size(18.dp)) }
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
            }

            Spacer(Modifier.height(40.dp))

            // --- ✅ SAVE ACTION ---
            AzuraButton(
                text = "Simpan Biometrik",
                isLoading = isProcessing,
                onClick = {
                    val finalName = name.trim()
                    val finalId = studentId.trim()
                    
                    if (selectedRombels.isEmpty() || embedding == null || finalName.isBlank() || finalId.isBlank()) {
                        scope.launch { snackbarHostState.showSnackbar("Lengkapi semua data, matkul, dan foto!") }
                        return@AzuraButton
                    }

                    isProcessing = true
                    scope.launch(Dispatchers.IO) {
                        try {
                            val path = PhotoStorageUtils.saveFacePhoto(context, capturedBitmap!!, finalId)
                            
                            if (path != null) {
                                // 🚀 FIX: Menggunakan registerFaceWithMultiUnit
                                viewModel.registerFaceWithMultiUnit(
                                    studentId = finalId,
                                    name = finalName,
                                    embedding = embedding!!,
                                    photoUrl = path,
                                    units = selectedRombels.toList(), // Menerima List
                                    onSuccess = {
                                        scope.launch(Dispatchers.Main) {
                                            isProcessing = false
                                            showSuccessDialog = true
                                        }
                                    },
                                    onDuplicate = { dupId: String -> // Tipe eksplisit ditambahkan
                                        scope.launch(Dispatchers.Main) {
                                            isProcessing = false
                                            snackbarHostState.showSnackbar("ID $dupId sudah digunakan!")
                                        }
                                    }
                                )
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                isProcessing = false
                                snackbarHostState.showSnackbar("Error: ${e.message}")
                            }
                        }
                    }
                }
            )
        }

        // --- 🚀 ROMBEL SELECTION DIALOG ---
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

        // --- 🎉 SUCCESS OVERLAY ---
        if (showSuccessDialog) {
            AlertDialog(
                onDismissRequest = { },
                icon = { Icon(Icons.Default.CheckCircle, null, tint = AzuraSuccess, modifier = Modifier.size(64.dp)) },
                title = { Text("Registrasi Berhasil", fontWeight = FontWeight.Bold) },
                text = { Text("Personel $name telah didaftarkan pada ${selectedRombels.size} mata kuliah.") },
                confirmButton = { 
                    AzuraButton(
                        text = "Selesai", 
                        onClick = { 
                            showSuccessDialog = false
                            onUpdateSuccess()
                        }
                    ) 
                },
                shape = RoundedCornerShape(28.dp),
                containerColor = Color.White
            )
        }
    }
}