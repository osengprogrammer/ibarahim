package com.example.crashcourse.ui.face

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.crashcourse.db.FaceEntity
import com.example.crashcourse.db.MasterClassWithNames
import com.example.crashcourse.ui.components.*
import com.example.crashcourse.ui.theme.AzuraPrimary
import com.example.crashcourse.viewmodel.*
import java.time.LocalDate

/**
 * 🏛️ Azura Tech Database Personel (Face List)
 * Layar pusat manajemen biometrik siswa/personel.
 * Bebas dari ketergantungan paket Management Account.
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
    val faces by faceVM.faceList.collectAsStateWithLifecycle()
    val syncState by syncVM.syncState.collectAsStateWithLifecycle()
    val masterClasses by masterClassVM.masterClassesWithNames.collectAsStateWithLifecycle(initialValue = emptyList())

    // --- 🔍 FILTER STATES ---
    var selectedUnit by remember { mutableStateOf<MasterClassWithNames?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf<LocalDate?>(null) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }

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
                title = { Text("Database Personel", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                    }
                },
                actions = {
                    if (syncState is SyncState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = AzuraPrimary)
                        Spacer(Modifier.width(16.dp))
                    } else {
                        IconButton(onClick = { syncVM.syncStudentsDown() }) {
                            Icon(Icons.Default.CloudSync, "Sync Data", tint = AzuraPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
        ) {
            // --- 🛠️ SECTION: SEARCH & FILTER ---
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AzuraInput(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = "Cari Nama atau ID",
                        leadingIcon = Icons.Default.Search
                    )

                    AzuraDropdown(
                        label = "Unit / Kelas",
                        options = masterClasses,
                        selected = selectedUnit,
                        onSelected = { selectedUnit = it },
                        itemLabel = { it.className }
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AzuraDatePicker(
                            label = "Mulai",
                            selectedDate = startDate,
                            onDateSelected = { startDate = it },
                            modifier = Modifier.weight(1f)
                        )

                        AzuraDatePicker(
                            label = "Selesai",
                            selectedDate = endDate,
                            onDateSelected = { endDate = it },
                            modifier = Modifier.weight(1f),
                            minDate = startDate,
                            maxDate = startDate?.plusDays(30) 
                        )
                    }

                    TextButton(
                        onClick = { 
                            selectedUnit = null
                            startDate = null
                            endDate = null
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

            // --- 📑 SECTION: DATA LIST ---
            Box(modifier = Modifier.weight(1f)) {
                if (filteredFaces.isEmpty()) {
                    EmptyFacesView(isFiltering = selectedUnit != null || searchQuery.isNotEmpty())
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 80.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredFaces, key = { it.studentId }) { face ->
                            StudentListItem(
                                face = face,
                                onEdit = { onNavigateToEdit(face.studentId) },
                                onDelete = { faceVM.deleteFace(face) }
                            )
                        }
                    }
                }

                // 🚀 FIX: Sinkronisasi Status (Penyebab Compile Error sebelumnya)
                // Kita pindahkan ke Box scope yang lebih aman dan menggunakan AnimatedVisibility standar
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp), 
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

/**
 * 🎨 KOMPONEN MANDIRI: StudentListItem
 */
@Composable
fun StudentListItem(
    face: FaceEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = face.photoUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = face.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ID: ${face.studentId} • ${face.className}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Edit", tint = AzuraPrimary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Hapus", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun SyncStatusCard(state: SyncState, onDismiss: () -> Unit) {
    val color = when (state) {
        is SyncState.Success -> Color(0xFF4CAF50)
        is SyncState.Error -> Color(0xFFE53935)
        else -> AzuraPrimary
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
            
            Text(text = message, color = Color.White, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.width(12.dp))
            if (state !is SyncState.Loading) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

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
            text = if (isFiltering) "Pencarian tidak ditemukan" else "Database personel kosong",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )
    }
}