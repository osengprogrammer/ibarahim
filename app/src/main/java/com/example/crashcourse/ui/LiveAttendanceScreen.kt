package com.example.crashcourse.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crashcourse.viewmodel.DashboardViewModel

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
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pie Chart
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        SimplePieChart(
                            present = stats.present,
                            sick = stats.sick,
                            permit = stats.permit,
                            alpha = stats.alpha,
                            total = stats.total
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stats.total.toString(), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                            Text("Total", fontSize = 12.sp)
                        }
                    }
                    
                    // Legend
                    Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                        LegendItem("Hadir", stats.present, Color(0xFF4CAF50))
                        LegendItem("Sakit", stats.sick, Color(0xFFFFC107))
                        LegendItem("Izin", stats.permit, Color(0xFF2196F3))
                        LegendItem("Alpha", stats.alpha, Color(0xFFF44336))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            
            Text("Live Feed (Hari Ini)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            // 2. LIST SECTION
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs) { log ->
                    LogItem(log)
                }
            }
        }
    }
}

@Composable
fun LogItem(log: com.example.crashcourse.viewmodel.LiveLog) {
    val color = when(log.status) {
        "PRESENT" -> Color(0xFF4CAF50)
        "SAKIT" -> Color(0xFFFFC107)
        "IZIN" -> Color(0xFF2196F3)
        else -> Color(0xFFF44336)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
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
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(log.name, fontWeight = FontWeight.Bold)
                Text("${log.className} • ${log.status}", style = MaterialTheme.typography.bodySmall)
            }
            Text(log.time, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun LegendItem(label: String, count: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(Icons.Default.Circle, null, tint = color, modifier = Modifier.size(12.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("$label: $count", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun SimplePieChart(present: Int, sick: Int, permit: Int, alpha: Int, total: Int) {
    if (total == 0) {
        Canvas(modifier = Modifier.size(140.dp)) {
            drawCircle(color = Color.LightGray, style = Stroke(width = 30f))
        }
        return
    }

    val pSweep = (present.toFloat() / total) * 360f
    val sSweep = (sick.toFloat() / total) * 360f
    val iSweep = (permit.toFloat() / total) * 360f
    val aSweep = (alpha.toFloat() / total) * 360f

    // Animasi agar terlihat smooth
    val animP by animateFloatAsState(targetValue = pSweep, label = "p")
    val animS by animateFloatAsState(targetValue = sSweep, label = "s")
    val animI by animateFloatAsState(targetValue = iSweep, label = "i")
    val animA by animateFloatAsState(targetValue = aSweep, label = "a")

    Canvas(modifier = Modifier.size(140.dp)) {
        val stroke = 30f
        var startAngle = -90f

        // Hadir
        drawArc(color = Color(0xFF4CAF50), startAngle = startAngle, sweepAngle = animP, useCenter = false, style = Stroke(stroke))
        startAngle += animP

        // Sakit
        drawArc(color = Color(0xFFFFC107), startAngle = startAngle, sweepAngle = animS, useCenter = false, style = Stroke(stroke))
        startAngle += animS

        // Izin
        drawArc(color = Color(0xFF2196F3), startAngle = startAngle, sweepAngle = animI, useCenter = false, style = Stroke(stroke))
        startAngle += animI

        // Alpha
        drawArc(color = Color(0xFFF44336), startAngle = startAngle, sweepAngle = animA, useCenter = false, style = Stroke(stroke))
    }
}