package com.example.crashcourse.ui.checkin.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.crashcourse.ui.theme.AzuraAccent
import com.example.crashcourse.ui.theme.AzuraPrimary

@Composable
fun CheckInLoadingView(modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = AzuraPrimary)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Menyiapkan AI Scanner...", color = Color.White)
    }
}

@Composable
fun CheckInHUD(
    schoolName: String,
    statusMessage: String,
    isFlashOn: Boolean,
    onFlashToggle: () -> Unit,
    onCameraFlip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Info Sekolah & Status
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(0.6f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = schoolName.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = AzuraAccent,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = statusMessage,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Tombol Kontrol
        Row {
            IconButton(
                onClick = onFlashToggle,
                modifier = Modifier.background(
                    if (isFlashOn) Color.Yellow.copy(0.3f) else Color.Black.copy(0.6f),
                    RoundedCornerShape(50)
                )
            ) {
                Icon(Icons.Outlined.WbSunny, null, tint = if (isFlashOn) Color.Yellow else Color.White)
            }
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = onCameraFlip,
                modifier = Modifier.background(Color.Black.copy(0.6f), RoundedCornerShape(50))
            ) {
                Icon(Icons.Default.FlipCameraAndroid, null, tint = Color.White)
            }
        }
    }
}