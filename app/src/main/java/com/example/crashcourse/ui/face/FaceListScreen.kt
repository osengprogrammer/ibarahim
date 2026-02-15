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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.crashcourse.db.FaceEntity
import com.example.crashcourse.db.MasterClassWithNames
import com.example.crashcourse.ui.SyncState
import com.example.crashcourse.ui.components.*
import com.example.crashcourse.ui.theme.AzuraPrimary
import com.example.crashcourse.viewmodel.FaceViewModel
import com.example.crashcourse.viewmodel.SyncViewModel
import com.example.crashcourse.viewmodel.MasterClassViewModel

/**
 * 📑 FaceListScreen (V.10.30 - Build Success)
 * Menggunakan nama parameter 'faceVM' agar sinkron dengan NavGraph.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    // 🔥 PENTING: Nama parameter ini harus 'faceVM' agar sesuai panggilan di NavGraph
    faceVM: FaceViewModel = viewModel(),
    syncVM: SyncViewModel = viewModel(),
    masterClassVM: MasterClassViewModel = viewModel()
) {
    // 📊 OBSERVATION
    val faces by faceVM.filteredFaces.collectAsStateWithLifecycle()
    val syncState by syncVM.syncState.collectAsStateWithLifecycle()
    val masterClasses by masterClassVM.masterClassesWithNames.collectAsStateWithLifecycle(initialValue = emptyList())
    
    val searchQuery by faceVM.searchQuery.collectAsStateWithLifecycle()
    val selectedUnit by faceVM.selectedUnit.collectAsStateWithLifecycle()

    // 🚀 Trigger Sync saat layar dibuka
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
            FaceFilterSection(
                searchQuery = searchQuery,
                selectedUnit = selectedUnit,
                masterClasses = masterClasses,
                onQueryChange = { faceVM.updateSearchQuery(it) },
                onUnitSelected = { faceVM.updateSelectedUnit(it) },
                onReset = { faceVM.resetFilters() }
            )

            FaceListContent(
                faces = faces,
                isFiltering = searchQuery.isNotEmpty() || selectedUnit != null,
                onEdit = onNavigateToEdit,
                onDelete = { faceVM.deleteFace(it) }
            )
        }

        SyncOverlay(
            syncState = syncState,
            onDismiss = { syncVM.resetState() }
        )
    }
}

// ==========================================
// 🛠️ SUB-COMPONENTS
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
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
                label = "Filter Berdasarkan Unit",
                options = masterClasses,
                selected = selectedUnit,
                onSelected = { onUnitSelected(it as? MasterClassWithNames) },
                itemLabel = { (it as? MasterClassWithNames)?.className ?: "" }
            )

            TextButton(
                onClick = onReset,
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
            ) {
                Icon(Icons.Default.FilterAltOff, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Hapus Filter")
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
                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 80.dp),
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
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = face.photoUrl,
                contentDescription = null,
                modifier = Modifier.size(50.dp).clip(CircleShape).background(Color(0xFFEEEEEE)),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(text = face.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                Text(text = "ID: ${face.studentId}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            
            Row {
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit", tint = AzuraPrimary, modifier = Modifier.size(20.dp)) }
                IconButton(onClick = onDelete) { Icon(Icons.Default.DeleteSweep, "Hapus", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp)) }
            }
        }
    }
}

@Composable
fun SyncOverlay(syncState: SyncState, onDismiss: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(bottom = 32.dp), contentAlignment = Alignment.BottomCenter) {
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
    val (containerColor, message) = when (state) {
        is SyncState.Loading -> Color(0xFF333333) to state.message
        is SyncState.Success -> Color(0xFF4CAF50) to state.message
        is SyncState.Error -> Color(0xFFE53935) to state.message
        SyncState.Idle -> Color.Transparent to ""
    }

    Surface(color = containerColor, shape = RoundedCornerShape(24.dp), modifier = Modifier.padding(horizontal = 24.dp)) {
        Row(modifier = Modifier.padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = message, color = Color.White, style = MaterialTheme.typography.bodyMedium)
            if (state !is SyncState.Loading) {
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
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