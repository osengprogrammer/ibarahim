package com.example.crashcourse.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onLogout: () -> Unit, // ✅ Menggunakan callback logout dari MainActivity
    onNavigateToForm: (String) -> Unit
) {
    // Karena info sekolah sekarang ada di database Firebase User, 
    // untuk sementara kita set label default. 
    // Anda bisa mengembangkannya dengan mempassing nama sekolah ke sini nanti.
    val schoolName = "AzuraTech School" 

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // --- BAGIAN 1: INFO STATUS AKUN ---
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = schoolName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Akun Terverifikasi",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF4CAF50) // Warna Hijau (Success)
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Data Master",
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Opsi-opsi standard
            item { OptionItem("Daftar Kelas", onClick = { onNavigateToForm("class") }) }
            item { OptionItem("Daftar Jurusan", onClick = { onNavigateToForm("major") }) }
            item { OptionItem("Daftar Mata Pelajaran", onClick = { onNavigateToForm("subject") }) }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Keamanan",
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // --- TOMBOL LOGOUT (Sistem Baru) ---
            item {
                Button(
                    onClick = { onLogout() }, // ✅ Memanggil fungsi logout pusat
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Keluar dari Akun")
                }
            }

            item {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Azura Attendance v1.1",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
private fun OptionItem(title: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}