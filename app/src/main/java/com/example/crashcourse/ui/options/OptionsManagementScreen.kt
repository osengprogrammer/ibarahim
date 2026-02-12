package com.example.crashcourse.ui.options

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.crashcourse.ui.OptionsHelpers
import com.example.crashcourse.ui.components.AzuraInput // ✅ Pastikan import komponen Azura
import com.example.crashcourse.viewmodel.OptionsViewModel
import com.example.crashcourse.db.*

/**
 * 🛠️ Azura Tech Options Management
 * Versi Teroptimasi: Local Search + Incremental Sync Trigger
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionsManagementScreen(
    onNavigateBack: () -> Unit,
    viewModel: OptionsViewModel = viewModel()
) {
    val context = LocalContext.current
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    
    // --- 📊 CATEGORY CONFIGURATION ---
    val optionTypes = listOf("Class", "SubClass", "Grade", "SubGrade", "Program", "Role")
    var selectedType by remember { mutableStateOf(optionTypes[0]) }
    var typeExpanded by remember { mutableStateOf(false) }

    // --- 🔍 FILTER & SEARCH STATE ---
    var searchQuery by remember { mutableStateOf("") }

    // --- 📊 DATA OBSERVATION ---
    val classOptions by viewModel.classOptions.collectAsStateWithLifecycle()
    val subClassOptions by viewModel.subClassOptions.collectAsStateWithLifecycle()
    val gradeOptions by viewModel.gradeOptions.collectAsStateWithLifecycle()
    val subGradeOptions by viewModel.subGradeOptions.collectAsStateWithLifecycle()
    val programOptions by viewModel.programOptions.collectAsStateWithLifecycle()
    val roleOptions by viewModel.roleOptions.collectAsStateWithLifecycle()

    // Determinasi list mana yang ditampilkan
    val rawList = when (selectedType) {
        "Class" -> classOptions
        "SubClass" -> subClassOptions
        "Grade" -> gradeOptions
        "SubGrade" -> subGradeOptions
        "Program" -> programOptions
        "Role" -> roleOptions
        else -> emptyList()
    }
    
    // 🧠 LOGIKA FILTER LOKAL (Sangat Cepat di Memori)
    val filteredList = remember(rawList, searchQuery) {
        rawList.filter { item ->
            OptionsHelpers.getName(item).contains(searchQuery, ignoreCase = true)
        }
    }

    val potentialParents = when (selectedType) {
        "SubClass" -> classOptions
        "SubGrade" -> gradeOptions
        else -> emptyList()
    }

    // --- 📝 ADD STATE ---
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Kamus Master", fontWeight = FontWeight.Bold)
                        Text(
                            text = if (isSyncing) "Sinkronisasi..." else "Data Lokal",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSyncing) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.syncAllFromCloud() }, 
                        enabled = !isSyncing
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.CloudDownload, "Sync Incremental")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true }, 
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, null, tint = Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF8F9FA)) // Background bersih Azura
                .padding(16.dp)
        ) {
            
            // --- 🎯 1. CATEGORY PICKER ---
            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = !typeExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Pilih Pilar Data") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                    optionTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = { 
                                selectedType = type 
                                searchQuery = "" // Reset cari saat ganti kategori
                                typeExpanded = false 
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // --- 🔍 2. SEARCH BAR ---
            AzuraInput(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = "Cari di Kamus $selectedType",
                leadingIcon = Icons.Default.Search
            )

            Spacer(Modifier.height(16.dp))

            // --- 📜 3. FILTERED LIST VIEW ---
            if (filteredList.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (searchQuery.isEmpty()) "Data kosong" else "Pencarian tidak ditemukan",
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f), 
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp) // Beri ruang untuk FAB
                ) {
                    items(filteredList, key = { OptionsHelpers.getId(it) }) { item ->
                        ExpandableOptionCard(
                            option = item,
                            parentOptions = potentialParents,
                            onUpdate = { updated ->
                                viewModel.updateOption(
                                    type = selectedType,
                                    option = updated,
                                    name = OptionsHelpers.getName(updated),
                                    order = OptionsHelpers.getOrder(updated),
                                    parentId = OptionsHelpers.getParentId(updated)
                                )
                            },
                            onDelete = { viewModel.deleteOption(selectedType, it) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        OptionAddDialog(
            type = selectedType,
            parents = potentialParents,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, order, pId ->
                viewModel.addOption(selectedType, name, order, pId)
                showAddDialog = false
            }
        )
    }
}

// ... Component ExpandableOptionCard, OptionAddDialog, dan ParentPicker tetap sama ...

/**
 * 📝 CARD: Expandable for quick editing
 */
@Composable
fun ExpandableOptionCard(
    option: Any,
    parentOptions: List<Any>,
    onUpdate: (Any) -> Unit,
    onDelete: (Any) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var nameInput by remember(option) { mutableStateOf(OptionsHelpers.getName(option)) }
    var orderInput by remember(option) { mutableStateOf(OptionsHelpers.getOrder(option).toString()) }
    var parentIdInput by remember(option) { mutableStateOf(OptionsHelpers.getParentId(option)) }

    // Cari nama parent untuk display label
    val parentName = remember(parentIdInput, parentOptions) {
        parentOptions.find { OptionsHelpers.getId(it) == parentIdInput }?.let { OptionsHelpers.getName(it) }
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isExpanded) MaterialTheme.colorScheme.surfaceVariant else Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(OptionsHelpers.getName(option), fontWeight = FontWeight.Bold)
                    if (parentName != null) {
                        Text("Induk: $parentName", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Icon(if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
            }

            if (isExpanded) {
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = nameInput, onValueChange = { nameInput = it }, label = { Text("Nama") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                
                if (parentOptions.isNotEmpty()) {
                    ParentPicker(label = "Ganti Induk", options = parentOptions, selectedId = parentIdInput) { parentIdInput = it }
                    Spacer(Modifier.height(8.dp))
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { onDelete(option) }, colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)) {
                        Icon(Icons.Default.Delete, null); Text("Hapus")
                    }
                    Button(onClick = { 
                        val updated = OptionsHelpers.copyWith(option, nameInput, orderInput.toIntOrNull() ?: 0, parentIdInput)
                        onUpdate(updated)
                        isExpanded = false
                    }) { Text("Update") }
                }
            }
        }
    }
}

/**
 * ➕ DIALOG: Add New Option
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionAddDialog(
    type: String,
    parents: List<Any>,
    onDismiss: () -> Unit,
    onConfirm: (String, Int, Int?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var order by remember { mutableStateOf("0") }
    var selectedParentId by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah $type") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Item") }, modifier = Modifier.fillMaxWidth())
                if (parents.isNotEmpty()) {
                    ParentPicker(label = "Pilih Induk (Wajib)", options = parents, selectedId = selectedParentId) { selectedParentId = it }
                }
                OutlinedTextField(value = order, onValueChange = { order = it }, label = { Text("Urutan Tampilan") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && (parents.isEmpty() || selectedParentId != null),
                onClick = { onConfirm(name, order.toIntOrNull() ?: 0, selectedParentId) }
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

/**
 * 🛠️ MINI COMPONENT: Parent Dropdown
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentPicker(
    label: String,
    options: List<Any>,
    selectedId: Int?,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = options.find { OptionsHelpers.getId(it) == selectedId }?.let { OptionsHelpers.getName(it) } ?: "Pilih Induk"

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { parent ->
                DropdownMenuItem(
                    text = { Text(OptionsHelpers.getName(parent)) },
                    onClick = { onSelect(OptionsHelpers.getId(parent)); expanded = false }
                )
            }
        }
    }
}