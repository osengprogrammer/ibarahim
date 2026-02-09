package com.example.crashcourse.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.crashcourse.db.*
import com.example.crashcourse.ui.components.FaceAvatar
import com.example.crashcourse.ui.components.StudentFormBody
import com.example.crashcourse.ui.theme.AzuraPrimary
import com.example.crashcourse.viewmodel.AuthState
import com.example.crashcourse.viewmodel.FaceViewModel
import com.example.crashcourse.viewmodel.OptionsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceListScreen(
    authState: AuthState.Active,
    faceViewModel: FaceViewModel = viewModel(),
    optionsViewModel: OptionsViewModel = viewModel(),
    onEditUser: (FaceEntity) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Master Data
    val classOptions by optionsViewModel.classOptions.collectAsStateWithLifecycle(emptyList())
    val subClassOptions by optionsViewModel.subClassOptions.collectAsStateWithLifecycle(emptyList())
    val gradeOptions by optionsViewModel.gradeOptions.collectAsStateWithLifecycle(emptyList())
    val subGradeOptions by optionsViewModel.subGradeOptions.collectAsStateWithLifecycle(emptyList())
    val programOptions by optionsViewModel.programOptions.collectAsStateWithLifecycle(emptyList())
    val roleOptions by optionsViewModel.roleOptions.collectAsStateWithLifecycle(emptyList())
    
    val allFaces by faceViewModel.getScopedFaceList(authState).collectAsStateWithLifecycle(emptyList())
    var searchQuery by remember { mutableStateOf("") }
    val isAdmin = authState.role == "ADMIN"

    val filteredFaces by remember(allFaces, searchQuery) {
        derivedStateOf { 
            allFaces.filter { it.name.contains(searchQuery, ignoreCase = true) } 
        }
    }

    // --- STATE DIALOG ---
    var editingFace by remember { mutableStateOf<FaceEntity?>(null) }
    var faceToDelete by remember { mutableStateOf<FaceEntity?>(null) }
    
    // State Form Edit
    var editName by remember { mutableStateOf("") }
    var selectedClass by remember { mutableStateOf<ClassOption?>(null) }
    var selectedSubClass by remember { mutableStateOf<SubClassOption?>(null) }
    var selectedGrade by remember { mutableStateOf<GradeOption?>(null) }
    var selectedSubGrade by remember { mutableStateOf<SubGradeOption?>(null) }
    var selectedProgram by remember { mutableStateOf<ProgramOption?>(null) }
    var selectedRole by remember { mutableStateOf<RoleOption?>(null) }

    // Init data saat dialog dibuka
    LaunchedEffect(editingFace) {
        editingFace?.let { face ->
            editName = face.name
            selectedClass = classOptions.find { it.id == face.classId || it.name == face.className }
            selectedSubClass = subClassOptions.find { it.id == face.subClassId || it.name == face.subClass }
            selectedGrade = gradeOptions.find { it.id == face.gradeId || it.name == face.grade }
            selectedSubGrade = subGradeOptions.find { it.id == face.subGradeId || it.name == face.subGrade }
            selectedProgram = programOptions.find { it.id == face.programId || it.name == face.program }
            selectedRole = roleOptions.find { it.id == face.roleId || it.name == face.role }
        }
    }

    // ==========================================
    // 1. DIALOG KONFIRMASI DELETE
    // ==========================================
    if (isAdmin && faceToDelete != null) {
        val face = faceToDelete!!
        AlertDialog(
            onDismissRequest = { faceToDelete = null },
            title = { Text("Hapus Data Wajah?") },
            text = { 
                Text("Apakah Anda yakin ingin menghapus data ${face.name}? Tindakan ini akan menghapus data di Lokal dan Cloud secara permanen.") 
            },
            confirmButton = {
                Button(
                    onClick = {
                        faceViewModel.deleteFace(face)
                        faceToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Hapus") }
            },
            dismissButton = {
                TextButton(onClick = { faceToDelete = null }) { Text("Batal") }
            }
        )
    }

    // ==========================================
    // 2. DIALOG UI (Quick Edit Profil)
    // ==========================================
    if (isAdmin && editingFace != null) {
        val face = editingFace!!
        AlertDialog(
            onDismissRequest = { editingFace = null },
            title = { Text("Update Profil Murid", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    StudentFormBody(
                        name = editName, onNameChange = { editName = it },
                        selectedClass = selectedClass, onClassChange = { selectedClass = it },
                        selectedSubClass = selectedSubClass, onSubClassChange = { selectedSubClass = it },
                        selectedGrade = selectedGrade, onGradeChange = { selectedGrade = it },
                        selectedSubGrade = selectedSubGrade, onSubGradeChange = { selectedSubGrade = it },
                        selectedProgram = selectedProgram, onProgramChange = { selectedProgram = it },
                        selectedRole = selectedRole, onRoleChange = { selectedRole = it },
                        classOptions = classOptions, subClassOptions = subClassOptions,
                        gradeOptions = gradeOptions, subGradeOptions = subGradeOptions,
                        programOptions = programOptions, roleOptions = roleOptions
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    faceViewModel.updateFace(
                        face.copy(
                            name = editName,
                            className = selectedClass?.name ?: face.className,
                            classId = selectedClass?.id ?: face.classId,
                            subClass = selectedSubClass?.name ?: face.subClass,
                            subClassId = selectedSubClass?.id ?: face.subClassId,
                            grade = selectedGrade?.name ?: face.grade,
                            gradeId = selectedGrade?.id ?: face.gradeId,
                            subGrade = selectedSubGrade?.name ?: face.subGrade,
                            subGradeId = selectedSubGrade?.id ?: face.subGradeId,
                            program = selectedProgram?.name ?: face.program,
                            programId = selectedProgram?.id ?: face.programId,
                            role = selectedRole?.name ?: face.role,
                            roleId = selectedRole?.id ?: face.roleId
                        )
                    ) { editingFace = null }
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { editingFace = null }) { Text("Batal") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Daftar Murid", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (isAdmin) "Akses: Admin" else "Guru: ${authState.email}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        faceViewModel.syncStudentsWithCloud(authState) 
                    }) {
                        Icon(Icons.Default.Refresh, "Sync Cloud")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Cari nama...") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = MaterialTheme.shapes.medium
            )
            
            Spacer(Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                if (filteredFaces.isEmpty()) {
                    item {
                        Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Tidak ada data murid", color = Color.Gray)
                        }
                    }
                }

                // 🚀 PERBAIKAN DI SINI: Ganti it.id menjadi it.studentId
                items(items = filteredFaces, key = { it.studentId }) { face ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FaceAvatar(photoPath = face.photoUrl, size = 56)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = face.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${face.className} • ${face.grade}", 
                                    style = MaterialTheme.typography.bodySmall, 
                                    color = Color.Gray
                                )
                            }
                            if (isAdmin) {
                                IconButton(onClick = { editingFace = face }) {
                                    Icon(Icons.Default.Edit, "Edit", tint = AzuraPrimary, modifier = Modifier.size(20.dp))
                                }
                                IconButton(onClick = { onEditUser(face) }) {
                                    Icon(Icons.Default.CameraAlt, "Foto", tint = Color.Gray, modifier = Modifier.size(20.dp))
                                }
                                IconButton(onClick = { faceToDelete = face }) {
                                    Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}