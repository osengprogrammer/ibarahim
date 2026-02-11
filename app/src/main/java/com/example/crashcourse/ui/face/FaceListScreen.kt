package com.example.crashcourse.ui.face

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.crashcourse.db.MasterClassWithNames
import com.example.crashcourse.ui.components.* import com.example.crashcourse.ui.management.UserListItem
import com.example.crashcourse.ui.theme.AzuraPrimary
import com.example.crashcourse.viewmodel.*
import java.time.LocalDate

/**
 * 👥 Azura Tech Face List Screen
 * Screen manajemen personel dengan filter Unit (6-Pilar) dan Sinkronisasi Cerdas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    faceVM: FaceViewModel = viewModel(),
    syncVM: SyncViewModel = viewModel(),
    masterClassVM: MasterClassViewModel = viewModel()
) {
    // --- 📊 DATA OBSERVATION ---
    val faces by faceVM.faceList.collectAsStateWithLifecycle()
    val syncState by syncVM.syncState.collectAsStateWithLifecycle()
    val masterClasses by masterClassVM.masterClassesWithNames.collectAsStateWithLifecycle(initialValue = emptyList())

    // --- 🔍 FILTER STATES ---
    var selectedUnit by remember { mutableStateOf<MasterClassWithNames?>(null) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // --- 🧠 FILTER LOGIC ---
    val filteredFaces = remember(faces, selectedUnit, searchQuery) {
        faces.filter { face ->
            val matchUnit = selectedUnit == null || face.className == selectedUnit?.className
            val matchSearch = searchQuery.isEmpty() || 
                             face.name.contains(searchQuery, ignoreCase = true) || 
                             face.studentId.contains(searchQuery)
            matchUnit && matchSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("Manajemen Personel", fontWeight = FontWeight.Bold) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                    }
                },
                actions = {
                    // 🔄 TOMBOL SYNC
                    if (syncState is SyncState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = AzuraPrimary
                        )
                        Spacer(Modifier.width(16.dp))
                    } else {
                        IconButton(onClick = { syncVM.syncStudentsDown() }) {
                            Icon(
                                imageVector = Icons.Default.CloudSync, 
                                contentDescription = "Sync Data", 
                                tint = AzuraPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF5F5F5)) // Background abu-abu muda khas Azura
        ) {
            // --- 🛠️ SECTION: FILTER CARD ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp), 
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Input Cari (Komponen Azura)
                    AzuraInput(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = "Cari Nama atau ID",
                        leadingIcon = Icons.Default.Search
                    )

                    // Dropdown Unit (Komponen Azura)
                    AzuraDropdown(
                        label = "Unit / Kelas",
                        options = masterClasses,
                        selected = selectedUnit,
                        onSelected = { selectedUnit = it },
                        itemLabel = { it.className }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(), 
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Date Picker (Komponen Azura)
                        AzuraDatePicker(
                            label = "Pilih Tanggal",
                            selectedDate = selectedDate,
                            onDateSelected = { selectedDate = it },
                            modifier = Modifier.weight(1f)
                        )

                        // Tombol Reset Filter
                        Surface(
                            onClick = { 
                                selectedUnit = null
                                selectedDate = null
                                searchQuery = "" 
                            },
                            color = Color(0xFFEEEEEE),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FilterAltOff, 
                                    contentDescription = "Reset", 
                                    tint = Color.DarkGray
                                )
                            }
                        }
                    }
                }
            }

            // --- 📑 DATA SECTION ---
            Box(modifier = Modifier.weight(1f)) {
                // List Personel
                if (filteredFaces.isEmpty()) {
                    EmptyFacesView(isFiltering = selectedUnit != null || searchQuery.isNotEmpty())
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredFaces, key = { it.studentId }) { face ->
                            UserListItem(
                                face = face,
                                onEdit = { onNavigateToEdit(face.studentId) },
                                onDelete = { faceVM.deleteFace(face) }
                            )
                        }
                    }
                }

                // 🚀 SYNC STATUS OVERLAY
                // Diletakkan dalam BoxScope agar bisa align BottomCenter
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp), 
                    contentAlignment = Alignment.BottomCenter
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = syncState !is SyncState.Idle,
                        enter = fadeIn() + slideInVertically { it },
                        exit = fadeOut() + slideOutVertically { it }
                    ) {
                        SyncStatusCard(
                            state = syncState, 
                            onDismiss = { syncVM.resetState() }
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 🎨 HELPER COMPONENT: SYNC STATUS CARD
// ==========================================
@Composable
fun SyncStatusCard(state: SyncState, onDismiss: () -> Unit) {
    val color = when (state) {
        is SyncState.Success -> Color(0xFF4CAF50) // Sukses = Hijau
        is SyncState.Error -> Color(0xFFE53935)   // Error = Merah
        else -> AzuraPrimary                      // Loading = Biru Azura
    }

    Surface(
        color = color,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val message = when (state) {
                is SyncState.Loading -> state.message
                is SyncState.Success -> state.message
                is SyncState.Error -> state.message
                else -> ""
            }
            
            Text(
                text = message,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(Modifier.width(12.dp))
            
            if (state !is SyncState.Loading) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close, 
                        contentDescription = "Tutup", 
                        tint = Color.White, 
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// 🎨 HELPER COMPONENT: EMPTY VIEW
// ==========================================
@Composable
fun EmptyFacesView(isFiltering: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (isFiltering) Icons.Default.SearchOff else Icons.Default.PeopleOutline,
            contentDescription = null, 
            modifier = Modifier.size(80.dp), 
            tint = Color.LightGray
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (isFiltering) "Pencarian tidak ditemukan" else "Daftar personel masih kosong",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )
    }
}