package com.example.crashcourse.ui.checkin

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.crashcourse.db.CheckInRecord
import com.example.crashcourse.ui.theme.*
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CheckInRecordCard(
    record: CheckInRecord,
    subClassName: String? = null,
    subGradeName: String? = null,
    programName: String? = null,
    onLongClick: () -> Unit
) {
    // Warna Status
    val statusColor = when (record.status.uppercase()) {
        "PRESENT" -> AzuraSuccess
        "SAKIT" -> Color(0xFFFFC107) 
        "IZIN" -> AzuraSecondary
        "ALPHA" -> AzuraError
        else -> Color.Gray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 12.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        // 🚀 KITA PAKAI WARNA GELAP UNTUK KARTU (Surface/Dark)
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E) // Hitam Elegan
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { /* Detail */ },
                    onLongClick = onLongClick
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- 1. STATUS BADGE ---
            Surface(
                color = statusColor,
                shape = CircleShape,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = record.status.take(1).uppercase(),
                        color = Color.White, // Tetap putih
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // --- 2. STUDENT INFO (TEXT WARNA PUTIH) ---
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 🚀 NAMA SISWA - SEKARANG PUTIH!
                    Text(
                        text = if (record.name.isNotBlank()) record.name else "ID: ${record.studentId}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White, // 🔥 GANTI KE PUTIH
                        maxLines = 1
                    )
                    
                    // WAKTU - ABU TERANG
                    Text(
                        text = record.timestamp.format(DateTimeFormatter.ofPattern("HH:mm")),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f) // 🔥 PUTIH TRANSPARAN
                    )
                }

                // KELAS & GRADE - PUTIH SOFT
                val classDetail = buildString {
                    append(record.className ?: "Umum")
                    if (!subClassName.isNullOrEmpty()) append(" ($subClassName)")
                }
                
                Text(
                    text = "Kelas: $classDetail",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f) // 🔥 PUTIH SOFT
                )

                if (!programName.isNullOrEmpty()) {
                    Text(
                        text = programName,
                        style = MaterialTheme.typography.labelSmall,
                        color = AzuraPrimary, // Warna Biru Azura (Tetap mencolok)
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!record.note.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "📝 ${record.note}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray // 🔥 ABU TERANG
                    )
                }
            }

            // --- 3. INDICATOR MANUAL ---
            if (record.faceId == null) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.EditNote,
                    contentDescription = "Manual",
                    tint = Color.White.copy(alpha = 0.5f), // 🔥 ICON PUTIH SOFT
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}