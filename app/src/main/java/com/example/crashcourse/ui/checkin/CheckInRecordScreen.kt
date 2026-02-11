package com.example.crashcourse.ui.checkin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.crashcourse.db.MasterClassWithNames
import com.example.crashcourse.ui.components.*
import com.example.crashcourse.viewmodel.AuthState
import com.example.crashcourse.viewmodel.CheckInViewModel
import com.example.crashcourse.viewmodel.MasterClassViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInRecordScreen(
    authState: AuthState.Active,
    onNavigateBack: () -> Unit,
    checkInVM: CheckInViewModel = viewModel(),
    masterClassVM: MasterClassViewModel = viewModel() // 🚀 Tambahkan VM MasterClass
) {
    // --- 📊 DATA OBSERVATION ---
    val masterClasses by masterClassVM.masterClassesWithNames.collectAsStateWithLifecycle(initialValue = emptyList())
    
    // --- 🔍 FILTER STATES ---
    var selectedUnit by remember { mutableStateOf<MasterClassWithNames?>(null) }
    var startDate by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }
    var endDate by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }
    var searchQuery by remember { mutableStateOf("") }

    // Formatter untuk ViewModel
    val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // Reactive Data Collection berdasarkan filter
    val records by checkInVM.getScopedCheckIns(
        role = authState.role,
        assignedClasses = authState.assignedClasses,
        nameFilter = searchQuery,
        startDateStr = startDate?.format(dtf) ?: "",
        endDateStr = endDate?.format(dtf) ?: "",
        className = selectedUnit?.className
    ).collectAsStateWithLifecycle(initialValue = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Riwayat Absensi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
        ) {
            // --- 🛠️ SECTION: FILTER CARD ---
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    
                    // 1. Dropdown Unit (Rombel)
                    AzuraDropdown(
                        label = "Pilih Unit / Kelas",
                        options = masterClasses,
                        selected = selectedUnit,
                        onSelected = { selectedUnit = it },
                        itemLabel = { it.className }
                    )

                    // 2. Input Cari Nama
                    AzuraInput(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = "Cari Nama Siswa",
                        leadingIcon = Icons.Default.Search
                    )

                    // 3. Filter Tanggal (Maks 1 Bulan)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AzuraDatePicker(
                            label = "Mulai",
                            selectedDate = startDate,
                            onDateSelected = { 
                                startDate = it
                                // Reset endDate jika startDate melampaui rentang 1 bulan yang sudah ada
                                if (it != null && endDate != null && endDate!!.isAfter(it.plusMonths(1))) {
                                    endDate = it.plusMonths(1)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            maxDate = endDate // Tidak boleh melebihi tanggal selesai
                        )

                        AzuraDatePicker(
                            label = "Selesai",
                            selectedDate = endDate,
                            onDateSelected = { endDate = it },
                            modifier = Modifier.weight(1f),
                            minDate = startDate, // Tidak boleh kurang dari tanggal mulai
                            maxDate = startDate?.plusMonths(1) // 🚀 LIMIT: MAKSIMAL 1 BULAN
                        )
                    }

                    // Button Reset
                    TextButton(
                        onClick = {
                            selectedUnit = null
                            startDate = LocalDate.now()
                            endDate = LocalDate.now()
                            searchQuery = ""
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.FilterAltOff, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Reset Filter")
                    }
                }
            }

            // --- 📑 LIST DATA ---
            if (records.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Data tidak ditemukan.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(records, key = { "${it.studentId}_${it.timestamp}" }) { record ->
                        CheckInRecordCard(
                            record = record,
                            onLongClick = { checkInVM.deleteCheckInRecord(record) }
                        )
                    }
                }
            }
        }
    }
}