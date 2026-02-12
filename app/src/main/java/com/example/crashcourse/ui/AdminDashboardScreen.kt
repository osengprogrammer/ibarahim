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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.crashcourse.navigation.Screen
import com.example.crashcourse.ui.components.AzuraDropdown
import com.example.crashcourse.ui.theme.*
import com.example.crashcourse.viewmodel.AuthState

data class AdminMenuItem(
    val title: String, 
    val icon: ImageVector, 
    val route: String, 
    val color: Color
)

@Composable
fun AdminDashboardScreen(
    navController: NavController,
    authState: AuthState.Active,
    onBack: () -> Unit = { navController.popBackStack() }
) {
    // --- 🎓 STATE PEMILIHAN SESI ---
    // Pastikan inisialisasi awal tidak null agar tidak mismatch
    var selectedSession by remember { mutableStateOf("") }
    val classes = authState.assignedClasses ?: emptyList() 

    // --- 🧠 LOGIKA FILTER MENU ---
    val menuItems = remember(authState.role) {
        val list = mutableListOf<AdminMenuItem>()

        // 1. MENU UMUM (GURU & ADMIN)
        list.add(AdminMenuItem("Data Siswa", Icons.AutoMirrored.Filled.List, Screen.FaceList.route, Color(0xFFFF9800)))
        list.add(AdminMenuItem("Profil Saya", Icons.Default.AccountCircle, Screen.Profile.route, Color(0xFF607D8B)))

        // 2. MENU ADMIN & SUPERVISOR
        if (authState.role == "ADMIN" || authState.role == "SUPERVISOR") {
             list.add(0, AdminMenuItem("Riwayat Absensi", Icons.Default.History, Screen.History.route, Color(0xFF3F51B5)))
             list.add(2, AdminMenuItem("Live Monitor", Icons.Default.MonitorHeart, Screen.LiveMonitor.route, Color(0xFF00BCD4)))
             list.add(AdminMenuItem("Staff & Guru", Icons.Default.ManageAccounts, Screen.UserManagement.route, Color(0xFF673AB7)))
        }

        // 3. MENU KHUSUS ADMIN (GOD MODE)
        if (authState.role == "ADMIN") {
            list.add(AdminMenuItem("Master Data", Icons.Default.Category, Screen.Options.route, Color(0xFF9C27B0)))
            list.add(AdminMenuItem("Manajemen Rombel", Icons.Default.Groups, Screen.MasterClass.route, Color(0xFFE91E63)))
            list.add(AdminMenuItem("Menu Pendaftaran", Icons.Default.AppRegistration, Screen.RegistrationMenu.route, Color(0xFF4CAF50)))
        }
        list
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AzuraBg)
            .padding(16.dp)
    ) {
        // --- HEADER ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = if (authState.role == "ADMIN") "Panel Admin" else "Menu Guru",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = AzuraPrimary
                )
                Text(
                    text = "${authState.schoolName} • ${authState.role}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AzuraText.copy(alpha = 0.6f)
                )
            }
            
            IconButton(
                onClick = { navController.navigate(Screen.Profile.route) },
                modifier = Modifier.background(Color.White, RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = AzuraPrimary)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        // --- 🚀 SECTION: PEMILIHAN SESI AKTIF ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Mulai Absensi Baru",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AzuraPrimary
                )
                Text(
                    text = "Pilih mata kuliah/sesi sebelum membuka kamera",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // ✅ FIXED: Menambahkan itemLabel lambda dan memastikan tipe data String
                AzuraDropdown(
                    label = "Pilih Sesi Matkul",
                    options = classes,
                    selected = selectedSession,
                    onSelected = { it?.let { selectedSession = it } },
                    itemLabel = { it } // Lambda ini memberitahu dropdown cara menampilkan teks
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedSession.isNotBlank(),
                    onClick = { 
                        // Navigasi ke scanner dengan parameter sesi
                        navController.navigate("checkin_screen/$selectedSession") 
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AzuraPrimary),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Icon(Icons.Default.QrCodeScanner, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Buka Scanner Kamera", fontWeight = FontWeight.Bold)
                }
                
                if (classes.isEmpty()) {
                    Text(
                        text = "*Anda belum memiliki jadwal kelas yang diampu.",
                        color = Color.Red,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Menu Manajemen",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = AzuraText.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(12.dp))

        // --- GRID MENU ---
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(menuItems) { menu ->
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
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
                modifier = Modifier.size(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = item.color,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = item.title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = AzuraText
            )
        }
    }
}