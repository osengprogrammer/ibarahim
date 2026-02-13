package com.example.crashcourse.ui.management

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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.crashcourse.firestore.user.UserProfile
import com.example.crashcourse.ui.theme.AzuraPrimary
import com.example.crashcourse.viewmodel.AuthState
import com.example.crashcourse.viewmodel.UserManagementViewModel
// 🔥 IMPORT YANG BENAR (Dari package yang sama atau file State baru)
import com.example.crashcourse.ui.management.UserListState 

/**
 * 👥 Staff & Account Management Screen
 * Layar kendali Admin untuk mengelola akun login Guru dan Staff.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    authState: AuthState.Active,
    onBack: () -> Unit,
    onEditScope: (String) -> Unit, // Mengirim UID atau Email ke EditUserScopeScreen
    userVM: UserManagementViewModel = viewModel()
) {
    // Gunakan 'by' dan pastikan import getValue ada (sudah ditangani oleh Compose runtime)
    val uiState by userVM.uiState.collectAsStateWithLifecycle()
    var showInviteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Manajemen Akun Staff", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(authState.schoolName, style = MaterialTheme.typography.labelSmall, color = AzuraPrimary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = { userVM.fetchUsers() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showInviteDialog = true },
                containerColor = AzuraPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.PersonAdd, "Undang Staff")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(Color(0xFFF8F9FA))) {
            // Casting state ke UserListState agar property 'users' dan 'message' terbaca
            when (val state = uiState) {
                is UserListState.Loading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                
                is UserListState.Success -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 80.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.users, key = { it.email }) { user ->
                            StaffAccountItem(
                                user = user,
                                onEditAccess = {
                                    val identifier = if (user.uid.isNotEmpty()) user.uid else user.email
                                    onEditScope(identifier)
                                }
                            )
                        }
                    }
                }

                is UserListState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.ErrorOutline, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(state.message, color = Color.Gray, fontSize = 14.sp)
                        TextButton(onClick = { userVM.fetchUsers() }) { Text("Coba Lagi") }
                    }
                }
                
                else -> {}
            }
        }
    }

    if (showInviteDialog) {
        InviteStaffDialog(
            onDismiss = { showInviteDialog = false },
            onConfirm = { email, role ->
                userVM.inviteStaff(email, role) 
                showInviteDialog = false
            }
        )
    }
}

/**
 * 🎨 Baris item Akun Staff
 */
@Composable
fun StaffAccountItem(user: UserProfile, onEditAccess: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = user.email, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                
                Spacer(Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (user.role == "ADMIN") Color(0xFFE3F2FD) else Color(0xFFF1F8E9),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = user.role,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (user.role == "ADMIN") Color(0xFF1976D2) else Color(0xFF388E3C),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(Modifier.width(8.dp))
                    
                    val statusText = if (user.isRegistered) "Aktif" else "Menunggu Registrasi"
                    val statusColor = if (user.isRegistered) Color(0xFF4CAF50) else Color(0xFFFF9800)
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(statusColor, RoundedCornerShape(50)))
                        Spacer(Modifier.width(4.dp))
                        Text(text = statusText, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
                
                if (user.assigned_classes.isNotEmpty()) {
                    Text(
                        text = "Scope: ${user.assigned_classes.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AzuraPrimary,
                        modifier = Modifier.padding(top = 6.dp),
                        maxLines = 1
                    )
                }
            }

            Surface(
                onClick = onEditAccess,
                color = AzuraPrimary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.VpnKey, "Atur Akses", tint = AzuraPrimary, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

/**
 * 🛠️ Dialog Input Undangan
 */
@Composable
fun InviteStaffDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("TEACHER") }
    val roles = listOf("TEACHER", "SUPERVISOR")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Undang Staff", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Guru yang diundang akan otomatis tergabung ke sekolah ini setelah registrasi.", fontSize = 12.sp, color = Color.Gray)
                
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Resmi") },
                    placeholder = { Text("guru@sekolah.com") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Email, null, modifier = Modifier.size(18.dp)) }
                )
                
                Column {
                    Text("Pilih Otoritas:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        roles.forEach { role ->
                            FilterChip(
                                selected = selectedRole == role,
                                onClick = { selectedRole = role },
                                label = { Text(role) },
                                leadingIcon = if (selectedRole == role) {
                                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(email.trim(), selectedRole) },
                enabled = email.contains("@") && email.length > 5,
                shape = RoundedCornerShape(8.dp)
            ) { Text("Kirim Undangan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}