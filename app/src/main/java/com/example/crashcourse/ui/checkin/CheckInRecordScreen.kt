package com.example.crashcourse.ui.checkin

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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.crashcourse.db.CheckInRecord
import com.example.crashcourse.db.MasterClassWithNames
import com.example.crashcourse.ui.components.*
import com.example.crashcourse.ui.theme.AzuraPrimary
import com.example.crashcourse.viewmodel.AuthState
import com.example.crashcourse.viewmodel.CheckInViewModel
import com.example.crashcourse.viewmodel.MasterClassViewModel
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 📊 Azura Tech Attendance History Screen (V.20.2)
 * Update: Fix parameter 'schoolId' pada Manual Check-In.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInRecordScreen(
    authState: AuthState.Active,
    onNavigateBack: () -> Unit,
    checkInVM: CheckInViewModel = viewModel(),
    masterClassVM: MasterClassViewModel = viewModel()
) {
    val context = LocalContext.current
    val isLoading by checkInVM.isLoadingHistory.collectAsStateWithLifecycle()
    val masterClasses by masterClassVM.masterClassesWithNames.collectAsStateWithLifecycle(initialValue = emptyList())
    
    val isAdmin = authState.role == "ADMIN" || authState.role == "SUPERVISOR"

    var selectedUnit by remember { mutableStateOf<MasterClassWithNames?>(null) }
    var startDate by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }
    var endDate by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }
    var searchQuery by remember { mutableStateOf("") }

    var showManualDialog by remember { mutableStateOf(false) }
    var recordToEdit by remember { mutableStateOf<CheckInRecord?>(null) }
    var recordToDelete by remember { mutableStateOf<CheckInRecord?>(null) }

    val records by checkInVM.getScopedCheckIns(
        role = authState.role,
        assignedClasses = authState.assignedClasses,
        nameFilter = searchQuery,
        startDateStr = startDate?.toString() ?: "",
        endDateStr = endDate?.toString() ?: "",
        className = selectedUnit?.className
    ).collectAsStateWithLifecycle(initialValue = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Riwayat Absensi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        if (startDate != null && endDate != null) {
                            checkInVM.fetchHistoricalData(startDate!!, endDate!!, selectedUnit?.className)
                        } else {
                            Toast.makeText(context, "Pilih rentang tanggal", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = "Sinkronisasi Cloud")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(
                    onClick = { showManualDialog = true },
                    containerColor = AzuraPrimary,
                    contentColor = Color.White
                ) { Icon(Icons.Default.Add, contentDescription = "Input Manual") }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(Color(0xFFF5F5F5))) {
            
            // UI Filter Card (Search, Class, Date)
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AzuraInput(
                        value = searchQuery, 
                        onValueChange = { searchQuery = it }, 
                        label = "Cari Nama Siswa", 
                        leadingIcon = Icons.Default.Search
                    )
                    
                    AzuraDropdown(
                        label = "Filter Mata Kuliah", 
                        options = masterClasses, 
                        selected = selectedUnit, 
                        onSelected = { selectedUnit = it }, 
                        itemLabel = { it.className }
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AzuraDatePicker(label = "Mulai", selectedDate = startDate, onDateSelected = { startDate = it }, modifier = Modifier.weight(1f))
                        AzuraDatePicker(label = "Selesai", selectedDate = endDate, onDateSelected = { endDate = it }, modifier = Modifier.weight(1f))
                    }
                }
            }

            if (records.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Inbox, null, Modifier.size(64.dp), Color.LightGray)
                        Spacer(Modifier.height(8.dp))
                        Text("Tidak ada data ditemukan", color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(records, key = { "${it.studentId}_${it.timestamp}" }) { record ->
                        CheckInRecordCard(
                            record = record,
                            isAdmin = isAdmin,
                            onClick = { recordToEdit = record },
                            onDelete = { recordToDelete = record }
                        )
                    }
                }
            }
        }

        // --- SECTION: DIALOGS ---

        if (showManualDialog) {
            ManualCheckInDialog(
                masterClasses = masterClasses,
                onDismiss = { showManualDialog = false },
                onConfirm = { name, sid, unit, status ->
                    // 🔥 FIXED: Sekarang menyertakan schoolId dari authState
                    val newRecord = CheckInRecord(
                        studentId = sid,
                        name = name,
                        schoolId = authState.schoolId, // Parameter krusial yang tadinya hilang
                        timestamp = LocalDateTime.now(),
                        status = status,
                        verified = true,
                        className = unit.className,
                        gradeName = unit.gradeName ?: "",
                        role = unit.roleName ?: "STUDENT",
                        syncStatus = "PENDING"
                    )
                    
                    checkInVM.saveCheckIn(newRecord, 0.0f) 
                    
                    showManualDialog = false
                    Toast.makeText(context, "Berhasil simpan log manual", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Edit Status & Delete Dialog (Tetap sama)
        recordToEdit?.let { record ->
            EditStatusDialog(
                currentStatus = record.status,
                onDismiss = { recordToEdit = null },
                onConfirm = { newStatus ->
                    checkInVM.updateCheckInStatus(record, newStatus)
                    recordToEdit = null
                }
            )
        }

        recordToDelete?.let { record ->
            AlertDialog(
                onDismissRequest = { recordToDelete = null },
                title = { Text("Konfirmasi Hapus") },
                text = { Text("Apakah Anda yakin ingin menghapus log ${record.name}?") },
                confirmButton = {
                    Button(
                        onClick = {
                            checkInVM.deleteCheckInRecord(record)
                            recordToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) { Text("Hapus", color = Color.White) }
                },
                dismissButton = {
                    TextButton(onClick = { recordToDelete = null }) { Text("Batal") }
                },
                shape = RoundedCornerShape(24.dp)
            )
        }
    }
}

// ... ManualCheckInDialog dan EditStatusDialog tetap sama seperti sebelumnya ...
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualCheckInDialog(
    masterClasses: List<MasterClassWithNames>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, sid: String, unit: MasterClassWithNames, status: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var sid by remember { mutableStateOf("") }
    var selectedUnit by remember { mutableStateOf<MasterClassWithNames?>(null) }
    var selectedStatus by remember { mutableStateOf("PRESENT") }
    val statuses = listOf("PRESENT", "SAKIT", "IZIN", "ALPHA")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Input Kehadiran Manual", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AzuraInput(value = name, onValueChange = { name = it }, label = "Nama Lengkap")
                AzuraInput(value = sid, onValueChange = { sid = it }, label = "NIM / ID Siswa")
                
                AzuraDropdown(
                    label = "Pilih Mata Kuliah", 
                    options = masterClasses, 
                    selected = selectedUnit, 
                    onSelected = { selectedUnit = it }, 
                    itemLabel = { it.className }
                )
                
                Text("Status Kehadiran:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    statuses.forEach { status ->
                        FilterChip(
                            selected = selectedStatus == status,
                            onClick = { selectedStatus = status },
                            label = { Text(status, fontSize = 10.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && sid.isNotBlank() && selectedUnit != null, 
                onClick = { onConfirm(name, sid, selectedUnit!!, selectedStatus) }
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
fun EditStatusDialog(
    currentStatus: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val statuses = listOf("PRESENT", "SAKIT", "IZIN", "ALPHA")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ubah Status") },
        text = {
            Column {
                statuses.forEach { status ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onConfirm(status) }
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = status == currentStatus, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = status, 
                            fontWeight = if (status == currentStatus) FontWeight.Bold else FontWeight.Normal,
                            color = if (status == currentStatus) AzuraPrimary else Color.Unspecified
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Tutup") } },
        shape = RoundedCornerShape(28.dp)
    )
}