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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crashcourse.viewmodel.OptionsViewModel
import com.example.crashcourse.db.*

// Import helper functions secara eksplisit
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
    var selectedOptionType by remember { mutableStateOf("Class") }
    var expanded by remember { mutableStateOf(false) }
    val optionTypes = listOf("Class", "SubClass", "Grade", "SubGrade", "Program", "Role")

    // State untuk Dialog Tambah Data
    var showAddDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newOrder by remember { mutableStateOf("0") }
    var newParentId by remember { mutableStateOf<Int?>(null) }

    // Collect lists dari database Room
    val classOptions by viewModel.classOptions.collectAsStateWithLifecycle(emptyList())
    val subClassOptions by viewModel.subClassOptions.collectAsStateWithLifecycle(emptyList())
    val gradeOptions by viewModel.gradeOptions.collectAsStateWithLifecycle(emptyList())
    val subGradeOptions by viewModel.subGradeOptions.collectAsStateWithLifecycle(emptyList())
    val programOptions by viewModel.programOptions.collectAsStateWithLifecycle(emptyList())
    val roleOptions by viewModel.roleOptions.collectAsStateWithLifecycle(emptyList())

    val options = when (selectedOptionType) {
        "Class" -> classOptions
        "SubClass" -> subClassOptions
        "Grade" -> gradeOptions
        "SubGrade" -> subGradeOptions
        "Program" -> programOptions
        "Role" -> roleOptions
        else -> emptyList()
    }
    
    val parentOptions = when (selectedOptionType) {
        "SubClass" -> classOptions
        "SubGrade" -> gradeOptions
        else -> emptyList()
    }

    // --- DIALOG TAMBAH DATA BARU ---
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Tambah $selectedOptionType Baru") },
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

                    if (selectedOptionType == "SubClass" || selectedOptionType == "SubGrade") {
                        var parentExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = parentOptions.find { getId(it) == newParentId }?.let { getName(it) } ?: "Pilih Parent",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Pilih Parent") },
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
                    }
                ) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Batal") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Kelola Data Master", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 16.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selectedOptionType,
                onValueChange = {},
                readOnly = true,
                label = { Text("Pilih Tipe Data") },
                trailingIcon = { IconButton(onClick = { expanded = !expanded }) { Icon(Icons.Filled.ArrowDropDown, null) } },
                modifier = Modifier.fillMaxWidth()
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.fillMaxWidth()) {
                optionTypes.forEach { type ->
                    DropdownMenuItem(text = { Text(type) }, onClick = { selectedOptionType = type; expanded = false })
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(options, key = { getId(it) }) { option ->
                // ✅ FITUR YANG TADI HILANG SUDAH DITAMBAHKAN DI BAWAH
                ExpandableOptionCard(
                    option = option,
                    parentOptions = parentOptions,
                    onSave = { updated ->
                        viewModel.updateOption(selectedOptionType, updated, getName(updated), getOrder(updated), getParentId(updated))
                    },
                    onDelete = { viewModel.deleteOption(selectedOptionType, it) }
                )
            }
        }
        
        Button(onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("Tambah $selectedOptionType Baru")
        }
    }
}

// ✅ BERIKUT ADALAH FUNGSI YANG TADI UNRESOLVED (DITAMBAHKAN KEMBALI)
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

    var name by remember(option) { mutableStateOf(getName(option)) }
    var order by remember(option) { mutableStateOf(getOrder(option).toString()) }
    var parentId by remember(option) { mutableStateOf(getParentId(option) ?: 1) }

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
                    Text(getName(option), style = MaterialTheme.typography.titleMedium)
                    Text("Urutan: ${getOrder(option)}", style = MaterialTheme.typography.bodySmall)
                }
                Icon(imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; onSave(setName(option, it)) },
                    label = { Text("Nama") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = order,
                    onValueChange = { order = it; onSave(setOrder(option, it.toIntOrNull() ?: 0)) },
                    label = { Text("Urutan (Order)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (parentOptions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = parentOptions.find { getId(it) == parentId }?.let { getName(it) } ?: "Pilih Parent",
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
                                        parentId = getId(parent)
                                        onSave(setParentId(option, parentId))
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { onDelete(option) }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Icon(Icons.Default.Delete, null); Spacer(Modifier.width(4.dp)); Text("Hapus")
                    }
                }
            }
        }
    }
}