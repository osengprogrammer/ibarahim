package com.example.crashcourse.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
    onNavigateBack: () -> Unit, // 🚀 Tambahkan navigasi balik
    onInviteStaff: (String, String) -> Unit = { _, _ -> }, 
    syncViewModel: SyncViewModel = viewModel()
) {
    val context = LocalContext.current
    val syncState by syncViewModel.syncState.collectAsStateWithLifecycle()

    var showInviteDialog by remember { mutableStateOf(false) }
    var staffEmail by remember { mutableStateOf("") }
    var staffPassword by remember { mutableStateOf("") }

    val isAdmin = authState.role == "ADMIN"

    // Feedback Sinkronisasi Cloud
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profil Sekolah") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Avatar & Informasi Utama
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = authState.schoolName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (isAdmin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = authState.role, 
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), 
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            
            Text(authState.email, style = MaterialTheme.typography.bodySmall, color = Color.Gray)

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Kartu Informasi Lisensi
            InfoCard(
                title = "Masa Aktif Lisensi",
                value = formatMillisToDate(authState.expiryMillis),
                icon = Icons.Default.DateRange
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // 3. FITUR SYNC DATA (Cloud -> Local)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudSync, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Sinkronisasi Data", 
                            style = MaterialTheme.typography.titleMedium, 
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        "Tarik data murid terbaru dari Cloud ke database offline perangkat.", 
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
                            // ✅ FIX: Panggil tanpa parameter atau sesuaikan dengan ViewModel
                            onClick = { syncViewModel.syncStudentsDown() }, 
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                contentColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Text("Ambil Data Murid")
                        }
                    }
                }
            }

            // 4. MANAJEMEN STAFF (KHUSUS ADMIN)
            if (isAdmin) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Manajemen Staff",
                    modifier = Modifier.fillMaxWidth(),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedButton(
                    onClick = { showInviteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.PersonAdd, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Daftarkan Guru Baru")
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(24.dp))

            // 5. Tombol Logout
            TextButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Logout, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Logout / Keluar Akun", fontWeight = FontWeight.Bold)
            }
        }
    }

    // --- DIALOG INVITE STAFF ---
    if (showInviteDialog) {
        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            title = { Text("Pendaftaran Guru") },
            text = {
                Column {
                    Text("Role: TEACHER. Akses kelas dapat diatur melalui Portal Admin.", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = staffEmail,
                        onValueChange = { staffEmail = it },
                        label = { Text("Email Guru") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = staffPassword,
                        onValueChange = { staffPassword = it },
                        label = { Text("Password Sementara") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onInviteStaff(staffEmail, staffPassword)
                        showInviteDialog = false
                        staffEmail = ""
                        staffPassword = ""
                    },
                    enabled = staffEmail.isNotBlank() && staffPassword.length >= 6
                ) {
                    Text("Simpan")
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
                Text(title, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

fun formatMillisToDate(millis: Long): String {
    val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
    return sdf.format(Date(millis))
}