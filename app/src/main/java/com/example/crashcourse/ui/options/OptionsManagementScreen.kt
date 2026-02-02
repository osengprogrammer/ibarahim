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

// Import helper functions
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
    viewModel: OptionsViewModel = viewModel(),
    onNavigateBack: () -> Unit // Menambah navigasi balik
) {
    var selectedOptionType by remember { mutableStateOf("Class") }
    var expanded by remember { mutableStateOf(false) }
    val optionTypes = listOf("Class", "SubClass", "Grade", "SubGrade", "Program", "Role")

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kelola Data Master") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
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
        ) {
            // Dropdown untuk pilih tipe data (Kelas, Jurusan, dll)
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedOptionType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Pilih Tipe Data") },
                    trailingIcon = {
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
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

            // List data yang bisa di-edit secara inline
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(options) { option ->
                    ExpandableOptionCard(
                        option = option,
                        parentOptions = parentOptions,
                        onSave = { updated ->
                            val parentId = when (selectedOptionType) {
                                "SubClass" -> getParentId(updated) ?: 1
                                "SubGrade" -> getParentId(updated) ?: 1
                                else -> null
                            }
                            viewModel.updateOption(
                                selectedOptionType,
                                updated,
                                getName(updated),
                                getOrder(updated),
                                parentId
                            )
                        },
                        onDelete = { viewModel.deleteOption(selectedOptionType, it) }
                    )
                }
            }
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

    var name by remember { mutableStateOf(getName(option)) }
    var order by remember { mutableStateOf(getOrder(option).toString()) }
    var parentId by remember { mutableStateOf(getParentId(option) ?: 1) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { isExpanded = !isExpanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(getName(option), style = MaterialTheme.typography.titleMedium)
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        onSave(setName(option, it))
                    },
                    label = { Text("Nama") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = order,
                    onValueChange = { 
                        order = it
                        onSave(setOrder(option, it.toIntOrNull() ?: 0))
                    },
                    label = { Text("Urutan (Order)") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (parentOptions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = parentOptions.find { getId(it) == parentId }?.let { getName(it) } ?: "Pilih Parent",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Induk (Parent)") },
                            trailingIcon = {
                                IconButton(onClick = { dropdownExpanded = !dropdownExpanded }) {
                                    Icon(Icons.Default.ArrowDropDown, null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
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

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onDelete(option) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Hapus")
                }
            }
        }
    }
}