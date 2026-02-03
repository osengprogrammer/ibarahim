package com.example.crashcourse.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.crashcourse.viewmodel.UserManagementViewModel
import com.example.crashcourse.viewmodel.UserProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    onBack: () -> Unit,
    onEditUser: (String) -> Unit,
    viewModel: UserManagementViewModel = viewModel()
) {
    val users by viewModel.users.collectAsState()

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
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            items(users) { user ->
                StaffCard(user, onEdit = { onEditUser(user.uid) })
            }
        }
    }
}

@Composable
fun StaffCard(user: UserProfile, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isInvitePending = user.role == "USER" // Atau status == PENDING
            
            Icon(
                imageVector = if (user.role == "ADMIN") Icons.Default.AdminPanelSettings else Icons.Default.Person,
                contentDescription = null,
                tint = if (user.role == "ADMIN") MaterialTheme.colorScheme.primary else Color.Gray,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(user.email, style = MaterialTheme.typography.titleMedium)
                Text("Role: ${user.role}", style = MaterialTheme.typography.bodySmall)
                if (user.assignedClasses.isNotEmpty()) {
                    Text(
                        "Kelas: ${user.assignedClasses.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Role & Scope")
            }
        }
    }
}