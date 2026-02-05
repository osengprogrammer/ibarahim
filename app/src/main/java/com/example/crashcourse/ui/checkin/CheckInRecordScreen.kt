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

// --- FUNGSI WARNA (Agar Konsisten) ---
private fun getAttendanceStatusColor(status: String): Color {
    return when (status.uppercase(Locale.ROOT)) {
        "PRESENT" -> Color(0xFF4CAF50) // Hijau
        "SAKIT" -> Color(0xFFFFC107)   // Kuning
        "IZIN" -> Color(0xFF2196F3)    // Biru
        "ALPHA" -> Color(0xFFF44336)   // Merah
        "LATE" -> Color(0xFFFF5722)    // Oranye
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

    // --- State Filter ---
    var nameFilter by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf<LocalDate?>(null) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    var showFilters by remember { mutableStateOf(false) }
    
    var selectedClass by remember { mutableStateOf<ClassOption?>(null) }
    var selectedGrade by remember { mutableStateOf<GradeOption?>(null) }
    var selectedProgram by remember { mutableStateOf<ProgramOption?>(null) }

    // --- State Dialog Manual ---
    var showManualDialog by remember { mutableStateOf(false) }
    var recordToEdit by remember { mutableStateOf<CheckInRecord?>(null) }

    // Mengambil daftar siswa sesuai Scope Guru/Admin untuk Dropdown Manual
    val scopedStudents by faceViewModel.getScopedFaceList(authState).collectAsStateWithLifecycle(initialValue = emptyList())

    // Mengambil Data Absensi yang sudah difilter
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

    // Menghitung Ringkasan Statistik
    val attendanceSummary = remember(records) {
        val summary = mutableMapOf("PRESENT" to 0, "SAKIT" to 0, "IZIN" to 0, "ALPHA" to 0)
        records.forEach { record ->
            val status = record.status.uppercase(Locale.ROOT)
            summary[status] = (summary[status] ?: 0) + 1
        }
        summary
    }

    val classOptions by optionsViewModel.classOptions.collectAsStateWithLifecycle(emptyList())
    val gradeOptions by optionsViewModel.gradeOptions.collectAsStateWithLifecycle(emptyList())
    val programOptions by optionsViewModel.programOptions.collectAsStateWithLifecycle(emptyList())

    // Launcher Export PDF
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let { destinationUri ->
            scope.launch(Dispatchers.IO) {
                try {
                    ExportUtils.writePdfToUri(context, destinationUri, records)
                    launch(Dispatchers.Main) {
                        Toast.makeText(context, "Laporan Berhasil Disimpan!", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    launch(Dispatchers.Main) {
                        Toast.makeText(context, "Gagal Export: ${e.message}", Toast.LENGTH_SHORT).show()
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
                            text = if (authState.role == "ADMIN") "Semua Data" else "Kelas: ${authState.assignedClasses.joinToString(", ")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        if (records.isNotEmpty()) exportLauncher.launch("Laporan_Azura_${LocalDate.now()}.pdf")
                        else Toast.makeText(context, "Tidak ada data untuk diexport", Toast.LENGTH_SHORT).show()
                    }) { Icon(Icons.Default.PictureAsPdf, "Export PDF") }
                    
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(if (showFilters) Icons.Default.FilterListOff else Icons.Default.FilterList, "Filter")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showManualDialog = true }) {
                Icon(Icons.Default.Add, "Absen Manual")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            // Kolom Pencarian Nama
            OutlinedTextField(
                value = nameFilter,
                onValueChange = { nameFilter = it },
                label = { Text("Cari Nama Siswa...") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Bagian Filter (Expandable)
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
            
            // Dashboard Statistik Ringkas
            StatisticsDashboard(summary = attendanceSummary)
            
            Spacer(modifier = Modifier.height(12.dp))

            // Daftar Absensi
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp) // Supaya tidak ketutup FAB
            ) {
                if (records.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Text("Belum ada data absensi.", color = Color.Gray)
                        }
                    }
                } else {
                    items(records) { record ->
                        CheckInRecordCard(record, optionsViewModel) { recordToEdit = record }
                    }
                }
            }
        }

        // Dialog Input/Edit Manual
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
    // Ambil nama kelas/grade dari ID jika namanya kosong (fallback)
    val classOptions by optionsViewModel.classOptions.collectAsStateWithLifecycle(emptyList())
    val gradeOptions by optionsViewModel.gradeOptions.collectAsStateWithLifecycle(emptyList())
    
    val className = record.className ?: record.classId?.let { id -> classOptions.find { it.id == id }?.name } ?: "-"
    val gradeName = record.gradeName ?: record.gradeId?.let { id -> gradeOptions.find { it.id == id }?.name } ?: "-"

    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = {}, onLongClick = onLongClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Status Avatar
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
            
            // Detail Siswa
            Column(modifier = Modifier.weight(1f)) {
                Text(record.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text("ID: ${record.studentId} • $className", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            
            // Jam Absen
            Text(
                text = record.timestamp.format(DateTimeFormatter.ofPattern("HH:mm\ndd MMM")), 
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(start = 8.dp)
            )
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
    // Cari siswa berdasarkan nama kalau sedang edit, atau null kalau baru
    var selectedStudent by remember { mutableStateOf(allStudents.find { it.name == existingRecord?.name }) }
    var status by remember { mutableStateOf(existingRecord?.status ?: "PRESENT") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingRecord == null) "Absen Manual" else "Edit Absensi") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (existingRecord == null) {
                    FilterDropdown(
                        label = "Pilih Murid", 
                        options = allStudents, 
                        selected = selectedStudent, 
                        onSelected = { selectedStudent = it }, 
                        getLabel = { "${it.name} (${it.className})" }
                    )
                } else {
                    // Kalau Edit, tampilkan nama saja (tidak bisa ganti orang)
                    OutlinedTextField(
                        value = existingRecord.name, 
                        onValueChange = {}, 
                        label = { Text("Nama Siswa") }, 
                        readOnly = true, 
                        enabled = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text("Status Kehadiran:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("PRESENT", "SAKIT", "IZIN", "ALPHA").forEach { opt ->
                        FilterChip(
                            selected = status == opt,
                            onClick = { status = opt },
                            label = { Text(opt.take(1)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = getAttendanceStatusColor(opt).copy(alpha = 0.2f),
                                selectedLabelColor = getAttendanceStatusColor(opt)
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedStudent == null && existingRecord == null) {
                        Toast.makeText(context, "Pilih siswa terlebih dahulu!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // ✅ FIX PENTING: Memasukkan studentId dari FaceEntity agar tidak null/error
                    val record = existingRecord?.copy(status = status) ?: CheckInRecord(
                        studentId = selectedStudent!!.studentId, // Ambil NIK/NIS dari data siswa
                        name = selectedStudent!!.name,
                        timestamp = LocalDateTime.now(),
                        faceId = selectedStudent!!.id, // ID Database Lokal
                        status = status,
                        classId = selectedStudent?.classId,
                        gradeId = selectedStudent?.gradeId,
                        className = selectedStudent?.className,
                        gradeName = selectedStudent?.grade
                    )
                    onSave(record)
                },
                enabled = (selectedStudent != null || existingRecord != null)
            ) { Text("Simpan") }
        },
        dismissButton = {
            Row {
                if (existingRecord != null) {
                    TextButton(onClick = { onDelete(existingRecord) }) { Text("Hapus", color = Color.Red) }
                }
                TextButton(onClick = onDismiss) { Text("Batal") }
            }
        }
    )
}

// --- FILTER COMPONENTS ---

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
    
    if (showEndPicker) {
        val dateState = rememberDatePickerState()
        DatePickerDialog(onDismissRequest = { showEndPicker = false }, confirmButton = {
            TextButton(onClick = {
                dateState.selectedDateMillis?.let { onEndDateChange(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()) }
                showEndPicker = false
            }) { Text("OK") }
        }) { DatePicker(state = dateState) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showStartPicker = true }, modifier = Modifier.weight(1f)) { 
                Icon(Icons.Default.DateRange, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(startDate?.format(DateTimeFormatter.ofPattern("dd MMM")) ?: "Mulai") 
            }
            OutlinedButton(onClick = { showEndPicker = true }, modifier = Modifier.weight(1f)) { 
                Icon(Icons.Default.DateRange, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(endDate?.format(DateTimeFormatter.ofPattern("dd MMM")) ?: "Selesai") 
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                FilterDropdown("Kelas", classOptions, selectedClass, onClassSelected, { it.name })
            }
            Box(modifier = Modifier.weight(1f)) {
                FilterDropdown("Grade", gradeOptions, selectedGrade, onGradeSelected, { it.name })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> FilterDropdown(label: String, options: List<T>, selected: T?, onSelected: (T?) -> Unit, getLabel: (T) -> String) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = selected?.let { getLabel(it) } ?: "", 
            onValueChange = {},
            label = { Text(label, fontSize = 12.sp) }, 
            readOnly = true,
            trailingIcon = { IconButton(onClick = { expanded = !expanded }) { Icon(Icons.Default.ArrowDropDown, null) } },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Semua") }, onClick = { onSelected(null); expanded = false })
            options.forEach { opt -> 
                DropdownMenuItem(text = { Text(getLabel(opt)) }, onClick = { onSelected(opt); expanded = false }) 
            }
        }
    }
}