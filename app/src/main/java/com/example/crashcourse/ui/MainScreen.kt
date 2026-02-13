package com.example.crashcourse.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crashcourse.ui.theme.AzuraPrimary
import com.example.crashcourse.viewmodel.AuthState

/**
 * 🏛️ Azura Tech Main Dashboard (V.6.6 - Fixed Parameter Mismatch)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    authState: AuthState.Active,
    onNavigateToCheckIn: (String) -> Unit, // ✅ FIX: Sekarang menerima String
    onNavigateToAdmin: () -> Unit,
    onLogoutRequest: () -> Unit 
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text("AZURA ATTENDANCE", fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp) 
                },
                actions = {
                    IconButton(onClick = onLogoutRequest) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, "Logout", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Brush.verticalGradient(colors = listOf(Color(0xFFE3F2FD), Color.White)))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- HEADER SECTION ---
            Spacer(modifier = Modifier.height(20.dp))
            Surface(shape = CircleShape, color = AzuraPrimary.copy(alpha = 0.1f), modifier = Modifier.size(90.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = authState.schoolName.take(1).uppercase(), fontSize = 42.sp, fontWeight = FontWeight.Bold, color = AzuraPrimary)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Selamat Datang,", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
            Text(text = authState.email.substringBefore("@").replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            Text(text = authState.schoolName, style = MaterialTheme.typography.bodyMedium, color = AzuraPrimary)
            
            Spacer(modifier = Modifier.height(32.dp))

            // --- 🛡️ ROLE-BASED LOGIC ---
            if (authState.role == "ADMIN") {
                AdminQuickStats()
            } else {
                // 🔥 Jika Staff, tampilkan daftar kelas yang di-assign padanya
                StaffClassSelector(
                    classes = authState.assignedClasses,
                    onClassSelect = { onNavigateToCheckIn(it) }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- TOMBOL UTAMA: MULAI ABSENSI ---
            Button(
                onClick = { onNavigateToCheckIn("General Session") }, // Kirim default jika klik tombol utama
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AzuraPrimary),
                elevation = ButtonDefaults.buttonElevation(8.dp)
            ) {
                Icon(Icons.Default.CameraAlt, null)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Absensi Cepat (General)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- TOMBOL SEKUNDER: DASHBOARD ---
            OutlinedButton(
                onClick = onNavigateToAdmin,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AzuraPrimary),
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 2.dp)
            ) {
                Icon(Icons.Default.Dashboard, null)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (authState.role == "ADMIN") "Manajemen Sekolah" else "Laporan Saya", 
                    fontSize = 16.sp, 
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            Text(text = "ID Sekolah: ${authState.sekolahId}", fontSize = 10.sp, color = Color.LightGray)
            Text(text = "Azura Tech v1.0 • Banyuwangi Digital Solution", fontSize = 12.sp, color = Color.LightGray)
        }
    }
}

/**
 * 🎨 KOMPONEN: Daftar Kelas untuk Staff
 */
@Composable
fun StaffClassSelector(classes: List<String>, onClassSelect: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Pilih Rombel / Kelas Hari Ini:", fontWeight = FontWeight.Bold, color = Color.DarkGray)
        Spacer(Modifier.height(12.dp))
        if (classes.isEmpty()) {
            Text("Belum ada rombel yang di-assign.", color = Color.Red, fontSize = 12.sp)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(classes) { className ->
                    Card(
                        modifier = Modifier
                            .width(140.dp)
                            .clickable { onClassSelect(className) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = AzuraPrimary.copy(alpha = 0.05f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Group, null, tint = AzuraPrimary)
                            Spacer(Modifier.height(8.dp))
                            Text(className, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AzuraPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminQuickStats() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Settings, null, tint = AzuraPrimary)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = "Status Akses", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text(text = "Administrator Sekolah", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}