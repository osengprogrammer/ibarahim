package com.example.crashcourse.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crashcourse.viewmodel.AuthState
import com.example.crashcourse.viewmodel.SyncState
import com.example.crashcourse.viewmodel.SyncViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authState: AuthState.Active,
    onLogout: () -> Unit,
    onInviteStaff: (String, String) -> Unit, 
    syncViewModel: SyncViewModel = viewModel()
) {
    val context = LocalContext.current
    val syncState by syncViewModel.syncState.collectAsStateWithLifecycle()

    var showInviteDialog by remember { mutableStateOf(false) }
    var staffEmail by remember { mutableStateOf("") }
    var staffPassword by remember { mutableStateOf("") }

    val isAdmin = authState.role == "ADMIN"

    LaunchedEffect(syncState) {
        when(val s = syncState) {
            is SyncState.Success -> {
                Toast.makeText(context, s.message, Toast.LENGTH_LONG).show()
                syncViewModel.resetState()
            }
            is SyncState.Error -> {
                Toast.makeText(context, s.message, Toast.LENGTH_LONG).show()
                syncViewModel.resetState()
            }
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Header Profil
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = authState.schoolName,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Badge(
            containerColor = if (isAdmin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
        ) {
            Text(authState.role, modifier = Modifier.padding(4.dp), color = Color.White)
        }
        Text(authState.email, style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Informasi Lisensi
        InfoCard(
            title = "Masa Aktif Lisensi",
            value = formatMillisToDate(authState.expiryMillis),
            icon = Icons.Default.DateRange
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        // 3. FITUR SYNC DATA
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudDownload, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Sinkronisasi Data", 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Text(
                    "Tarik data murid dari Cloud sesuai skop kelas Anda.", 
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                if (syncState is SyncState.Loading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text((syncState as SyncState.Loading).message, fontSize = 14.sp)
                    }
                } else {
                    Button(
                        // ✅ FIX: Gunakan authState.uid. Jika error, cek model Active Anda.
                        onClick = { syncViewModel.syncStudentsDown(authState.uid) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            contentColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text("Sync Now")
                    }
                }
            }
        }

        // 4. MENU KHUSUS ADMIN
        if (isAdmin) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Manajemen Staff",
                modifier = Modifier.fillMaxWidth(),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            // Button Invite
            OutlinedButton(
                onClick = { showInviteDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.PersonAdd, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Undang Guru Baru")
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(32.dp))

        // 5. Tombol Logout
        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Default.ExitToApp, null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Keluar dari Akun", color = Color.White)
        }
    }

    // --- DIALOG INVITE STAFF ---
    if (showInviteDialog) {
        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            title = { Text("Daftarkan Guru Baru") },
            text = {
                Column {
                    Text("Role default: TEACHER. Skop kelas bisa diatur di menu Manage Staff.", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = staffEmail,
                        onValueChange = { staffEmail = it },
                        label = { Text("Email Guru") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = staffPassword,
                        onValueChange = { staffPassword = it },
                        label = { Text("Password Sementara") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onInviteStaff(staffEmail, staffPassword)
                    showInviteDialog = false
                    staffEmail = ""
                    staffPassword = ""
                }) {
                    Text("Daftarkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInviteDialog = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
fun InfoCard(title: String, value: String, icon: ImageVector) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

fun formatMillisToDate(millis: Long): String {
    val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
    return sdf.format(Date(millis))
}