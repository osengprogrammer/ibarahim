package com.example.crashcourse.ui

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.crashcourse.ui.components.FaceAvatar
import com.example.crashcourse.db.FaceEntity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crashcourse.viewmodel.FaceViewModel
import com.example.crashcourse.viewmodel.AuthState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceListScreen(
    authState: AuthState.Active,
    viewModel: FaceViewModel = viewModel(),
    onEditUser: (FaceEntity) -> Unit = {} // Ini navigasi ke layar ambil foto
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // --- DATA MASTER UNTUK DROPDOWN ---
    val classOptions by viewModel.classOptions.collectAsStateWithLifecycle()
    val gradeOptions by viewModel.gradeOptions.collectAsStateWithLifecycle()
    val programOptions by viewModel.programOptions.collectAsStateWithLifecycle()
    
    // --- DATA MURID (TEACHER SCOPE) ---
    val scopedFlow = remember(authState.uid) { viewModel.getScopedFaceList(authState) }
    val allFaces by scopedFlow.collectAsStateWithLifecycle()
    
    var searchQuery by remember { mutableStateOf("") }
    val isAdmin = authState.role == "ADMIN"

    val filteredFaces by remember {
        derivedStateOf { allFaces.filter { it.name.contains(searchQuery, ignoreCase = true) } }
    }

    // --- DIALOG EDIT DETAIL STATE ---
    var editingFace by remember { mutableStateOf<FaceEntity?>(null) }
    var editName by remember { mutableStateOf("") }
    var editClass by remember { mutableStateOf("") }
    var editGrade by remember { mutableStateOf("") }
    var editProgram by remember { mutableStateOf("") }

    if (isAdmin) {
        editingFace?.let { face ->
            AlertDialog(
                onDismissRequest = { editingFace = null },
                title = { Text("Update Profil Murid") },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = editName, 
                            onValueChange = { editName = it }, 
                            label = { Text("Nama Lengkap") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Dropdown Pilihan Kelas dari Master Data
                        MasterDataDropdown(
                            label = "Kelas",
                            options = classOptions.map { it.name },
                            selectedOption = editClass,
                            onOptionSelected = { editClass = it }
                        )

                        MasterDataDropdown(
                            label = "Grade",
                            options = gradeOptions.map { it.name },
                            selectedOption = editGrade,
                            onOptionSelected = { editGrade = it }
                        )

                        MasterDataDropdown(
                            label = "Program",
                            options = programOptions.map { it.name },
                            selectedOption = editProgram,
                            onOptionSelected = { editProgram = it }
                        )
                        
                        Text(
                            "ID Murid: ${face.studentId}", 
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.updateFace(
                            face.copy(
                                name = editName,
                                className = editClass,
                                grade = editGrade,
                                program = editProgram
                            )
                        ) { editingFace = null }
                    }) { Text("Simpan") }
                },
                dismissButton = {
                    TextButton(onClick = { editingFace = null }) { Text("Batal") }
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Manajemen Murid")
                        Text(
                            text = if (isAdmin) "Full Access" else "Scope: ${authState.assignedClasses.joinToString()}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { scope.launch { com.example.crashcourse.db.FaceCache.refresh(context) } }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Cari nama murid...") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, null) }
                    }
                }
            )
            
            Spacer(Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items = filteredFaces, key = { it.studentId }) { face ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (isAdmin) {
                                editingFace = face
                                editName = face.name
                                editClass = face.className
                                editGrade = face.grade
                                editProgram = face.program
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FaceAvatar(photoPath = face.photoUrl, size = 64)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = face.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "${face.className} • ${face.grade} • ${face.program}", 
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (!isAdmin) Icon(Icons.Default.Lock, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                        }
                        
                        if (isAdmin) {
                            Row(
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // TOMBOL 1: Edit Data (Dialog)
                                TextButton(onClick = {
                                    editingFace = face
                                    editName = face.name
                                    editClass = face.className
                                    editGrade = face.grade
                                    editProgram = face.program
                                }) {
                                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Data")
                                }

                                // TOMBOL 2: Edit Foto (Navigasi ke Kamera + Embedding)
                                TextButton(onClick = { onEditUser(face) }) {
                                    Icon(Icons.Default.PhotoCamera, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Foto")
                                }

                                Spacer(Modifier.weight(1f))

                                // TOMBOL 3: Hapus
                                IconButton(onClick = { viewModel.deleteFace(face) }) {
                                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterDataDropdown(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { selectionOption ->
                DropdownMenuItem(
                    text = { Text(selectionOption) },
                    onClick = {
                        onOptionSelected(selectionOption)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}