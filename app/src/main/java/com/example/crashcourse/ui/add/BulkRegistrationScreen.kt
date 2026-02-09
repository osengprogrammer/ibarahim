package com.example.crashcourse.ui.add

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.crashcourse.ui.components.* import com.example.crashcourse.ui.theme.*
import com.example.crashcourse.viewmodel.RegisterViewModel
import androidx.compose.ui.draw.clip // 🚀 TAMBAHKAN BARIS INI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkRegistrationScreen(
    bulkViewModel: RegisterViewModel = viewModel(),
    onNavigateBack: () -> Unit // 🚀 Navigasi kembali
) {
    val context = LocalContext.current
    val bulkState by bulkViewModel.state.collectAsState()
    
    var fileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }

    // Launcher untuk pilih file CSV
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val contentResolver = context.contentResolver
            var fileName: String? = null
            contentResolver.query(it, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex != -1) fileName = cursor.getString(nameIndex)
            }
            
            fileUri = it
            selectedFileName = fileName ?: "data_siswa.csv"
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AzuraText)
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
            Spacer(Modifier.height(32.dp))
            AzuraTitle("Import Massal")
            
            Text(
                text = "Gunakan file CSV untuk mendaftarkan murid dalam jumlah banyak sekaligus.",
                style = MaterialTheme.typography.bodyMedium,
                color = AzuraText.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // --- SELECTION CARD ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
            ) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (selectedFileName == null) {
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
                        AzuraButton(
                            text = "Pilih File CSV",
                            onClick = { fileLauncher.launch("*/*") }
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                            Text(bulkState.estimatedTime, style = MaterialTheme.typography.labelSmall, color = AzuraPrimary)
                            Spacer(Modifier.height(8.dp))
                            AzuraButton(
                                text = "Mulai Proses Sekarang",
                                onClick = { fileUri?.let { bulkViewModel.processCsvFile(context, it) } }
                            )
                        }
                    }
                }
            }

            // --- PROGRESS OVERLAY ---
            AnimatedVisibility(visible = bulkState.isProcessing) {
                Column(Modifier.padding(vertical = 32.dp)) {
                    LinearProgressIndicator(
                        progress = { bulkState.progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        color = AzuraPrimary,
                        trackColor = AzuraPrimary.copy(alpha = 0.1f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(bulkState.status, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text(bulkState.currentPhotoType, style = MaterialTheme.typography.labelSmall)
                }
            }

            // --- SUMMARY & LOGS ---
            if (!bulkState.isProcessing && bulkState.results.isNotEmpty()) {
                Spacer(Modifier.height(32.dp))
                ResultSummaryView(bulkState.successCount, bulkState.duplicateCount, bulkState.errorCount)
                
                Spacer(Modifier.height(24.dp))
                Text("Log Aktivitas", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                
                bulkState.results.forEach { result ->
                    LogItemView(result)
                }
            }
            
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
fun ResultSummaryView(success: Int, dup: Int, err: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SummaryItem("Sukses", success, AzuraSuccess, Modifier.weight(1f))
        SummaryItem("Duplikat", dup, Color(0xFFFFA500), Modifier.weight(1f))
        SummaryItem("Gagal", err, AzuraError, Modifier.weight(1f))
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
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(count.toString(), fontSize = 20.sp, fontWeight = FontWeight.Black, color = color)
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun LogItemView(result: com.example.crashcourse.utils.ProcessResult) {
    val color = when {
        result.status.contains("Registered") -> AzuraSuccess
        result.status.contains("Duplicate") -> Color(0xFFFFA500)
        else -> AzuraError
    }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).background(Color.White, RoundedCornerShape(8.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(result.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(result.status, fontSize = 12.sp, color = color)
        }
    }
}