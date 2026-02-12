package com.example.crashcourse.ui.options

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.crashcourse.db.*
import com.example.crashcourse.ui.OptionsHelpers
import com.example.crashcourse.ui.components.AzuraInput
import com.example.crashcourse.viewmodel.MasterClassViewModel
import com.example.crashcourse.viewmodel.OptionsViewModel

// Definisikan warna Azura jika belum ada di Theme
val AzuraPrimary = Color(0xFF1E88E5)

/**
 * 🏛️ Azura Tech Master Class Management
 * Sistem perakitan identitas grup menggunakan 6 kategori master data.
 * Fitur: Flexible Assembly, Null Safety, Incremental Sync UI, & Local Search.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterClassManagementScreen(
    onNavigateBack: () -> Unit = {},
    masterVM: MasterClassViewModel = viewModel(),
    optionsVM: OptionsViewModel = viewModel()
) {
    val context = LocalContext.current
    
    // --- 📊 OBSERVASI DATA ---
    val masterClasses by masterVM.masterClassesWithNames.collectAsStateWithLifecycle(initialValue = emptyList())
    
    val classOptions by optionsVM.classOptions.collectAsStateWithLifecycle()
    val subClassOptions by optionsVM.subClassOptions.collectAsStateWithLifecycle()
    val gradeOptions by optionsVM.gradeOptions.collectAsStateWithLifecycle()
    val subGradeOptions by optionsVM.subGradeOptions.collectAsStateWithLifecycle()
    val programOptions by optionsVM.programOptions.collectAsStateWithLifecycle()
    val roleOptions by optionsVM.roleOptions.collectAsStateWithLifecycle()

    // --- 🔍 SEARCH & FILTER STATE ---
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    // 🧠 LOGIKA FILTER LOKAL (FIXED NULL SAFETY)
    val filteredMasterClasses = remember(masterClasses, searchQuery) {
        masterClasses.filter { 
            (it.className?.contains(searchQuery, ignoreCase = true) == true) || 
            (it.gradeName?.contains(searchQuery, ignoreCase = true) == true)
        }
    }

    

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Manajemen Unit", fontWeight = FontWeight.Bold)
                        Text("${masterClasses.size} Unit Terdaftar", style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        // Trigger manual sync jika diperlukan
                        Toast.makeText(context, "Sinkronisasi Cloud...", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.CloudSync, "Sync Cloud", tint = AzuraPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = AzuraPrimary
            ) {
                Icon(Icons.Default.Add, "Tambah Unit", tint = Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(16.dp)
        ) {
            // --- 🔍 1. SEARCH BAR ---
            AzuraInput(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = "Cari Unit (Contoh: XII IPA 1)",
                leadingIcon = Icons.Default.Search
            )
            
            Spacer(Modifier.height(16.dp))

            // --- 📜 2. DATA LIST ---
            if (filteredMasterClasses.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (searchQuery.isEmpty()) "Belum ada unit rakitan.\nKlik + untuk membuat unit baru." else "Unit tidak ditemukan.", 
                        color = Color.Gray, 
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredMasterClasses, key = { it.classId }) { item ->
                        MasterClassCard(
                            item = item, 
                            onDelete = { 
                                masterVM.deleteClass(item) 
                                Toast.makeText(context, "Unit berhasil dihapus", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }

            // --- 📥 3. EXPORT BUTTON ---
            if (filteredMasterClasses.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { 
                        Toast.makeText(context, "Mengekspor ${filteredMasterClasses.size} unit...", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D3436))
                ) {
                    Icon(Icons.Default.Download, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Download Daftar Unit (.csv)")
                }
            }
        }
    }

    // --- 🛠️ 4. ADD DIALOG ---
    if (showAddDialog) {
        AddMasterClassDialog(
            classOptions = classOptions,
            subClassOptions = subClassOptions,
            gradeOptions = gradeOptions,
            subGradeOptions = subGradeOptions,
            programOptions = programOptions,
            roleOptions = roleOptions,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, cId, scId, gId, sgId, pId, rId ->
                masterVM.addMasterClassFull(name, cId, scId, gId, sgId, pId, rId)
                showAddDialog = false
            }
        )
    }
}

/**
 * ➕ DIALOG PENDAFTARAN (FLEKSIBEL)
 * Tidak wajib mengisi semua pilar. Minimal 1 pilar untuk simpan.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMasterClassDialog(
    classOptions: List<ClassOption>,
    subClassOptions: List<SubClassOption>,
    gradeOptions: List<GradeOption>,
    subGradeOptions: List<SubGradeOption>,
    programOptions: List<ProgramOption>,
    roleOptions: List<RoleOption>,
    onDismiss: () -> Unit,
    onConfirm: (String, Int, Int, Int, Int, Int, Int) -> Unit
) {
    var selectedClass by remember { mutableStateOf<ClassOption?>(null) }
    var selectedSubClass by remember { mutableStateOf<SubClassOption?>(null) }
    var selectedGrade by remember { mutableStateOf<GradeOption?>(null) }
    var selectedSubGrade by remember { mutableStateOf<SubGradeOption?>(null) }
    var selectedProgram by remember { mutableStateOf<ProgramOption?>(null) }
    var selectedRole by remember { mutableStateOf<RoleOption?>(null) }
    
    // 🧠 SMART AUTO-NAMING: Hanya merakit pilar yang TIDAK NULL
    val autoClassName = remember(
        selectedClass, selectedSubClass, selectedGrade, 
        selectedSubGrade, selectedProgram, selectedRole
    ) {
        listOfNotNull(
            selectedGrade?.name,
            selectedClass?.name,
            selectedProgram?.name,
            selectedSubClass?.name,
            selectedSubGrade?.name,
            selectedRole?.name
        ).joinToString(" ").trim().replace("\\s+".toRegex(), " ")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Column {
                Text("Rakit Unit Baru", fontWeight = FontWeight.Bold)
                Text("Pilih pilar yang dibutuhkan saja", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp)
            ) {
                item { MasterOptionPicker("Tingkat (Grade)", gradeOptions, selectedGrade) { selectedGrade = it as GradeOption } }
                item { MasterOptionPicker("Departemen (Class)", classOptions, selectedClass) { selectedClass = it as ClassOption } }
                item { MasterOptionPicker("Jurusan (Program)", programOptions, selectedProgram) { selectedProgram = it as ProgramOption } }
                item { MasterOptionPicker("Sub-Unit (SubClass)", subClassOptions, selectedSubClass) { selectedSubClass = it as SubClassOption } }
                item { MasterOptionPicker("Periode (SubGrade)", subGradeOptions, selectedSubGrade) { selectedSubGrade = it as SubGradeOption } }
                item { MasterOptionPicker("Jabatan (Role)", roleOptions, selectedRole) { selectedRole = it as RoleOption } }

                item {
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        color = AzuraPrimary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Preview Nama Unit:", style = MaterialTheme.typography.labelSmall, color = AzuraPrimary)
                            Text(
                                text = autoClassName.ifEmpty { "(Pilih kategori)" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (autoClassName.isEmpty()) Color.Gray else Color.Black
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = autoClassName.isNotBlank(),
                onClick = {
                    onConfirm(
                        autoClassName,
                        selectedClass?.id ?: 0,
                        selectedSubClass?.id ?: 0,
                        selectedGrade?.id ?: 0,
                        selectedSubGrade?.id ?: 0,
                        selectedProgram?.id ?: 0,
                        selectedRole?.id ?: 0
                    )
                },
                shape = RoundedCornerShape(12.dp)
            ) { Text("Simpan Unit") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterOptionPicker(
    label: String,
    options: List<Any>,
    selected: Any?,
    onSelect: (Any) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = if (selected != null) OptionsHelpers.getName(selected) else ""

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded, 
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(OptionsHelpers.getName(option)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun MasterClassCard(item: MasterClassWithNames, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp), 
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = AzuraPrimary.copy(alpha = 0.1f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.DashboardCustomize, null, tint = AzuraPrimary)
                }
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.className ?: "Unit Tanpa Nama", 
                    fontWeight = FontWeight.Bold, 
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${item.gradeName ?: "-"} • ${item.classOptionName ?: "-"} • ${item.roleName ?: "-"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
            
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteSweep, null, tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}