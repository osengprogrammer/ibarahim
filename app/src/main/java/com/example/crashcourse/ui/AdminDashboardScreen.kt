package com.example.crashcourse.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.crashcourse.ui.theme.*

/**
 * 🏛️ Admin Dashboard Menu Item Data
 */
data class AdminMenuItem(
    val title: String, 
    val icon: ImageVector, 
    val route: String, 
    val color: Color
)

@Composable
fun AdminDashboardScreen(navController: NavController) {
    
    val adminMenus = listOf(
        AdminMenuItem("Master Data", Icons.Default.Category, Screen.Options.route, Color(0xFF9C27B0)),
        AdminMenuItem("Manajemen Rombel", Icons.Default.Groups, Screen.MasterClass.route, Color(0xFFE91E63)),
        AdminMenuItem("Staff & Guru", Icons.Default.ManageAccounts, Screen.UserManagement.route, Color(0xFF673AB7)),
        AdminMenuItem("Menu Pendaftaran", Icons.Default.AppRegistration, Screen.RegistrationMenu.route, Color(0xFF4CAF50)),
        AdminMenuItem("Data Wajah", Icons.AutoMirrored.Filled.List, Screen.FaceList.route, Color(0xFFFF9800)),
        AdminMenuItem("Live Monitor", Icons.Default.MonitorHeart, Screen.LiveMonitor.route, Color(0xFF00BCD4))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AzuraBg)
            .padding(16.dp)
    ) {
        Text(
            text = "Panel Admin",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = AzuraPrimary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        Text(
            text = "Pusat kendali dan manajemen Azura Attendance.",
            style = MaterialTheme.typography.bodyMedium,
            color = AzuraText.copy(alpha = 0.6f),
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
            .height(140.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = item.color.copy(alpha = 0.1f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = item.color,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            
            Text(
                text = item.title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = AzuraText
            )
        }
    }
}