package com.example.crashcourse.ui.checkin

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.crashcourse.db.CheckInRecord
import com.example.crashcourse.viewmodel.AuthState
import com.example.crashcourse.viewmodel.CheckInViewModel
import java.time.format.DateTimeFormatter
import java.time.LocalDate
import androidx.compose.foundation.shape.RoundedCornerShape


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInRecordScreen(
    authState: AuthState.Active,
    viewModel: CheckInViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- STATE UI ---
    var searchQuery by remember { mutableStateOf("") }
    var showFilterDialog by remember { mutableStateOf(false) }
    
    // State CRUD
    var selectedRecordForEdit by remember { mutableStateOf<CheckInRecord?>(null) }
    var recordToDelete by remember { mutableStateOf<CheckInRecord?>(null) }

    // State Filter
    var startDateStr by remember { mutableStateOf(LocalDate.now().toString()) }
    var endDateStr by remember { mutableStateOf(LocalDate.now().toString()) }
    var selectedClass by remember { mutableStateOf("Semua Kelas") }

    val records by viewModel.getScopedCheckIns(
        role = authState.role,
        assignedClasses = authState.assignedClasses,
        nameFilter = searchQuery,
        startDateStr = startDateStr,
        endDateStr = endDateStr,
        className = if (selectedClass == "Semua Kelas") null else selectedClass
    ).collectAsStateWithLifecycle(initialValue = emptyList())

    val isLoading by viewModel.isLoadingHistory.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Riwayat Absensi", fontWeight = FontWeight.Bold)
                        Text(
                            text = if(authState.role == "ADMIN") "Akses Admin" else "Akses Guru",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                }
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Cari Siswa...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(16.dp))
            
            if (isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(records, key = { it.firestoreId ?: it.id.toString() }) { record ->
                    AttendanceCard(
                        record = record,
                        canEdit = authState.role == "ADMIN", // Hanya Admin yang bisa CRUD
                        onEdit = { selectedRecordForEdit = record },
                        onDelete = { recordToDelete = record }
                    )
                }
            }
        }
    }

    // ==========================================
    // 1. DIALOG EDIT (UPDATE)
    // ==========================================
    if (selectedRecordForEdit != null) {
        var newStatus by remember { mutableStateOf(selectedRecordForEdit!!.status) }
        val statuses = listOf("PRESENT", "LATE", "SICK", "PERMIT", "ALPHA")

        AlertDialog(
            onDismissRequest = { selectedRecordForEdit = null },
            title = { Text("Edit Status Absensi") },
            text = {
                Column {
                    Text("Siswa: ${selectedRecordForEdit!!.name}", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Pilih Status Baru:")
                    statuses.forEach { status ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = (newStatus == status),
                                onClick = { newStatus = status }
                            )
                            Text(status, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateCheckInStatus(selectedRecordForEdit!!, newStatus)
                    selectedRecordForEdit = null
                    Toast.makeText(context, "Status diperbarui", Toast.LENGTH_SHORT).show()
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { selectedRecordForEdit = null }) { Text("Batal") }
            }
        )
    }

    // ==========================================
    // 2. DIALOG KONFIRMASI HAPUS (DELETE)
    // ==========================================
    if (recordToDelete != null) {
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = { Text("Hapus Riwayat") },
            text = { Text("Apakah Anda yakin ingin menghapus data absensi ${recordToDelete!!.name}?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCheckInRecord(recordToDelete!!)
                        recordToDelete = null
                        Toast.makeText(context, "Data dihapus", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Hapus") }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) { Text("Batal") }
            }
        )
    }

    // ... (Filter Dialog tetap sama atau Brother bisa tambahkan DatePicker) ...
}

@Composable
fun AttendanceCard(
    record: CheckInRecord,
    canEdit: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = record.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = "${record.className} • ${record.timestamp.format(DateTimeFormatter.ofPattern("dd MMM HH:mm"))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Status Badge
                Surface(
                    color = when(record.status) {
                        "PRESENT" -> Color(0xFFE8F5E9)
                        "SICK", "PERMIT" -> Color(0xFFE3F2FD)
                        else -> Color(0xFFFFEBEE)
                    },
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = record.status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = when(record.status) {
                            "PRESENT" -> Color(0xFF2E7D32)
                            "SICK", "PERMIT" -> Color(0xFF1976D2)
                            else -> Color(0xFFC62828)
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            if (canEdit) {
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}