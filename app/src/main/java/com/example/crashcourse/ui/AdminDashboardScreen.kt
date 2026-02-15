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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.crashcourse.navigation.Screen
import com.example.crashcourse.ui.components.AzuraDropdown
import com.example.crashcourse.ui.theme.*
import com.example.crashcourse.viewmodel.AuthState
import com.example.crashcourse.viewmodel.MasterClassViewModel
import com.example.crashcourse.viewmodel.RecognitionViewModel
import com.example.crashcourse.db.MasterClassWithNames

// ✅ DEFINISI DATA CLASS (Taruh di luar agar dikenal di file ini)
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
    masterClassVM: MasterClassViewModel = viewModel(),
    recognitionVM: RecognitionViewModel = viewModel(),
    onSwitchToScanner: () -> Unit // Parameter ini wajib dikirim dari MainScreen
) {
    var showScannerDialog by remember { mutableStateOf(false) }
    var selectedSession by remember { mutableStateOf("General") } 
    
    // ✅ FIX: Tambahkan tipe data eksplisit agar tidak error 'Cannot infer type'
    val allMasterClasses: List<MasterClassWithNames> by masterClassVM.masterClassesWithNames.collectAsState(initial = emptyList())
    
    val sessionOptions = remember(allMasterClasses, authState.assignedClasses) {
        val list = mutableListOf("General") 
        if (authState.role == "ADMIN") {
            list.addAll(allMasterClasses.map { it.className })
        } else {
            list.addAll(authState.assignedClasses)
        }
        list.distinct()
    }

    val menuItems = remember(authState.role) {
        val list = mutableListOf<AdminMenuItem>()
        list.add(AdminMenuItem("Scanner Absensi", Icons.Default.QrCodeScanner, "SCANNER_TRIGGER", AzuraPrimary))
        list.add(AdminMenuItem("Data Siswa", Icons.AutoMirrored.Filled.List, Screen.FaceList.route, Color(0xFFFF9800)))
        
        if (authState.role == "ADMIN" || authState.role == "SUPERVISOR") {
             list.add(AdminMenuItem("Riwayat", Icons.Default.History, Screen.History.route, Color(0xFF3F51B5)))
             list.add(AdminMenuItem("Live Monitor", Icons.Default.MonitorHeart, Screen.LiveMonitor.route, Color(0xFF00BCD4)))
             list.add(AdminMenuItem("Staff & Guru", Icons.Default.ManageAccounts, Screen.UserManagement.route, Color(0xFF673AB7)))
        }

        if (authState.role == "ADMIN") {
            list.add(AdminMenuItem("Master Data", Icons.Default.Category, Screen.Options.route, Color(0xFF9C27B0)))
            list.add(AdminMenuItem("Rombel", Icons.Default.Groups, Screen.MasterClass.route, Color(0xFFE91E63)))
            list.add(AdminMenuItem("Pendaftaran", Icons.Default.AppRegistration, Screen.RegistrationMenu.route, Color(0xFF4CAF50)))
        }
        list.add(AdminMenuItem("Profil Saya", Icons.Default.AccountCircle, Screen.Profile.route, Color(0xFF607D8B)))
        list
    }

    Scaffold(containerColor = AzuraBg) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            // HEADER ... (Kodingan header kamu tetap sama)
            Text(text = "Panel Admin", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = AzuraPrimary)
            Spacer(modifier = Modifier.height(24.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(menuItems) { menu ->
                    AdminMenuCard(item = menu) {
                        if (menu.route == "SCANNER_TRIGGER") {
                            showScannerDialog = true 
                        } else {
                            navController.navigate(menu.route)
                        }
                    }
                }
            }
        }
    }

    if (showScannerDialog) {
        AlertDialog(
            onDismissRequest = { showScannerDialog = false },
            title = { Text("Mulai Absensi", fontWeight = FontWeight.Bold) },
            text = {
                AzuraDropdown(
                    label = "Sesi Aktif",
                    options = sessionOptions,
                    selected = selectedSession,
                    onSelected = { selectedSession = it ?: "General" },
                    itemLabel = { it }
                )
            },
            confirmButton = {
                Button(onClick = {
                    showScannerDialog = false
                    recognitionVM.activeSessionClass = selectedSession
                    onSwitchToScanner() // ✅ Pindah tab
                }) { Text("Buka Kamera") }
            },
            dismissButton = { TextButton(onClick = { showScannerDialog = false }) { Text("Batal") } }
        )
    }
}

// ✅ PINDAHKAN AdminMenuCard KE LUAR
@Composable
fun AdminMenuCard(item: AdminMenuItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(140.dp).clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(item.icon, null, tint = item.color, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text(item.title, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}