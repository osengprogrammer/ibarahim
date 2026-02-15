package com.example.crashcourse.ui.checkin.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.crashcourse.ui.theme.AzuraAccent
import com.example.crashcourse.ui.theme.AzuraPrimary
import com.example.crashcourse.viewmodel.AttendanceResult // ✅ Pastikan import ini benar

/**
 * 🎫 AttendanceResultCard (V.12.5)
 * Komponen UI untuk menampilkan feedback akhir dari proses absensi.
 */
@Composable
fun AttendanceResultCard(
    result: AttendanceResult,
    activeSession: String,
    modifier: Modifier = Modifier
) {
    // 🔥 Kita buat kartu ini HANYA muncul jika hasilnya bukan Idle dan bukan sedang Menunggu Kedip
    AnimatedVisibility(
        visible = result !is AttendanceResult.Idle && result !is AttendanceResult.WaitingBlink,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier
    ) {
        val containerColor = when(result) {
            is AttendanceResult.Success -> AzuraPrimary
            is AttendanceResult.Unauthorized -> Color(0xFFD32F2F) // Merah (Salah Kelas)
            is AttendanceResult.Cooldown -> Color(0xFF454545)    // Abu Gelap (Sudah Absen)
            is AttendanceResult.Error -> Color.Black             // Hitam (Gagal Kenal)
            else -> Color.Gray
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = containerColor),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .shadow(16.dp, RoundedCornerShape(24.dp))
        ) {
            Row(
                Modifier.padding(20.dp), 
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when(result) {
                        is AttendanceResult.Success -> Icons.Default.CheckCircle
                        is AttendanceResult.Unauthorized -> Icons.Default.Block
                        is AttendanceResult.Cooldown -> Icons.Default.Timer
                        is AttendanceResult.Error -> Icons.Default.Warning
                        else -> Icons.Default.Info
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(Modifier.width(16.dp))
                
                Column {
                    val title = when(result) {
                        is AttendanceResult.Success -> "ABSENSI BERHASIL"
                        is AttendanceResult.Unauthorized -> "AKSES DITOLAK"
                        is AttendanceResult.Cooldown -> "SUDAH ABSEN"
                        is AttendanceResult.Error -> "IDENTITAS ASING"
                        is AttendanceResult.WaitingBlink -> "VERIFIKASI NYAWA"
                        else -> ""
                    }
                    
                    val nameText = when(result) {
                        is AttendanceResult.Success -> result.name
                        is AttendanceResult.Unauthorized -> result.name
                        is AttendanceResult.Cooldown -> result.name
                        is AttendanceResult.Error -> result.message
                        is AttendanceResult.WaitingBlink -> "Silakan Berkedip"
                        else -> ""
                    }
                    
                    Text(
                        text = title, 
                        style = MaterialTheme.typography.labelSmall, 
                        color = Color.White.copy(0.8f)
                    )
                    Text(
                        text = nameText, 
                        style = MaterialTheme.typography.headlineSmall, 
                        color = Color.White, 
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Tampilkan kelas aktif di bawah nama
                    Text(
                        text = "Sesi: $activeSession", 
                        style = MaterialTheme.typography.bodyMedium, 
                        color = AzuraAccent
                    )
                }
            }
        }
    }
}