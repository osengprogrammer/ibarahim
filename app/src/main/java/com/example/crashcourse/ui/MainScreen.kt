package com.example.crashcourse.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crashcourse.ui.theme.AzuraPrimary
import com.example.crashcourse.viewmodel.AuthState

/**
 * 🏛️ Azura Tech Main Dashboard
 * UI yang adaptif berdasarkan Role (Admin Sekolah vs Guru/Staff).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    authState: AuthState.Active,
    onNavigateToCheckIn: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onLogoutRequest: () -> Unit // 🚀 Ditambahkan agar user bisa logout dengan aman
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "AZURA ATTENDANCE", 
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    ) 
                },
                actions = {
                    IconButton(onClick = onLogoutRequest) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp, 
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFE3F2FD), Color.White)
                    )
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- WELCOME HEADER ---
            Spacer(modifier = Modifier.height(20.dp))
            
            Surface(
                shape = CircleShape,
                color = AzuraPrimary.copy(alpha = 0.1f),
                modifier = Modifier.size(90.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = authState.schoolName.take(1).uppercase(),
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                        color = AzuraPrimary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Selamat Datang,",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray
            )
            
            Text(
                text = authState.email.substringBefore("@").replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black
            )
            
            Text(
                text = authState.schoolName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = AzuraPrimary
            )
            
            Spacer(modifier = Modifier.height(48.dp))

            // --- ROLE-BASED INFO CARD ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (authState.role == "ADMIN") Icons.Default.Settings else Icons.Default.Group,
                        contentDescription = null,
                        tint = AzuraPrimary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Status Akses", 
                            style = MaterialTheme.typography.labelSmall, 
                            color = Color.Gray
                        )
                        Text(
                            text = if (authState.role == "ADMIN") "Administrator Sekolah" else "Staff Pengajar",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- TOMBOL UTAMA: MULAI ABSENSI (Berlaku untuk Semua Role) ---
            Button(
                onClick = onNavigateToCheckIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AzuraPrimary),
                elevation = ButtonDefaults.buttonElevation(8.dp)
            ) {
                Icon(Icons.Default.CameraAlt, null)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Mulai Absensi Wajah", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- TOMBOL SEKUNDER: DASHBOARD (Filter Teks Berdasarkan Role) ---
            OutlinedButton(
                onClick = onNavigateToAdmin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
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
            
            // --- FOOTER INFO ---
            Text(
                text = "ID Sekolah: ${authState.sekolahId}",
                fontSize = 10.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                color = Color.LightGray
            )
            Text(
                text = "Azura Tech v1.0 • Banyuwangi Digital Solution",
                fontSize = 12.sp,
                color = Color.LightGray,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}