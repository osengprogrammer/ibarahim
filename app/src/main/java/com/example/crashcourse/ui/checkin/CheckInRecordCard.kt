package com.example.crashcourse.ui.checkin

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.crashcourse.db.*
import com.example.crashcourse.viewmodel.OptionsViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CheckInRecordCard(
    record: CheckInRecord,
    onLongClick: () -> Unit // Dibutuhkan untuk memicu dialog Edit/Delete
) {
    val optionsViewModel: OptionsViewModel = viewModel()
    
    // 1. Collect Options dari Flow (Lokal Room)
    val subClassOptions by optionsViewModel.subClassOptions.collectAsStateWithLifecycle(emptyList())
    val subGradeOptions by optionsViewModel.subGradeOptions.collectAsStateWithLifecycle(emptyList())
    val programOptions by optionsViewModel.programOptions.collectAsStateWithLifecycle(emptyList())
    val roleOptions by optionsViewModel.roleOptions.collectAsStateWithLifecycle(emptyList())
    
    // 2. Lookup Nama berdasarkan ID yang tersimpan di record
    val subClassName = remember(record.subClassId, subClassOptions) {
        subClassOptions.find { it.id == record.subClassId }?.name
    }
    val subGradeName = remember(record.subGradeId, subGradeOptions) {
        subGradeOptions.find { it.id == record.subGradeId }?.name
    }
    val programName = remember(record.programId, programOptions) {
        programOptions.find { it.id == record.programId }?.name
    }
    val roleName = remember(record.roleId, roleOptions) {
        roleOptions.find { it.id == record.roleId }?.name
    }

    // 3. Tentukan Warna Badge berdasarkan Status (S, I, A, P)
    val statusColor = when (record.status) {
        "PRESENT" -> Color(0xFF4CAF50) // Hijau
        "SAKIT" -> Color(0xFFFFC107)   // Kuning
        "IZIN" -> Color(0xFF2196F3)    // Biru
        "ALPHA" -> Color(0xFFF44336)   // Merah
        else -> Color.Gray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
            // ✅ Menggunakan combinedClickable agar bisa tekan lama untuk EDIT
            .combinedClickable(
                onClick = { /* Bisa ditambahkan navigasi ke detail jika perlu */ },
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Badge Status Lingkaran
            Surface(
                color = statusColor,
                shape = CircleShape,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = record.status.take(1),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = record.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = record.timestamp.format(DateTimeFormatter.ofPattern("HH:mm")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Info Kelas & Grade
                val classInfo = buildString {
                    append(record.className ?: "-")
                    if (subClassName != null) append(" ($subClassName)")
                }
                val gradeInfo = buildString {
                    append(record.gradeName ?: "-")
                    if (subGradeName != null) append(" ($subGradeName)")
                }

                Text(
                    text = "Kelas: $classInfo | Grade: $gradeInfo",
                    style = MaterialTheme.typography.bodySmall
                )

                if (programName != null || roleName != null) {
                    Text(
                        text = "${programName ?: ""} ${if (roleName != null) "- $roleName" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Tampilkan Catatan jika ada (Misal: "Sakit demam")
                if (!record.note.isNullOrBlank()) {
                    Text(
                        text = "Note: ${record.note}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            // Indikator jika input manual (Bukan hasil Scan Wajah)
            if (record.faceId == null) {
                Icon(
                    imageVector = Icons.Default.EditNote,
                    contentDescription = "Manual Entry",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Helper untuk mendapatkan warna status di luar Composable jika dibutuhkan
 */
fun getStatusColor(status: String): Color = when (status) {
    "PRESENT" -> Color(0xFF4CAF50)
    "SAKIT" -> Color(0xFFFFC107)
    "IZIN" -> Color(0xFF2196F3)
    "ALPHA" -> Color(0xFFF44336)
    else -> Color.Gray
}