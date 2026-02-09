package com.example.crashcourse.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crashcourse.viewmodel.DashboardViewModel
import com.example.crashcourse.viewmodel.LiveLog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveAttendanceScreen(
    onBack: () -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Live Monitoring", fontWeight = FontWeight.Bold)
                        Text("Real-time from Cloud", style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        // Gunakan AutoMirrored agar support RTL (Arab/Hebrew)
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant, // Warna TopBar lebih soft
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 1. CHART SECTION
            Card(
                modifier = Modifier.fillMaxWidth().height(220.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pie Chart Area
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        SimplePieChart(
                            present = stats.present,
                            sick = stats.sick,
                            permit = stats.permit,
                            alpha = stats.alpha,
                            total = stats.total
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stats.total.toString(), 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 32.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text("Total", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                    
                    // Legend Area
                    Column(
                        modifier = Modifier.weight(1f).padding(start = 16.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        LegendItem("Hadir", stats.present, Color(0xFF4CAF50)) // Green
                        LegendItem("Sakit", stats.sick, Color(0xFFFFC107))    // Amber
                        LegendItem("Izin", stats.permit, Color(0xFF2196F3))   // Blue
                        LegendItem("Alpha", stats.alpha, Color(0xFFF44336))   // Red
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Live Feed (Hari Ini)", 
                style = MaterialTheme.typography.titleMedium, 
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 2. LIST SECTION
            if (logs.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text("Belum ada absensi hari ini", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(items = logs, key = { it.id }) { log ->
                        LogItem(log)
                    }
                }
            }
        }
    }
}

@Composable
fun LogItem(log: LiveLog) {
    val color = when(log.status) {
        "PRESENT", "LATE" -> Color(0xFF4CAF50)
        "SAKIT", "SICK" -> Color(0xFFFFC107)
        "IZIN", "PERMIT" -> Color(0xFF2196F3)
        else -> Color(0xFFF44336)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Dot
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(16.dp))
            
            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(log.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${log.className} • ${log.status}", 
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            
            // Time
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = log.time, 
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall, 
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun LegendItem(label: String, count: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically, 
        modifier = Modifier.padding(vertical = 6.dp)
    ) {
        Icon(Icons.Default.Circle, null, tint = color, modifier = Modifier.size(10.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label: $count", 
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SimplePieChart(present: Int, sick: Int, permit: Int, alpha: Int, total: Int) {
    // Animasi Smooth (1 Detik)
    val animSpec = tween<Float>(durationMillis = 1000)

    val pSweep = if (total > 0) (present.toFloat() / total) * 360f else 0f
    val sSweep = if (total > 0) (sick.toFloat() / total) * 360f else 0f
    val iSweep = if (total > 0) (permit.toFloat() / total) * 360f else 0f
    val aSweep = if (total > 0) (alpha.toFloat() / total) * 360f else 0f

    val animP by animateFloatAsState(pSweep, animSpec, label = "P")
    val animS by animateFloatAsState(sSweep, animSpec, label = "S")
    val animI by animateFloatAsState(iSweep, animSpec, label = "I")
    val animA by animateFloatAsState(aSweep, animSpec, label = "A")

    Canvas(modifier = Modifier.size(160.dp)) {
        val strokeWidth = 35f // Ketebalan Donut
        var startAngle = -90f // Mulai dari jam 12

        // Background Circle (Abu-abu jika kosong)
        if (total == 0) {
            drawCircle(
                color = Color.LightGray.copy(alpha = 0.3f),
                style = Stroke(width = strokeWidth)
            )
            return@Canvas
        }

        // 1. Hadir
        if (animP > 0) {
            drawArc(
                color = Color(0xFF4CAF50),
                startAngle = startAngle,
                sweepAngle = animP,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
            startAngle += animP
        }

        // 2. Sakit
        if (animS > 0) {
            drawArc(
                color = Color(0xFFFFC107),
                startAngle = startAngle,
                sweepAngle = animS,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
            startAngle += animS
        }

        // 3. Izin
        if (animI > 0) {
            drawArc(
                color = Color(0xFF2196F3),
                startAngle = startAngle,
                sweepAngle = animI,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
            startAngle += animI
        }

        // 4. Alpha
        if (animA > 0) {
            drawArc(
                color = Color(0xFFF44336),
                startAngle = startAngle,
                sweepAngle = animA,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
        }
    }
}