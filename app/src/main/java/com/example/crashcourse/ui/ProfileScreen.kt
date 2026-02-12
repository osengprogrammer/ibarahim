package com.example.crashcourse.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape // 🚀 FIX: Import ini wajib ada
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip // 🚀 FIX: Import ini wajib ada
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

/**
 * 👤 ProfileScreen
 * Menampilkan informasi akun dan sinkronisasi data cloud.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authState: AuthState.Active,
    onLogout: () -> Unit,
    onNavigateBack: () -> Unit,
    syncViewModel: SyncViewModel = viewModel()
) {
    val context = LocalContext.current
    val syncState by syncViewModel.syncState.collectAsStateWithLifecycle()
    val isAdmin = authState.role == "ADMIN"

    // --- 🔔 FEEDBACK SINKRONISASI ---
    LaunchedEffect(syncState) {
        when(val s = syncState) {
            is SyncState.Success -> {
                Toast.makeText(context, s.message, Toast.LENGTH_SHORT).show()
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
                title = { Text("Profil Akun", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8F9FA))
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- 1. HEADER IDENTITAS ---
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = null,
                    modifier = Modifier.size(50.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = authState.schoolName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isAdmin) Color(0xFFE3F2FD) else Color(0xFFF1F8E9),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Text(
                    text = authState.role, 
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), 
                    color = if (isAdmin) Color(0xFF1976D2) else Color(0xFF388E3C),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text(text = authState.email, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)

            Spacer(modifier = Modifier.height(32.dp))

            // --- 2. KARTU INFORMASI LISENSI ---
            InfoCard(
                title = "Status Lisensi Sekolah",
                value = "Berlaku hingga ${formatMillisToDate(authState.expiryMillis)}",
                icon = Icons.Default.VerifiedUser,
                iconColor = Color(0xFF4CAF50)
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // --- 3. KARTU SINKRONISASI DATA ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudSync, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Sinkronisasi Cloud", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        "Tarik data personel terbaru dari server ke database lokal.", 
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (syncState is SyncState.Loading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)))
                        Text(
                            text = (syncState as SyncState.Loading).message,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Button(
                            onClick = { syncViewModel.syncStudentsDown() }, 
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tarik Data Terbaru")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // --- 4. TOMBOL KELUAR ---
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Logout, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Keluar Akun", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun InfoCard(title: String, value: String, icon: ImageVector, iconColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = iconColor.copy(alpha = 0.1f),
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

fun formatMillisToDate(millis: Long): String {
    val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
    return sdf.format(Date(millis))
}