package com.example.crashcourse.ui.add

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle // 🚀 Gunakan ini agar lifecycle-aware
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.crashcourse.utils.ProcessResult
import com.example.crashcourse.viewmodel.RegisterViewModel

// Pastikan kamu punya file warna tema ini, atau ganti dengan MaterialTheme.colorScheme
// Contoh sederhana jika file theme belum ada:
val AzuraPrimary = Color(0xFF6200EE)
val AzuraBg = Color(0xFFF5F5F5)
val AzuraText = Color(0xFF333333)
val AzuraError = Color(0xFFB00020)
val AzuraSuccess = Color(0xFF4CAF50)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkRegistrationScreen(
    bulkViewModel: RegisterViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    
    // 🚀 Gunakan collectAsStateWithLifecycle untuk performa UI yang lebih baik
    val bulkState by bulkViewModel.state.collectAsStateWithLifecycle()
    
    var fileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }

    // Launcher untuk pilih file CSV
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val contentResolver = context.contentResolver
            var fileName: String? = null
            
            // Ambil nama file asli dari URI
            contentResolver.query(it, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex != -1) fileName = cursor.getString(nameIndex)
            }
            
            fileUri = it
            selectedFileName = fileName ?: "data_siswa.csv"
            
            // Reset state sebelum mulai proses baru
            bulkViewModel.resetState()
            bulkViewModel.prepareProcessing(context, it)
        }
    }

    Scaffold(
        containerColor = AzuraBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Bulk Registration", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = AzuraText)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(24.dp))
            
            Text(
                "Import Massal", 
                style = MaterialTheme.typography.headlineSmall, 
                fontWeight = FontWeight.ExtraBold,
                color = AzuraPrimary
            )
            
            Text(
                text = "Gunakan file CSV untuk mendaftarkan murid dalam jumlah banyak sekaligus. Pastikan format kolom sesuai template.",
                style = MaterialTheme.typography.bodyMedium,
                color = AzuraText.copy(alpha = 0.6f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(Modifier.height(24.dp))

            // --- SELECTION CARD ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
            ) {
                Column(
                    Modifier.padding(24.dp), 
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (selectedFileName == null) {
                        // Tampilan Belum Pilih File
                        Surface(
                            shape = CircleShape,
                            color = AzuraPrimary.copy(alpha = 0.1f),
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.CloudUpload, null, tint = AzuraPrimary, modifier = Modifier.size(40.dp))
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { fileLauncher.launch("text/comma-separated-values") }, // MIME Type CSV
                            colors = ButtonDefaults.buttonColors(containerColor = AzuraPrimary)
                        ) {
                            Text("Pilih File CSV")
                        }
                    } else {
                        // Tampilan File Terpilih
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Description, null, tint = AzuraPrimary, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = selectedFileName!!, 
                                modifier = Modifier.weight(1f), 
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            IconButton(onClick = { 
                                selectedFileName = null
                                fileUri = null
                                bulkViewModel.resetState() 
                            }) {
                                Icon(Icons.Default.Delete, null, tint = AzuraError)
                            }
                        }
                        
                        if (!bulkState.isProcessing) {
                            Spacer(Modifier.height(20.dp))
                            
                            // Estimasi Waktu
                            if (bulkState.estimatedTime.isNotEmpty()) {
                                Text(
                                    bulkState.estimatedTime, 
                                    style = MaterialTheme.typography.labelSmall, 
                                    color = AzuraPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(Modifier.height(8.dp))
                            }

                            Button(
                                onClick = { 
                                    fileUri?.let { uri -> 
                                        bulkViewModel.processCsvFile(context, uri) 
                                    } 
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = AzuraPrimary)
                            ) {
                                Text("Mulai Proses Sekarang")
                            }
                        }
                    }
                }
            }

            // --- PROGRESS OVERLAY ---
            AnimatedVisibility(visible = bulkState.isProcessing) {
                Column(Modifier.padding(vertical = 32.dp)) {
                    LinearProgressIndicator(
                        progress = { bulkState.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = AzuraPrimary,
                        trackColor = AzuraPrimary.copy(alpha = 0.1f),
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(bulkState.status, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("${(bulkState.progress * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                    if (bulkState.currentPhotoType.isNotEmpty()) {
                        Text(bulkState.currentPhotoType, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
            }

            // --- SUMMARY & LOGS ---
            if (!bulkState.isProcessing && bulkState.results.isNotEmpty()) {
                Spacer(Modifier.height(32.dp))
                
                // Ringkasan Hasil
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryItem("Sukses", bulkState.successCount, AzuraSuccess, Modifier.weight(1f))
                    SummaryItem("Duplikat", bulkState.duplicateCount, Color(0xFFFFA500), Modifier.weight(1f))
                    SummaryItem("Gagal", bulkState.errorCount, AzuraError, Modifier.weight(1f))
                }
                
                Spacer(Modifier.height(24.dp))
                Text("Log Aktivitas", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                
                // Daftar Log (Scrollable di dalam Column parent)
                bulkState.results.forEach { result ->
                    LogItemView(result)
                }
            }
            
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
fun SummaryItem(label: String, count: Int, color: Color, modifier: Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(
            Modifier.padding(12.dp), 
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(count.toString(), fontSize = 20.sp, fontWeight = FontWeight.Black, color = color)
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun LogItemView(result: ProcessResult) {
    val color = when {
        result.status.contains("Registered") -> AzuraSuccess
        result.status.contains("Duplicate") -> Color(0xFFFFA500)
        else -> AzuraError
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(result.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(result.status, fontSize = 12.sp, color = color)
                if (result.error != null) {
                    Text("Err: ${result.error}", fontSize = 10.sp, color = Color.Red)
                }
            }
        }
    }
}