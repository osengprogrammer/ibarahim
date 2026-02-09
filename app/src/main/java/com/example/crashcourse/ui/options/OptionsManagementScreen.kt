package com.example.crashcourse.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crashcourse.viewmodel.OptionsViewModel
import com.example.crashcourse.db.*

// Import helpers secara eksplisit
import com.example.crashcourse.ui.OptionsHelpers.getName
import com.example.crashcourse.ui.OptionsHelpers.getOrder
import com.example.crashcourse.ui.OptionsHelpers.getParentId
import com.example.crashcourse.ui.OptionsHelpers.setName
import com.example.crashcourse.ui.OptionsHelpers.setOrder
import com.example.crashcourse.ui.OptionsHelpers.setParentId
import com.example.crashcourse.ui.OptionsHelpers.getId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionsManagementScreen(
    viewModel: OptionsViewModel = viewModel()
) {
    // 🚀 STATE SINKRONISASI
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()

    // 1. Kategori Lengkap
    val optionTypes = listOf("Class", "SubClass", "Grade", "SubGrade", "Program", "Role")
    var selectedOptionType by remember { mutableStateOf(optionTypes[0]) }
    var expanded by remember { mutableStateOf(false) }

    // 2. State untuk Dialog Tambah Data
    var showAddDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newOrder by remember { mutableStateOf("0") }
    var newParentId by remember { mutableStateOf<Int?>(null) }

    // 3. Koleksi Data dari Database (Reaktif)
    val classOptions by viewModel.classOptions.collectAsStateWithLifecycle(emptyList())
    val subClassOptions by viewModel.subClassOptions.collectAsStateWithLifecycle(emptyList())
    val gradeOptions by viewModel.gradeOptions.collectAsStateWithLifecycle(emptyList())
    val subGradeOptions by viewModel.subGradeOptions.collectAsStateWithLifecycle(emptyList())
    val programOptions by viewModel.programOptions.collectAsStateWithLifecycle(emptyList())
    val roleOptions by viewModel.roleOptions.collectAsStateWithLifecycle(emptyList())

    // 4. Mapping List berdasarkan pilihan Kategori
    val options = when (selectedOptionType) {
        "Class" -> classOptions
        "SubClass" -> subClassOptions
        "Grade" -> gradeOptions
        "SubGrade" -> subGradeOptions
        "Program" -> programOptions
        "Role" -> roleOptions
        else -> emptyList()
    }
    
    // 5. Mapping Parent (Relasi)
    val parentOptions = when (selectedOptionType) {
        "SubClass" -> classOptions
        "SubGrade" -> gradeOptions
        else -> emptyList()
    }

    // --- DIALOG CRUD (CREATE) ---
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { 
                showAddDialog = false
                newName = ""; newOrder = "0"; newParentId = null 
            },
            title = { Text("Tambah $selectedOptionType Baru", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Nama $selectedOptionType") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newOrder,
                        onValueChange = { newOrder = it },
                        label = { Text("Urutan Tampilan") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (parentOptions.isNotEmpty()) {
                        var parentExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = parentOptions.find { getId(it) == newParentId }?.let { getName(it) } ?: "Pilih Parent",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Relasi Induk") },
                                trailingIcon = {
                                    IconButton(onClick = { parentExpanded = !parentExpanded }) {
                                        Icon(Icons.Default.ArrowDropDown, null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            DropdownMenu(
                                expanded = parentExpanded,
                                onDismissRequest = { parentExpanded = false }
                            ) {
                                parentOptions.forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text(getName(p)) },
                                        onClick = {
                                            newParentId = getId(p)
                                            parentExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.addOption(selectedOptionType, newName, newOrder.toIntOrNull() ?: 0, newParentId)
                            newName = ""; newOrder = "0"; newParentId = null
                            showAddDialog = false
                        }
                    },
                    enabled = newName.isNotBlank()
                ) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showAddDialog = false
                    newName = ""; newOrder = "0"; newParentId = null 
                }) { Text("Batal") }
            }
        )
    }

    // --- MAIN UI ---
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        
        // 🚀 HEADER DENGAN TOMBOL SYNC PULL
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Data Master", 
                    style = MaterialTheme.typography.headlineMedium, 
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Sinkronisasi Master Data AzuraTech",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
            
            // TOMBOL SYNC PULL
            IconButton(
                onClick = { viewModel.syncAllFromCloud() },
                enabled = !isSyncing
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        Icons.Default.CloudDownload, 
                        contentDescription = "Sync", 
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dropdown Kategori (READ)
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selectedOptionType,
                onValueChange = {},
                readOnly = true,
                label = { Text("Pilih Kategori") },
                leadingIcon = { Icon(Icons.Default.Category, null) },
                trailingIcon = { IconButton(onClick = { expanded = !expanded }) { Icon(Icons.Filled.ArrowDropDown, null) } },
                modifier = Modifier.fillMaxWidth()
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.fillMaxWidth()) {
                optionTypes.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type) }, 
                        onClick = { 
                            selectedOptionType = type
                            expanded = false 
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // LIST DATA (READ / UPDATE / DELETE)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(options, key = { getId(it) }) { option ->
                ExpandableOptionCard(
                    option = option,
                    parentOptions = parentOptions,
                    onSave = { updated ->
                        viewModel.updateOption(
                            selectedOptionType, 
                            updated, 
                            getName(updated), 
                            getOrder(updated), 
                            getParentId(updated)
                        )
                    },
                    onDelete = { toDelete ->
                        viewModel.deleteOption(selectedOptionType, toDelete)
                    }
                )
            }
        }
        
        // Floating Action Button (CREATE Trigger)
        Button(
            onClick = { showAddDialog = true }, 
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("Tambah $selectedOptionType")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandableOptionCard(
    option: Any,
    parentOptions: List<Any>,
    onSave: (Any) -> Unit,
    onDelete: (Any) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    var nameInput by remember(option) { mutableStateOf(getName(option)) }
    var orderInput by remember(option) { mutableStateOf(getOrder(option).toString()) }
    var parentIdInput by remember(option) { mutableStateOf(getParentId(option)) }

    val isChanged = nameInput != getName(option) || 
                    orderInput != getOrder(option).toString() || 
                    parentIdInput != getParentId(option)

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(getName(option), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Urutan: ${getOrder(option)}", style = MaterialTheme.typography.bodySmall)
                }
                Icon(imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Nama") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = orderInput,
                    onValueChange = { orderInput = it },
                    label = { Text("Urutan") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (parentOptions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = parentOptions.find { getId(it) == parentIdInput }?.let { getName(it) } ?: "Pilih Parent",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Induk (Parent)") },
                            trailingIcon = { IconButton(onClick = { dropdownExpanded = !dropdownExpanded }) { Icon(Icons.Default.ArrowDropDown, null) } },
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(expanded = dropdownExpanded, onDismissRequest = { dropdownExpanded = false }) {
                            parentOptions.forEach { parent ->
                                DropdownMenuItem(
                                    text = { Text(getName(parent)) },
                                    onClick = {
                                        parentIdInput = getId(parent)
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        onClick = { onDelete(option) }, 
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Hapus")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            var updated = option
                            updated = setName(updated, nameInput)
                            updated = setOrder(updated, orderInput.toIntOrNull() ?: 0)
                            updated = setParentId(updated, parentIdInput)
                            onSave(updated)
                            isExpanded = false
                        },
                        enabled = isChanged && nameInput.isNotBlank()
                    ) {
                        Icon(Icons.Default.Save, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Simpan")
                    }
                }
            }
        }
    }
}