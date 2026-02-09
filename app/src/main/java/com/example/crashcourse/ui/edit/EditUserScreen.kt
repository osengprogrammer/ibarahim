package com.example.crashcourse.ui.edit

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.* import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.crashcourse.db.FaceEntity
import com.example.crashcourse.ui.components.StudentFormBody // 🚀 PENTING: Import Komponen Reusable
import com.example.crashcourse.ui.theme.AzuraPrimary
import com.example.crashcourse.utils.PhotoStorageUtils
import com.example.crashcourse.utils.showToast
import com.example.crashcourse.viewmodel.FaceViewModel
import com.example.crashcourse.viewmodel.OptionsViewModel
import com.example.crashcourse.ui.add.CaptureMode
import com.example.crashcourse.ui.add.FaceCaptureScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditUserScreen(
    studentId: String,
    onNavigateBack: () -> Unit = {},
    onUserUpdated: () -> Unit = {},
    faceViewModel: FaceViewModel = viewModel(),
    optionsViewModel: OptionsViewModel = viewModel()
) {
    val context = LocalContext.current
    
    // 1. Ambil list wajah dari ViewModel
    val allFacesState: List<FaceEntity> by faceViewModel.faceList.collectAsStateWithLifecycle(initialValue = emptyList())
    
    // 2. Cari user secara spesifik
    val user: FaceEntity? = remember(allFacesState, studentId) { 
        allFacesState.find { it.studentId == studentId } 
    }

    // Master Data Options
    val classOptions by optionsViewModel.classOptions.collectAsStateWithLifecycle(emptyList())
    val subClassOptions by optionsViewModel.subClassOptions.collectAsStateWithLifecycle(emptyList())
    val gradeOptions by optionsViewModel.gradeOptions.collectAsStateWithLifecycle(emptyList())
    val subGradeOptions by optionsViewModel.subGradeOptions.collectAsStateWithLifecycle(emptyList())
    val programOptions by optionsViewModel.programOptions.collectAsStateWithLifecycle(emptyList())
    val roleOptions by optionsViewModel.roleOptions.collectAsStateWithLifecycle(emptyList())

    // Early exit jika user tidak ditemukan
    if (user == null) {
        ErrorStateScreen(
            title = "Siswa Tidak Ditemukan",
            message = "Data dengan ID '$studentId' tidak ada di database.",
            onNavigateBack = onNavigateBack
        )
        return
    }

    // --- FORM STATES (Inisialisasi dari data User) ---
    var name by remember(user) { mutableStateOf(user.name) }
    
    // State Options (Menggunakan Object agar ID tersimpan)
    var selectedClass by remember(user, classOptions) { 
        mutableStateOf(classOptions.find { it.id == user.classId || it.name == user.className }) 
    }
    var selectedSubClass by remember(user, subClassOptions) { 
        mutableStateOf(subClassOptions.find { it.id == user.subClassId || it.name == user.subClass }) 
    }
    var selectedGrade by remember(user, gradeOptions) { 
        mutableStateOf(gradeOptions.find { it.id == user.gradeId || it.name == user.grade }) 
    }
    var selectedSubGrade by remember(user, subGradeOptions) { 
        mutableStateOf(subGradeOptions.find { it.id == user.subGradeId || it.name == user.subGrade }) 
    }
    var selectedProgram by remember(user, programOptions) {
        mutableStateOf(programOptions.find { it.id == user.programId || it.name == user.program })
    }
    var selectedRole by remember(user, roleOptions) { 
        mutableStateOf(roleOptions.find { it.id == user.roleId || it.name == user.role }) 
    }

    var embedding by remember { mutableStateOf<FloatArray?>(null) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var currentPhotoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var showFaceCapture by remember { mutableStateOf(false) }

    // Load foto lama jika ada
    LaunchedEffect(user.photoUrl) {
        user.photoUrl?.let { path ->
            currentPhotoBitmap = withContext(Dispatchers.IO) { PhotoStorageUtils.loadFacePhoto(path) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profil Siswa") },
                navigationIcon = { 
                    IconButton(onClick = onNavigateBack) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null) 
                    } 
                },
                actions = {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = 16.dp))
                    } else {
                        TextButton(onClick = {
                            if (name.isNotBlank()) {
                                isProcessing = true
                                val updatedUser = user.copy(
                                    name = name.trim(),
                                    className = selectedClass?.name ?: user.className,
                                    classId = selectedClass?.id ?: user.classId,
                                    subClass = selectedSubClass?.name ?: user.subClass,
                                    subClassId = selectedSubClass?.id ?: user.subClassId,
                                    grade = selectedGrade?.name ?: user.grade,
                                    gradeId = selectedGrade?.id ?: user.gradeId,
                                    subGrade = selectedSubGrade?.name ?: user.subGrade,
                                    subGradeId = selectedSubGrade?.id ?: user.subGradeId,
                                    program = selectedProgram?.name ?: user.program,
                                    programId = selectedProgram?.id ?: user.programId,
                                    role = selectedRole?.name ?: user.role,
                                    roleId = selectedRole?.id ?: user.roleId
                                )

                                faceViewModel.updateFaceWithPhoto(
                                    face = updatedUser,
                                    photoBitmap = capturedBitmap,
                                    embedding = embedding ?: user.embedding,
                                    onComplete = {
                                        isProcessing = false
                                        context.showToast("Profil diperbarui!")
                                        onUserUpdated()
                                        onNavigateBack()
                                    },
                                    onError = { msg ->
                                        isProcessing = false
                                        context.showToast(msg)
                                    }
                                )
                            }
                        }) {
                            Text("Simpan", fontWeight = FontWeight.Bold, color = AzuraPrimary)
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- BAGIAN FOTO (Spesifik Screen Ini) ---
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val img = capturedBitmap ?: currentPhotoBitmap
                if (img != null) {
                    Image(
                        bitmap = img.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(120.dp).clip(CircleShape)
                    )
                } else {
                    Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(120.dp), tint = Color.LightGray)
                }
                
                IconButton(
                    onClick = { showFaceCapture = true },
                    modifier = Modifier.align(Alignment.BottomCenter).offset(y = 12.dp)
                ) {
                    Surface(shape = CircleShape, color = AzuraPrimary, shadowElevation = 4.dp) {
                        Icon(Icons.Default.PhotoCamera, null, tint = Color.White, modifier = Modifier.padding(8.dp))
                    }
                }
            }

            HorizontalDivider()
            
            // --- BAGIAN FORM (Reusable Component) ---
            // 🚀 Menggantikan puluhan baris kode manual sebelumnya
            StudentFormBody(
                name = name, onNameChange = { name = it },
                selectedClass = selectedClass, onClassChange = { selectedClass = it },
                selectedSubClass = selectedSubClass, onSubClassChange = { selectedSubClass = it },
                selectedGrade = selectedGrade, onGradeChange = { selectedGrade = it },
                selectedSubGrade = selectedSubGrade, onSubGradeChange = { selectedSubGrade = it },
                selectedProgram = selectedProgram, onProgramChange = { selectedProgram = it },
                selectedRole = selectedRole, onRoleChange = { selectedRole = it },
                classOptions = classOptions,
                subClassOptions = subClassOptions,
                gradeOptions = gradeOptions,
                subGradeOptions = subGradeOptions,
                programOptions = programOptions,
                roleOptions = roleOptions
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Overlay Capture Wajah
        if (showFaceCapture) {
            FaceCaptureScreen(
                mode = CaptureMode.EMBEDDING,
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
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.ErrorOutline, null, tint = Color.Gray, modifier = Modifier.size(64.dp))
            Text(title, fontWeight = FontWeight.Bold)
            Text(message, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
            Button(onClick = onNavigateBack) { Text("Kembali") }
        }
    }
}