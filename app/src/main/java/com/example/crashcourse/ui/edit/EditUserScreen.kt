package com.example.crashcourse.ui.edit

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.crashcourse.db.MasterClassWithNames
import com.example.crashcourse.ui.add.CaptureMode
import com.example.crashcourse.ui.add.FaceCaptureScreen
import com.example.crashcourse.ui.components.RombelSelectionDialog
import com.example.crashcourse.ui.theme.AzuraPrimary
import com.example.crashcourse.utils.PhotoStorageUtils
import com.example.crashcourse.viewmodel.FaceViewModel
import com.example.crashcourse.viewmodel.MasterClassViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    
    val allFacesState by faceViewModel.faceList.collectAsStateWithLifecycle()
    val masterClasses by masterClassViewModel.masterClassesWithNames.collectAsStateWithLifecycle(initialValue = emptyList())
    
    val user = remember(allFacesState, studentId) { 
        allFacesState.find { it.studentId == studentId } 
    }

    var name by remember(user) { mutableStateOf(user?.name ?: "") }
    val selectedRombels = remember { mutableStateListOf<MasterClassWithNames>() }
    var showRombelDialog by remember { mutableStateOf(false) }

    // Memastikan data kelas lama ter-load ke Chips
    LaunchedEffect(user, masterClasses) {
        if (user != null && masterClasses.isNotEmpty() && selectedRombels.isEmpty()) {
            val currentNames = user.className.split(",").map { it.trim() }
            val matched = masterClasses.filter { it.className in currentNames }
            selectedRombels.addAll(matched)
        }
    }
    
    var embedding by remember { mutableStateOf<FloatArray?>(null) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var currentPhotoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var showFaceCapture by remember { mutableStateOf(false) }

    LaunchedEffect(user?.photoUrl) {
        if (user?.photoUrl != null) {
            currentPhotoBitmap = withContext(Dispatchers.IO) { 
                PhotoStorageUtils.loadFacePhoto(user.photoUrl) 
            }
        }
    }

    if (user == null) {
        ErrorStateScreen("User Tidak Ditemukan", "Data dengan ID $studentId tidak tersedia.", onNavigateBack)
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profil", fontWeight = FontWeight.Bold) },
                navigationIcon = { 
                    IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } 
                },
                actions = {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = 16.dp), color = AzuraPrimary)
                    } else {
                        IconButton(onClick = {
                            if (name.isNotBlank() && selectedRombels.isNotEmpty()) {
                                isProcessing = true
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val finalPhotoPath = if (capturedBitmap != null) {
                                            PhotoStorageUtils.saveFacePhoto(context, capturedBitmap!!, studentId) 
                                        } else user.photoUrl

                                        // 🔥 PERBAIKAN: Gunakan fungsi baru 'updateFaceWithMultiUnit'
                                        faceViewModel.updateFaceWithMultiUnit(
                                            originalFace = user,
                                            newName = name.trim(),
                                            newUnits = selectedRombels.toList(), // Kirim sebagai List
                                            newPhotoPath = finalPhotoPath,
                                            newEmbedding = embedding,
                                            onSuccess = {
                                                isProcessing = false
                                                scope.launch(Dispatchers.Main) {
                                                    Toast.makeText(context, "Data diperbarui!", Toast.LENGTH_SHORT).show()
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
                                Toast.makeText(context, "Lengkapi Nama & Matkul!", Toast.LENGTH_SHORT).show()
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
                        modifier = Modifier.size(160.dp).clip(CircleShape).shadow(4.dp, CircleShape)
                    )
                } else {
                    Icon(Icons.Default.AccountCircle, null, Modifier.size(160.dp), tint = Color.LightGray)
                }
                SmallFloatingActionButton(
                    onClick = { showFaceCapture = true },
                    containerColor = AzuraPrimary,
                    modifier = Modifier.align(Alignment.BottomEnd).offset((-4).dp, (-4).dp)
                ) { Icon(Icons.Default.PhotoCamera, null, Modifier.size(20.dp)) }
            }

            // --- INPUT NAMA ---
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Nama Lengkap") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // --- MULTI-SELECT CHIPS ---
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Mata Kuliah / Rombel Terdaftar", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = { showRombelDialog = true },
                        label = { Text("Tambah") },
                        leadingIcon = { Icon(Icons.Default.Add, null, Modifier.size(18.dp)) }
                    )
                    selectedRombels.forEach { rombel ->
                        InputChip(
                            selected = true,
                            onClick = {},
                            label = { Text(rombel.className) },
                            trailingIcon = {
                                Icon(Icons.Default.Close, null, Modifier.size(16.dp).clickable { selectedRombels.remove(rombel) })
                            }
                        )
                    }
                }
            }

            // --- ID ---
            OutlinedTextField(
                value = studentId, onValueChange = {}, readOnly = true, enabled = false,
                label = { Text("ID User") }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        if (showRombelDialog) {
            RombelSelectionDialog(
                allClasses = masterClasses,
                alreadySelected = selectedRombels,
                onDismiss = { showRombelDialog = false },
                onSave = { 
                    selectedRombels.clear()
                    selectedRombels.addAll(it)
                    showRombelDialog = false
                }
            )
        }

        if (showFaceCapture) {
            FaceCaptureScreen(
                mode = CaptureMode.EMBEDDING,
                onClose = { showFaceCapture = false },
                onEmbeddingCaptured = { embedding = it; showFaceCapture = false },
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