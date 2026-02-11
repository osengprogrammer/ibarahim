package com.example.crashcourse.ui.add

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.crashcourse.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationMenuScreen(
    // 🚀 FIXED: These names now match NavGraph.kt exactly
    onNavigateToSingleAdd: () -> Unit,
    onNavigateToBulk: () -> Unit,
    onNavigateToGallery: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = AzuraBg,
        topBar = {
            TopAppBar(
                title = { Text("Menu Pendaftaran", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Pilih Metode Registrasi",
                style = MaterialTheme.typography.titleLarge,
                color = AzuraText,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(8.dp))

            RegistrationCard(
                title = "Tambah Manual",
                subtitle = "Input data & ambil foto langsung",
                icon = Icons.Default.PersonAdd,
                color = AzuraPrimary,
                onClick = onNavigateToSingleAdd
            )

            RegistrationCard(
                title = "Upload Galeri",
                subtitle = "Ekstrak biometrik dari foto HP",
                icon = Icons.Default.CloudUpload,
                color = AzuraSecondary,
                onClick = onNavigateToGallery
            )

            RegistrationCard(
                title = "Import Bulk (CSV)",
                subtitle = "Registrasi massal via file",
                icon = Icons.Default.GroupAdd,
                color = AzuraSuccess,
                onClick = onNavigateToBulk
            )
        }
    }
}

@Composable
fun RegistrationCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(12.dp),
                color = color.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}