package com.example.crashcourse.ui.checkin

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.crashcourse.db.*
import com.example.crashcourse.ui.components.StatisticsDashboard
import com.example.crashcourse.utils.ExportUtils
import com.example.crashcourse.viewmodel.CheckInViewModel
import com.example.crashcourse.viewmodel.FaceViewModel
import com.example.crashcourse.viewmodel.OptionsViewModel
import com.example.crashcourse.viewmodel.AuthState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

// --- FUNGSI WARNA (Ganti Nama agar tidak bentrok) ---
private fun getAttendanceStatusColor(status: String): Color {
    return when (status.uppercase(Locale.ROOT)) {
        "PRESENT" -> Color(0xFF4CAF50)
        "SAKIT" -> Color(0xFFFFC107)
        "IZIN" -> Color(0xFF2196F3)
        "ALPHA" -> Color(0xFFF44336)
        else -> Color.Gray
    }
}

private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInRecordScreen(
    authState: AuthState.Active,
    checkInViewModel: CheckInViewModel = viewModel(),
    optionsViewModel: OptionsViewModel = viewModel(),
    faceViewModel: FaceViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var nameFilter by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf<LocalDate?>(null) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    var showFilters by remember { mutableStateOf(false) }
    
    var selectedClass by remember { mutableStateOf<ClassOption?>(null) }
    var selectedGrade by remember { mutableStateOf<GradeOption?>(null) }
    var selectedProgram by remember { mutableStateOf<ProgramOption?>(null) }

    var showManualDialog by remember { mutableStateOf(false) }
    var recordToEdit by remember { mutableStateOf<CheckInRecord?>(null) }

    val scopedStudents by faceViewModel.getScopedFaceList(authState).collectAsStateWithLifecycle(initialValue = emptyList())

    val records by remember(
        nameFilter, startDate, endDate, selectedClass, 
        selectedGrade, selectedProgram, authState
    ) {
        checkInViewModel.getScopedCheckIns(
            authState = authState,
            nameFilter = nameFilter,
            startDate = startDate?.format(dateFormatter) ?: "",
            endDate = endDate?.format(dateFormatter) ?: "",
            className = selectedClass?.name, 
            gradeId = selectedGrade?.id,
            programId = selectedProgram?.id
        )
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val attendanceSummary = remember(records) {
        val summary = mutableMapOf("PRESENT" to 0, "SAKIT" to 0, "IZIN" to 0, "ALPHA" to 0)
        records.forEach { record ->
            val status = record.status.uppercase(Locale.ROOT)
            if (summary.containsKey(status)) {
                summary[status] = (summary[status] ?: 0) + 1
            }
        }
        summary
    }

    val classOptions by optionsViewModel.classOptions.collectAsStateWithLifecycle(emptyList())
    val gradeOptions by optionsViewModel.gradeOptions.collectAsStateWithLifecycle(emptyList())
    val programOptions by optionsViewModel.programOptions.collectAsStateWithLifecycle(emptyList())

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let { destinationUri ->
            scope.launch(Dispatchers.IO) {
                try {
                    ExportUtils.writePdfToUri(context, destinationUri, records)
                    launch(Dispatchers.Main) {
                        Toast.makeText(context, "Laporan Berhasil!", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    launch(Dispatchers.Main) {
                        Toast.makeText(context, "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Riwayat Absensi", fontWeight = FontWeight.Bold)
                        Text(
                            text = if (authState.role == "ADMIN") "Semua Data" else "Skop: ${authState.assignedClasses.joinToString(", ")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        if (records.isNotEmpty()) exportLauncher.launch("Laporan_Azura.pdf") 
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
            OutlinedTextField(
                value = nameFilter,
                onValueChange = { nameFilter = it },
                label = { Text("Cari Nama") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = RoundedCornerShape(12.dp)
            )

            if (showFilters) {
                Spacer(modifier = Modifier.height(12.dp))
                FilterSection(
                    startDate = startDate, onStartDateChange = { startDate = it },
                    endDate = endDate, onEndDateChange = { endDate = it },
                    classOptions = classOptions, selectedClass = selectedClass, onClassSelected = { selectedClass = it },
                    gradeOptions = gradeOptions, selectedGrade = selectedGrade, onGradeSelected = { selectedGrade = it },
                    programOptions = programOptions, selectedProgram = selectedProgram, onProgramSelected = { selectedProgram = it }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            StatisticsDashboard(summary = attendanceSummary)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(records) { record ->
                    CheckInRecordCard(record, optionsViewModel) { recordToEdit = record }
                }
            }
        }

        if (showManualDialog || recordToEdit != null) {
            ManualEntryDialog(
                existingRecord = recordToEdit,
                allStudents = scopedStudents, 
                onDismiss = { showManualDialog = false; recordToEdit = null },
                onSave = { record ->
                    if (recordToEdit != null) checkInViewModel.updateRecord(record)
                    else checkInViewModel.addManualRecord(record)
                    showManualDialog = false; recordToEdit = null
                },
                onDelete = { record ->
                    checkInViewModel.deleteRecord(record)
                    recordToEdit = null
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CheckInRecordCard(record: CheckInRecord, optionsViewModel: OptionsViewModel, onLongClick: () -> Unit) {
    val classOptions by optionsViewModel.classOptions.collectAsStateWithLifecycle(emptyList())
    val gradeOptions by optionsViewModel.gradeOptions.collectAsStateWithLifecycle(emptyList())
    
    val className = record.className ?: record.classId?.let { id -> classOptions.find { it.id == id }?.name } ?: ""
    val gradeName = record.gradeName ?: record.gradeId?.let { id -> gradeOptions.find { it.id == id }?.name } ?: ""

    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = {}, onLongClick = onLongClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = getAttendanceStatusColor(record.status), 
                shape = CircleShape, 
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(record.status.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(record.name, fontWeight = FontWeight.Bold)
                Text("$className • $gradeName", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text(record.timestamp.format(DateTimeFormatter.ofPattern("HH:mm - dd MMM")), style = MaterialTheme.typography.labelSmall)
            }
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingRecord == null) "Absen Manual" else "Edit Absensi") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (existingRecord == null) {
                    FilterDropdown("Pilih Murid", allStudents, selectedStudent, { selectedStudent = it }, { it.name })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("PRESENT", "SAKIT", "IZIN", "ALPHA").forEach { opt ->
                        FilterChip(
                            selected = status == opt,
                            onClick = { status = opt },
                            label = { Text(opt.take(1)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = getAttendanceStatusColor(opt).copy(alpha = 0.2f)
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                // ✅ FIX: Gunakan Safe Call ?.copy() untuk menghindari error nullability
                val record = existingRecord?.copy(status = status) ?: CheckInRecord(
                    name = selectedStudent?.name ?: "Unknown",
                    timestamp = LocalDateTime.now(),
                    faceId = null,
                    status = status,
                    classId = selectedStudent?.classId,
                    gradeId = selectedStudent?.gradeId,
                    className = selectedStudent?.className,
                    gradeName = selectedStudent?.grade
                )
                onSave(record)
            }) { Text("Simpan") }
        },
        dismissButton = {
            if (existingRecord != null) {
                TextButton(onClick = { onDelete(existingRecord) }) { Text("Hapus", color = Color.Red) }
            }
        }
    )
}

// --- FILTER COMPONENTS (Dropdown & Section) ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSection(
    startDate: LocalDate?, onStartDateChange: (LocalDate?) -> Unit,
    endDate: LocalDate?, onEndDateChange: (LocalDate?) -> Unit,
    classOptions: List<ClassOption>, selectedClass: ClassOption?, onClassSelected: (ClassOption?) -> Unit,
    gradeOptions: List<GradeOption>, selectedGrade: GradeOption?, onGradeSelected: (GradeOption?) -> Unit,
    programOptions: List<ProgramOption>, selectedProgram: ProgramOption?, onProgramSelected: (ProgramOption?) -> Unit
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    if (showStartPicker) {
        val dateState = rememberDatePickerState()
        DatePickerDialog(onDismissRequest = { showStartPicker = false }, confirmButton = {
            TextButton(onClick = {
                dateState.selectedDateMillis?.let { onStartDateChange(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()) }
                showStartPicker = false
            }) { Text("OK") }
        }) { DatePicker(state = dateState) }
    }
    // ... (Simetris untuk endPicker jika diperlukan)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showStartPicker = true }, modifier = Modifier.weight(1f)) { Text(startDate?.toString() ?: "Mulai") }
            OutlinedButton(onClick = { showEndPicker = true }, modifier = Modifier.weight(1f)) { Text(endDate?.toString() ?: "Selesai") }
        }
        FilterDropdown("Kelas", classOptions, selectedClass, onClassSelected, { it.name })
        FilterDropdown("Grade", gradeOptions, selectedGrade, onGradeSelected, { it.name })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> FilterDropdown(label: String, options: List<T>, selected: T?, onSelected: (T?) -> Unit, getLabel: (T) -> String) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = selected?.let { getLabel(it) } ?: "", onValueChange = {},
            label = { Text(label, fontSize = 12.sp) }, readOnly = true,
            trailingIcon = { IconButton(onClick = { expanded = !expanded }) { Icon(Icons.Default.ArrowDropDown, null) } },
            modifier = Modifier.fillMaxWidth()
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Semua") }, onClick = { onSelected(null); expanded = false })
            options.forEach { opt -> DropdownMenuItem(text = { Text(getLabel(opt)) }, onClick = { onSelected(opt); expanded = false }) }
        }
    }
}