package com.example.crashcourse.ui.options

import android.widget.Toast
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
import com.example.crashcourse.viewmodel.MasterClassViewModel
import com.example.crashcourse.viewmodel.OptionsViewModel
import com.example.crashcourse.db.MasterClassWithNames // 🚀 FIX: Import dari package DB
import com.example.crashcourse.db.MasterClassRoom // 🚀 FIX: Import Entity Room

/**
 * 🏛️ Azura Tech Master Class Management
 * Sistem perakitan identitas grup menggunakan 6 kategori master data.
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
    // 🚀 FIX: Berikan tipe list eksplisit agar compiler tidak "Cannot infer type"
    val masterClasses: List<MasterClassWithNames> by masterVM.masterClassesWithNames.collectAsStateWithLifecycle(initialValue = emptyList())
    
    val classOptions by optionsVM.classOptions.collectAsStateWithLifecycle()
    val subClassOptions by optionsVM.subClassOptions.collectAsStateWithLifecycle()
    val gradeOptions by optionsVM.gradeOptions.collectAsStateWithLifecycle()
    val subGradeOptions by optionsVM.subGradeOptions.collectAsStateWithLifecycle()
    val programOptions by optionsVM.programOptions.collectAsStateWithLifecycle()
    val roleOptions by optionsVM.roleOptions.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manajemen Unit (6-Pilar)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, "Tambah Unit", tint = Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)
        ) {
            Text(
                "Daftar Unit Aktif", 
                style = MaterialTheme.typography.titleLarge, 
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                "Identitas grup yang dirakit dari 6 kategori master data.", 
                style = MaterialTheme.typography.bodySmall, 
                color = Color.Gray
            )
            
            Spacer(Modifier.height(16.dp))

            if (masterClasses.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        "Belum ada unit rakitan.\nKlik + untuk membuat unit baru.", 
                        color = Color.Gray, 
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(masterClasses, key = { it.classId }) { item ->
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
        }
    }

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
    
    // 🧠 Auto-Naming Engine
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
        title = { Text("Rakit Identitas Baru", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)
            ) {
                item { MasterOptionPicker("1. Tingkat (Grade)", gradeOptions, selectedGrade) { selectedGrade = it as GradeOption } }
                item { MasterOptionPicker("2. Departemen (Class)", classOptions, selectedClass) { selectedClass = it as ClassOption } }
                item { MasterOptionPicker("3. Jurusan (Program)", programOptions, selectedProgram) { selectedProgram = it as ProgramOption } }
                item { MasterOptionPicker("4. Sub-Unit (SubClass)", subClassOptions, selectedSubClass) { selectedSubClass = it as SubClassOption } }
                item { MasterOptionPicker("5. Periode (SubGrade)", subGradeOptions, selectedSubGrade) { selectedSubGrade = it as SubGradeOption } }
                item { MasterOptionPicker("6. Jabatan (Role)", roleOptions, selectedRole) { selectedRole = it as RoleOption } }

                item {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Preview Nama Unit:", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = autoClassName.ifEmpty { "(Pilih kategori)" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (autoClassName.isEmpty()) Color.Gray else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedGrade != null && selectedClass != null && selectedRole != null,
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
            ) { Text("Simpan") }
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
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.DashboardCustomize, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.className, 
                    fontWeight = FontWeight.Bold, 
                    style = MaterialTheme.typography.titleMedium
                )
                // Detail Join SQL
                Text(
                    text = "${item.gradeName} • ${item.classOptionName} • ${item.roleName}",
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