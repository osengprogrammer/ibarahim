package com.example.crashcourse.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items // 🚀 Import Penting
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.crashcourse.navigation.Screen

// Data class untuk Menu Admin
data class AdminMenuItem(
    val title: String, 
    val icon: ImageVector, 
    val route: String, 
    val color: Color
)

@Composable
fun AdminDashboardScreen(navController: NavController) {
    // Definisi item menu
    val adminMenus = listOf(
        // 1. Menu Pendaftaran (Hub ke Camera/Gallery/Bulk)
        AdminMenuItem("Menu Pendaftaran", Icons.Default.AppRegistration, Screen.RegistrationMenu.route, Color(0xFF4CAF50)),
        
        // 2. Upload CSV Langsung (Shortcut)
        AdminMenuItem("Bulk Upload", Icons.Default.CloudUpload, Screen.BulkRegister.route, Color(0xFF2196F3)),
        
        // 3. Daftar Wajah (Manage Faces)
        AdminMenuItem("Data Wajah", Icons.AutoMirrored.Filled.List, Screen.Manage.route, Color(0xFFFF9800)),
        
        // 4. Live Monitor
        AdminMenuItem("Live Monitor", Icons.Default.MonitorHeart, Screen.LiveMonitor.route, Color(0xFFE91E63)),
        
        // 5. Manage Staff
        AdminMenuItem("Staff & Guru", Icons.Default.ManageAccounts, Screen.UserManagement.route, Color(0xFF673AB7)),
        
        // 6. Master Data (Options)
        AdminMenuItem("Master Data", Icons.Default.Category, Screen.Options.route, Color(0xFF9C27B0))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Panel Admin",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Pusat kendali sistem AzuraTech.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(adminMenus) { menu ->
                AdminMenuCard(item = menu) {
                    navController.navigate(menu.route)
                }
            }
        }
    }
}

@Composable
fun AdminMenuCard(item: AdminMenuItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = item.color.copy(alpha = 0.1f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = item.color,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}