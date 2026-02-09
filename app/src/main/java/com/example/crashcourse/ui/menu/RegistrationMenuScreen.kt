package com.example.crashcourse.ui.menu

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate // 🚀 FIX: Import Icon Gallery
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crashcourse.ui.components.AzuraTitle
import com.example.crashcourse.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationMenuScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddUser: () -> Unit,
    onNavigateToBulkRegister: () -> Unit,
    onNavigateToSingleUpload: () -> Unit // 🚀 FIX: Parameter baru untuk Single Upload
) {
    Scaffold(
        containerColor = AzuraBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Registrasi Siswa", 
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AzuraText
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = AzuraText
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            AzuraTitle("Metode Pendaftaran")
            
            Text(
                text = "Pilih salah satu metode pendaftaran murid di bawah ini untuk memulai sinkronisasi database Azura AI.",
                style = MaterialTheme.typography.bodyMedium,
                color = AzuraText.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // 1. Pendaftaran Mandiri (Kamera Live)
            AzuraRegistrationCard(
                title = "Pendaftaran Mandiri",
                description = "Gunakan kamera untuk memindai wajah murid secara langsung di lokasi.",
                icon = Icons.Default.PersonAdd,
                onClick = onNavigateToAddUser
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Upload Foto Tunggal (Galeri)
            AzuraRegistrationCard(
                title = "Upload Foto Galeri",
                description = "Daftarkan satu murid menggunakan file foto yang sudah ada di galeri ponsel.",
                icon = Icons.Default.AddPhotoAlternate,
                onClick = onNavigateToSingleUpload
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Bulk Import (CSV/Excel)
            AzuraRegistrationCard(
                title = "Bulk Import (CSV)",
                description = "Daftarkan banyak murid sekaligus menggunakan file CSV dan kumpulan foto.",
                icon = Icons.Default.FileUpload,
                onClick = onNavigateToBulkRegister
            )

            Spacer(modifier = Modifier.weight(1f))

            // Branding Footer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = AzuraPrimary.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "AZURA AI SECURE v1.0",
                        style = MaterialTheme.typography.labelSmall,
                        color = AzuraPrimary.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AzuraRegistrationCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Container
            Surface(
                shape = CircleShape,
                color = AzuraPrimary.copy(alpha = 0.1f),
                modifier = Modifier.size(60.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp),
                        tint = AzuraPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = AzuraText,
                        fontSize = 17.sp
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = AzuraText.copy(alpha = 0.5f),
                    lineHeight = 16.sp
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.LightGray,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}