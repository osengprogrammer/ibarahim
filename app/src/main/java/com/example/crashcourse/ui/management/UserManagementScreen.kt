package com.example.crashcourse.ui.management // 🚀 FIXED PACKAGE

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.crashcourse.db.FaceEntity
import com.example.crashcourse.ui.theme.AzuraPrimary
import com.example.crashcourse.utils.showToast
import com.example.crashcourse.viewmodel.FaceViewModel

/**
 * 👥 Azura Tech User Management Screen
 * Provides a searchable list of students for the Admin.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    onBack: () -> Unit,
    onEditUser: (String) -> Unit,
    faceViewModel: FaceViewModel = viewModel()
) {
    val context = LocalContext.current
    val faces by faceViewModel.faceList.collectAsStateWithLifecycle()
    
    // Search & Filter State
    var searchQuery by remember { mutableStateOf("") }
    val filteredFaces = remember(faces, searchQuery) {
        if (searchQuery.isBlank()) faces
        else faces.filter { 
            it.name.contains(searchQuery, ignoreCase = true) || 
            it.studentId.contains(searchQuery) ||
            it.className.contains(searchQuery, ignoreCase = true)
        }
    }

    // Delete Confirmation State
    var faceToDelete by remember { mutableStateOf<FaceEntity?>(null) }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer)) {
                TopAppBar(
                    title = { Text("Manajemen Siswa", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
                // --- SEARCH BAR ---
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari Nama, ID, atau Kelas...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, null)
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (filteredFaces.isEmpty()) {
                EmptyStateView(isSearching = searchQuery.isNotEmpty())
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredFaces, key = { it.studentId }) { face ->
                        UserListItem(
                            face = face,
                            onEdit = { onEditUser(face.studentId) },
                            onDelete = { faceToDelete = face }
                        )
                    }
                }
            }
        }
    }

    // --- DELETE DIALOG ---
    if (faceToDelete != null) {
        AlertDialog(
            onDismissRequest = { faceToDelete = null },
            title = { Text("Hapus Data Siswa?") },
            text = { Text("Apakah Anda yakin ingin menghapus ${faceToDelete?.name}? Data biometrik dan foto akan dihapus permanen.") },
            confirmButton = {
                Button(
                    onClick = {
                        faceToDelete?.let { 
                            faceViewModel.deleteFace(it)
                            context.showToast("Siswa dihapus")
                        }
                        faceToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Hapus") }
            },
            dismissButton = {
                TextButton(onClick = { faceToDelete = null }) { Text("Batal") }
            }
        )
    }
}

@Composable
fun UserListItem(
    face: FaceEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Student Photo
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
            
            // Actions
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
fun EmptyStateView(isSearching: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (isSearching) Icons.Default.SearchOff else Icons.Default.PeopleOutline,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color.LightGray
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isSearching) "Pencarian tidak ditemukan" else "Belum ada data siswa",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )
    }
}