package com.example.crashcourse.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.example.crashcourse.utils.showToast
import com.example.crashcourse.viewmodel.CheckInViewModel
import com.example.crashcourse.viewmodel.FaceViewModel
import com.example.crashcourse.viewmodel.OptionsViewModel
import com.example.crashcourse.viewmodel.AuthState // ✅ Import AuthState
import kotlinx.coroutines.launch
import java.time.*
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInRecordScreen(
    authState: AuthState.Active, // ✅ FIX: Wajib kirim AuthState agar bisa filter Scope
    checkInViewModel: CheckInViewModel = viewModel(),
    optionsViewModel: OptionsViewModel = viewModel(),
    faceViewModel: FaceViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- STATES FILTERS ---
    var nameFilter by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf<LocalDate?>(null) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    var showFilters by remember { mutableStateOf(false) }
    
    var selectedClass by remember { mutableStateOf<ClassOption?>(null) }
    var selectedSubClass by remember { mutableStateOf<SubClassOption?>(null) }
    var selectedGrade by remember { mutableStateOf<GradeOption?>(null) }
    var selectedSubGrade by remember { mutableStateOf<SubGradeOption?>(null) }
    var selectedProgram by remember { mutableStateOf<ProgramOption?>(null) }
    var selectedRole by remember { mutableStateOf<RoleOption?>(null) }

    // --- STATES CRUD ---
    var showManualDialog by remember { mutableStateOf(false) }
    var recordToEdit by remember { mutableStateOf<CheckInRecord?>(null) }

    // --- DATA COLLECTIONS ---
    // ✅ FIX 1: Gunakan data murid yang juga sudah difilter Scope
    val scopedStudents by faceViewModel.getScopedFaceList(authState).collectAsStateWithLifecycle(initialValue = emptyList())

    // ✅ FIX 2: Gunakan getScopedCheckIns (Bukan getFilteredCheckIns yang private)
    val records by remember(
        nameFilter, startDate, endDate, selectedClass, selectedSubClass, 
        selectedGrade, selectedSubGrade, selectedProgram, selectedRole, authState
    ) {
        checkInViewModel.getScopedCheckIns(
            authState = authState, // Memasukkan Scope Guru ke dalam Flow
            nameFilter = nameFilter,
            startDate = startDate?.format(dateFormatter) ?: "",
            endDate = endDate?.format(dateFormatter) ?: "",
            classId = selectedClass?.id,
            subClassId = selectedSubClass?.id,
            gradeId = selectedGrade?.id,
            subGradeId = selectedSubGrade?.id,
            programId = selectedProgram?.id,
            roleId = selectedRole?.id
        )
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val classOptions by optionsViewModel.classOptions.collectAsStateWithLifecycle(emptyList())
    val subClassOptions by optionsViewModel.subClassOptions.collectAsStateWithLifecycle(emptyList())
    val gradeOptions by optionsViewModel.gradeOptions.collectAsStateWithLifecycle(emptyList())
    val subGradeOptions by optionsViewModel.subGradeOptions.collectAsStateWithLifecycle(emptyList())
    val programOptions by optionsViewModel.programOptions.collectAsStateWithLifecycle(emptyList())
    val roleOptions by optionsViewModel.roleOptions.collectAsStateWithLifecycle(emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Riwayat Absensi")
                        Text(
                            text = if (authState.role == "ADMIN") "Semua Data" else "Skop: ${authState.assignedClasses.joinToString(", ")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        scope.launch {
                            // ✅ FIX 3: Export juga mematuhi Scope
                            val file = checkInViewModel.exportToPdf(records, authState)
                            context.showToast("Export PDF: ${file.name}")
                        }
                    }) { Icon(Icons.Default.PictureAsPdf, null) }
                    
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(if (showFilters) Icons.Default.FilterListOff else Icons.Default.FilterList, null)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showManualDialog = true }) {
                Icon(Icons.Default.Add, "Manual")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            // SEARCH BAR
            OutlinedTextField(
                value = nameFilter,
                onValueChange = { nameFilter = it },
                label = { Text("Cari Nama Murid") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = { 
                    if (nameFilter.isNotEmpty()) {
                        IconButton(onClick = { nameFilter = "" }) { Icon(Icons.Default.Clear, null) }
                    } else {
                        Icon(Icons.Default.Search, null) 
                    }
                }
            )

            // FILTER SECTION
            if (showFilters) {
                Spacer(modifier = Modifier.height(8.dp))
                FilterSection(
                    startDate = startDate, onStartDateChange = { startDate = it },
                    endDate = endDate, onEndDateChange = { endDate = it },
                    classOptions = classOptions, selectedClass = selectedClass, onClassSelected = { selectedClass = it },
                    subClassOptions = subClassOptions, selectedSubClass = selectedSubClass, onSubClassSelected = { selectedSubClass = it },
                    gradeOptions = gradeOptions, selectedGrade = selectedGrade, onGradeSelected = { selectedGrade = it },
                    subGradeOptions = subGradeOptions, selectedSubGrade = selectedSubGrade, onSubGradeSelected = { selectedSubGrade = it },
                    programOptions = programOptions, selectedProgram = selectedProgram, onProgramSelected = { selectedProgram = it },
                    roleOptions = roleOptions, selectedRole = selectedRole, onRoleSelected = { selectedRole = it }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // LIST RECORDS
            if (records.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Belum ada data absensi dalam skop Anda.", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(records) { record ->
                        CheckInRecordCard(
                            record = record,
                            optionsViewModel = optionsViewModel,
                            onLongClick = { recordToEdit = record }
                        )
                    }
                }
            }
        }

        // DIALOGS
        if (showManualDialog || recordToEdit != null) {
            ManualEntryDialog(
                existingRecord = recordToEdit,
                allStudents = scopedStudents, // ✅ Menggunakan murid yang sudah difilter skop
                onDismiss = { 
                    showManualDialog = false
                    recordToEdit = null
                },
                onSave = { record ->
                    if (recordToEdit != null) checkInViewModel.updateRecord(record)
                    else checkInViewModel.addManualRecord(record)
                    showManualDialog = false
                    recordToEdit = null
                },
                onDelete = { record ->
                    checkInViewModel.deleteRecord(record)
                    recordToEdit = null
                }
            )
        }
    }
}

// --- KOMPONEN PENDUKUNG ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSection(
    startDate: LocalDate?,
    onStartDateChange: (LocalDate?) -> Unit,
    endDate: LocalDate?,
    onEndDateChange: (LocalDate?) -> Unit,
    classOptions: List<ClassOption>,
    selectedClass: ClassOption?,
    onClassSelected: (ClassOption?) -> Unit,
    subClassOptions: List<SubClassOption>,
    selectedSubClass: SubClassOption?,
    onSubClassSelected: (SubClassOption?) -> Unit,
    gradeOptions: List<GradeOption>,
    selectedGrade: GradeOption?,
    onGradeSelected: (GradeOption?) -> Unit,
    subGradeOptions: List<SubGradeOption>,
    selectedSubGrade: SubGradeOption?,
    onSubGradeSelected: (SubGradeOption?) -> Unit,
    programOptions: List<ProgramOption>,
    selectedProgram: ProgramOption?,
    onProgramSelected: (ProgramOption?) -> Unit,
    roleOptions: List<RoleOption>,
    selectedRole: RoleOption?,
    onRoleSelected: (RoleOption?) -> Unit
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    // Date Picker Logic
    if (showStartPicker) {
        val dateState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let {
                        onStartDateChange(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate())
                    }
                    showStartPicker = false
                }) { Text("OK") }
            }
        ) { DatePicker(state = dateState) }
    }

    if (showEndPicker) {
        val dateState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let {
                        onEndDateChange(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate())
                    }
                    showEndPicker = false
                }) { Text("OK") }
            }
        ) { DatePicker(state = dateState) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showStartPicker = true }, modifier = Modifier.weight(1f)) {
                Text(startDate?.toString() ?: "Tgl Mulai")
            }
            OutlinedButton(onClick = { showEndPicker = true }, modifier = Modifier.weight(1f)) {
                Text(endDate?.toString() ?: "Tgl Selesai")
            }
        }
        
        FilterDropdown("Kelas", classOptions, selectedClass, onClassSelected, { it.name })
        if (selectedClass != null) {
            FilterDropdown("Sub Kelas", subClassOptions.filter { it.parentClassId == selectedClass.id }, selectedSubClass, onSubClassSelected, { it.name })
        }
        FilterDropdown("Grade", gradeOptions, selectedGrade, onGradeSelected, { it.name })
        FilterDropdown("Program", programOptions, selectedProgram, onProgramSelected, { it.name })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> FilterDropdown(
    label: String,
    options: List<T>,
    selectedOption: T?,
    onOptionSelected: (T?) -> Unit,
    getOptionLabel: (T) -> String
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedOption?.let { getOptionLabel(it) } ?: "",
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            trailingIcon = { IconButton(onClick = { expanded = !expanded }) { Icon(Icons.Default.ArrowDropDown, null) } },
            modifier = Modifier.fillMaxWidth()
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Semua") }, onClick = { onOptionSelected(null); expanded = false })
            options.forEach { opt ->
                DropdownMenuItem(text = { Text(getOptionLabel(opt)) }, onClick = { onOptionSelected(opt); expanded = false })
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CheckInRecordCard(
    record: CheckInRecord, 
    optionsViewModel: OptionsViewModel, 
    onLongClick: () -> Unit
) {
    val classOptions by optionsViewModel.classOptions.collectAsStateWithLifecycle(emptyList())
    val className = remember(record.classId, classOptions) {
        record.classId?.let { id -> classOptions.find { it.id == id }?.name } ?: record.className
    }
    val gradeOptions by optionsViewModel.gradeOptions.collectAsStateWithLifecycle(emptyList())
    val gradeName = remember(record.gradeId, gradeOptions) {
        record.gradeId?.let { id -> gradeOptions.find { it.id == id }?.name } ?: record.gradeName
    }

    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = {}, onLongClick = onLongClick)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = getStatusColor(record.status), shape = CircleShape, modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(record.status.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(record.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                
                val details = listOfNotNull(className, gradeName).joinToString(" - ")
                if (details.isNotEmpty()) {
                    Text(details, style = MaterialTheme.typography.bodySmall)
                }
                
                Text(
                    record.timestamp.format(DateTimeFormatter.ofPattern("HH:mm - dd MMM yyyy")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (record.faceId == null) Icon(Icons.Default.EditNote, null, tint = Color.Gray)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntryDialog(
    existingRecord: CheckInRecord?,
    allStudents: List<FaceEntity>,
    onDismiss: () -> Unit,
    onSave: (CheckInRecord) -> Unit,
    onDelete: (CheckInRecord) -> Unit
) {
    var selectedStudent by remember { mutableStateOf(allStudents.find { it.name == existingRecord?.name }) }
    var status by remember { mutableStateOf(existingRecord?.status ?: "PRESENT") }
    val statusOptions = listOf("PRESENT", "SAKIT", "IZIN", "ALPHA")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingRecord == null) "Absen Manual" else "Edit Absensi") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (existingRecord == null) {
                    if (allStudents.isEmpty()) {
                        Text("⚠️ Belum ada data murid dalam skop Anda.", color = MaterialTheme.colorScheme.error)
                    } else {
                        FilterDropdown("Pilih Murid", allStudents, selectedStudent, { selectedStudent = it }, { it.name })
                    }
                } else {
                    Text("Murid: ${existingRecord.name}", fontWeight = FontWeight.Bold)
                }
                
                Text("Status:")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    statusOptions.forEach { opt ->
                        FilterChip(
                            selected = status == opt,
                            onClick = { status = opt },
                            label = { Text(opt.take(1)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = getStatusColor(opt).copy(alpha = 0.2f),
                                selectedLabelColor = getStatusColor(opt)
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val record = existingRecord?.copy(status = status) ?: CheckInRecord(
                        name = selectedStudent?.name ?: "Unknown",
                        timestamp = LocalDateTime.now(),
                        faceId = null,
                        status = status,
                        classId = selectedStudent?.classId,
                        subClassId = selectedStudent?.subClassId,
                        gradeId = selectedStudent?.gradeId,
                        subGradeId = selectedStudent?.subGradeId,
                        programId = selectedStudent?.programId,
                        roleId = selectedStudent?.roleId,
                        className = selectedStudent?.className,
                        gradeName = selectedStudent?.grade
                    )
                    onSave(record)
                },
                enabled = selectedStudent != null || existingRecord != null
            ) { Text("Simpan") }
        },
        dismissButton = {
            if (existingRecord != null) {
                TextButton(onClick = { onDelete(existingRecord) }) { Text("Hapus", color = MaterialTheme.colorScheme.error) }
            } else {
                TextButton(onClick = onDismiss) { Text("Batal") }
            }
        }
    )
}

fun getStatusColor(status: String): Color = when (status) {
    "PRESENT" -> Color(0xFF4CAF50)
    "SAKIT" -> Color(0xFFFFC107)
    "IZIN" -> Color(0xFF2196F3)
    "ALPHA" -> Color(0xFFF44336)
    else -> Color.Gray
}