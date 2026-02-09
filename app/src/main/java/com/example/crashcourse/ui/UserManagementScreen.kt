package com.example.crashcourse.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // 🚀 Import Penting
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.crashcourse.viewmodel.UserListState // Import State
import com.example.crashcourse.viewmodel.UserManagementViewModel
import com.example.crashcourse.viewmodel.UserProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    onBack: () -> Unit,
    onEditUser: (String) -> Unit, // Mengirim UID
    viewModel: UserManagementViewModel = viewModel()
) {
    // 🚀 OBSERVASI STATE (Bukan list langsung)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manajemen Staff") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // 🚀 HANDLE LOGIC UI STATE
            when (val state = uiState) {
                is UserListState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                
                is UserListState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Warning, null, tint = Color.Red, modifier = Modifier.size(48.dp))
                        Text(
                            text = state.message,
                            color = Color.Red,
                            modifier = Modifier.padding(16.dp)
                        )
                        Button(onClick = { viewModel.fetchUsers() }) {
                            Text("Coba Lagi")
                        }
                    }
                }
                
                is UserListState.Success -> {
                    val users = state.users
                    
                    if (users.isEmpty()) {
                        Text(
                            "Belum ada data staff.",
                            modifier = Modifier.align(Alignment.Center),
                            color = Color.Gray
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 🚀 items sekarang mengambil data dari state.users
                            items(items = users, key = { it.uid }) { user ->
                                StaffCard(user, onEdit = { onEditUser(user.uid) })
                            }
                        }
                    }
                }
                
                else -> { /* Idle State - Do nothing */ }
            }
        }
    }
}

@Composable
fun StaffCard(user: UserProfile, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Role
            Icon(
                imageVector = if (user.role == "ADMIN") Icons.Default.AdminPanelSettings else Icons.Default.Person,
                contentDescription = null,
                tint = if (user.role == "ADMIN") MaterialTheme.colorScheme.primary else Color.Gray,
                modifier = Modifier.size(40.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Info User
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.email.ifBlank { "No Email" }, 
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Role: ", style = MaterialTheme.typography.bodySmall)
                    Text(
                        user.role, 
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (user.role == "ADMIN") MaterialTheme.colorScheme.primary else Color.Unspecified
                    )
                }

                if (user.assignedClasses.isNotEmpty()) {
                    Text(
                        text = "Akses: ${user.assignedClasses.joinToString(", ")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            // Tombol Edit
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Role")
            }
        }
    }
}