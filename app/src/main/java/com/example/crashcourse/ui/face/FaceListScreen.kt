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
import com.example.crashcourse.ui.SyncState
import com.example.crashcourse.ui.components.*
import com.example.crashcourse.ui.theme.AzuraPrimary
import com.example.crashcourse.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    faceVM: FaceViewModel = viewModel(),
    syncVM: SyncViewModel = viewModel(),
    masterClassVM: MasterClassViewModel = viewModel()
) {
    // 🔥 DATA DARI VIEWMODEL (Reactive & Scoped)
    val faces by faceVM.filteredFaces.collectAsStateWithLifecycle()
    val syncState by syncVM.syncState.collectAsStateWithLifecycle()
    val masterClasses by masterClassVM.masterClassesWithNames.collectAsStateWithLifecycle(initialValue = emptyList())
    
    // 🔥 FILTER STATES (Dihubungkan ke ViewModel)
    val searchQuery by faceVM.searchQuery.collectAsStateWithLifecycle()
    val selectedUnit by faceVM.selectedUnit.collectAsStateWithLifecycle()

    // 🚀 AUTO-SYNC SAAT LAYAR DIBUKA
    LaunchedEffect(Unit) {
        syncVM.syncStudentsDown()
    }

    Scaffold(
        topBar = {
            FaceTopBar(
                syncState = syncState,
                onBack = onNavigateBack,
                onSync = { syncVM.syncStudentsDown() }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
        ) {
            // --- 🛠️ COMPONENT: FILTER CARD ---
            FaceFilterSection(
                searchQuery = searchQuery,
                selectedUnit = selectedUnit,
                masterClasses = masterClasses,
                onQueryChange = { faceVM.updateSearchQuery(it) },
                onUnitSelected = { faceVM.updateSelectedUnit(it) },
                onReset = { faceVM.resetFilters() }
            )

            // --- 📑 COMPONENT: LIST CONTENT ---
            FaceListContent(
                faces = faces,
                isFiltering = searchQuery.isNotEmpty() || selectedUnit != null,
                onEdit = onNavigateToEdit,
                onDelete = { faceVM.deleteFace(it) }
            )
        }

        // Overlay Sync Status (Floating)
        SyncOverlay(
            syncState = syncState,
            onDismiss = { syncVM.resetState() }
        )
    }
}

// ==========================================
// 🛠️ SUB-COMPONENTS (Pecahan UI)
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceTopBar(syncState: SyncState, onBack: () -> Unit, onSync: () -> Unit) {
    TopAppBar(
        title = { Text("Database Personel", fontWeight = FontWeight.Bold) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
            }
        },
        actions = {
            if (syncState is SyncState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = AzuraPrimary)
                Spacer(Modifier.width(16.dp))
            } else {
                IconButton(onClick = onSync) {
                    Icon(Icons.Default.CloudSync, "Sync Data", tint = AzuraPrimary)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
    )
}

@Composable
fun FaceFilterSection(
    searchQuery: String,
    selectedUnit: MasterClassWithNames?,
    masterClasses: List<MasterClassWithNames>,
    onQueryChange: (String) -> Unit,
    onUnitSelected: (MasterClassWithNames?) -> Unit,
    onReset: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AzuraInput(
                value = searchQuery,
                onValueChange = onQueryChange,
                label = "Cari Nama atau ID",
                leadingIcon = Icons.Default.Search
            )

            AzuraDropdown(
                label = "Unit / Kelas",
                options = masterClasses,
                selected = selectedUnit?.className ?: "",
                onSelected = { onUnitSelected(it as? MasterClassWithNames) },
                itemLabel = { (it as? MasterClassWithNames)?.className ?: "" }
            )

            TextButton(
                onClick = onReset,
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Default.FilterAltOff, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Reset Filter")
            }
        }
    }
}

@Composable
fun ColumnScope.FaceListContent(
    faces: List<FaceEntity>,
    isFiltering: Boolean,
    onEdit: (String) -> Unit,
    onDelete: (FaceEntity) -> Unit
) {
    Box(modifier = Modifier.weight(1f)) {
        if (faces.isEmpty()) {
            EmptyFacesView(isFiltering = isFiltering)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(faces, key = { it.studentId }) { face ->
                    StudentListItem(
                        face = face,
                        onEdit = { onEdit(face.studentId) },
                        onDelete = { onDelete(face) }
                    )
                }
            }
        }
    }
}

@Composable
fun StudentListItem(face: FaceEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = face.photoUrl,
                contentDescription = null,
                modifier = Modifier.size(50.dp).clip(CircleShape).background(Color.LightGray),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = face.name, fontWeight = FontWeight.Bold)
                Text(text = "ID: ${face.studentId} • ${face.className}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Row {
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, null, tint = AzuraPrimary) }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
fun SyncOverlay(syncState: SyncState, onDismiss: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(
            visible = syncState !is SyncState.Idle,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it }
        ) {
            SyncStatusCard(state = syncState, onDismiss = onDismiss)
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

    Surface(color = color, shape = RoundedCornerShape(12.dp), shadowElevation = 8.dp) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            val message = when (state) {
                is SyncState.Loading -> state.message
                is SyncState.Success -> state.message
                is SyncState.Error -> state.message
                else -> ""
            }
            Text(text = message, color = Color.White)
            if (state !is SyncState.Loading) {
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = Color.White) }
            }
        }
    }
}

@Composable
fun EmptyFacesView(isFiltering: Boolean) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = if (isFiltering) Icons.Default.SearchOff else Icons.Default.PeopleOutline, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color.LightGray)
        Spacer(Modifier.height(16.dp))
        Text(text = if (isFiltering) "Pencarian tidak ditemukan" else "Database personel kosong", color = Color.Gray)
    }
}