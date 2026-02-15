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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.crashcourse.db.FaceEntity
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

/**
 * 🛠️ EditUserScreen (V.10.8 - Build Success Version)
 * Mengelola pembaruan identitas, foto, dan rombel (Many-to-Many).
 */
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
    
    // 🔥 DATA OBSERVATION (Reactive dari Database)
    val allFacesState by faceViewModel.filteredFaces.collectAsStateWithLifecycle()
    val masterClasses by masterClassViewModel.masterClassesWithNames.collectAsStateWithLifecycle(initialValue = emptyList())
    
    // Cari user berdasarkan ID dari list global
    val user: FaceEntity? = remember(allFacesState, studentId) { 
        allFacesState.find { it.studentId == studentId } 
    }

    // --- FORM STATES ---
    var name by remember(user) { mutableStateOf(user?.name ?: "") }
    val selectedRombels = remember { mutableStateListOf<MasterClassWithNames>() }
    var showRombelDialog by remember { mutableStateOf(false) }

    // 🔥 FIX: Mapping Rombel Terdaftar (List<String> -> List<Object>)
    LaunchedEffect(user, masterClasses) {
        if (user != null && masterClasses.isNotEmpty() && selectedRombels.isEmpty()) {
            // Karena user.enrolledClasses sudah List<String>, kita langsung filter
            val matched = masterClasses.filter { it.className in user.enrolledClasses }
            selectedRombels.addAll(matched)
        }
    }
    
    var embedding by remember { mutableStateOf<FloatArray?>(null) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var currentPhotoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var showFaceCapture by remember { mutableStateOf(false) }

    // Load foto profil lama dari storage internal
    LaunchedEffect(user?.photoUrl) {
        val url = user?.photoUrl
        if (!url.isNullOrBlank()) {
            currentPhotoBitmap = withContext(Dispatchers.IO) { 
                PhotoStorageUtils.loadFacePhoto(url) 
            }
        }
    }

    // Jika data tidak ditemukan (misal dihapus oleh admin lain)
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
                                scope.launch {
                                    try {
                                        // Simpan foto baru jika ada, jika tidak pakai yang lama
                                        val finalPhotoPath = if (capturedBitmap != null) {
                                            withContext(Dispatchers.IO) {
                                                PhotoStorageUtils.saveFacePhoto(context, capturedBitmap!!, studentId) 
                                            }
                                        } else user.photoUrl

                                        // 🔥 UPDATE KE VIEWMODEL (MULTI-UNIT)
                                        faceViewModel.updateFaceWithMultiUnit(
                                            originalFace = user,
                                            newName = name.trim(),
                                            newUnits = selectedRombels.toList(),
                                            newPhotoPath = finalPhotoPath,
                                            newEmbedding = embedding,
                                            onSuccess = {
                                                isProcessing = false
                                                onUpdateSuccess() 
                                            }
                                        )
                                    } catch (e: Exception) {
                                        isProcessing = false
                                        Toast.makeText(context, "Gagal: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Lengkapi Nama & Rombel!", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(Icons.Default.Check, "Simpan", tint = AzuraPrimary, modifier = Modifier.size(28.dp))
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
            // --- SECTION: FOTO PROFIL ---
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
                ) { Icon(Icons.Default.PhotoCamera, null, Modifier.size(20.dp), tint = Color.White) }
            }

            // --- SECTION: IDENTITAS ---
            OutlinedTextField(
                value = name, 
                onValueChange = { name = it },
                label = { Text("Nama Lengkap") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Person, null) }
            )

            // --- SECTION: MULTI-UNIT SELECTION ---
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Unit / Mata Kuliah Terdaftar", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = { showRombelDialog = true },
                        label = { Text("Ubah Unit") },
                        leadingIcon = { Icon(Icons.Default.Edit, null, Modifier.size(18.dp)) }
                    )
                    selectedRombels.forEach { rombel ->
                        InputChip(
                            selected = true,
                            onClick = {},
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

            // --- SECTION: INFO SISTEM (READ-ONLY) ---
            OutlinedTextField(
                value = studentId, 
                onValueChange = {}, 
                readOnly = true, 
                enabled = false,
                label = { Text("ID Siswa (Student ID)") }, 
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Fingerprint, null) }
            )
            
            Text(
                "Biometrik wajah sudah tersimpan secara offline. " +
                "Lakukan scan ulang jika ingin memperbarui data wajah.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // --- DIALOGS ---

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

/**
 * 🛠️ Layar Feedback jika user tidak ditemukan
 */
@Composable
fun ErrorStateScreen(title: String, message: String, onNavigateBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.SearchOff, null, tint = Color.Gray, modifier = Modifier.size(80.dp))
            Spacer(Modifier.height(16.dp))
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(message, textAlign = TextAlign.Center, color = Color.Gray)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onNavigateBack) { Text("KEMBALI KE LIST") }
        }
    }
}