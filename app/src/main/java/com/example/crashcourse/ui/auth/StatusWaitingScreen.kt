package com.example.crashcourse.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.crashcourse.viewmodel.AuthViewModel
import com.example.crashcourse.viewmodel.AuthState

@Composable
fun StatusWaitingScreen(viewModel: AuthViewModel = viewModel()) {
    val state by viewModel.authState.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.Lock, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Verifikasi Akun", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        
        if (state is AuthState.StatusWaiting) {
            Card(modifier = Modifier.padding(16.dp)) {
                Text((state as AuthState.StatusWaiting).message, modifier = Modifier.padding(16.dp))
            }
        }

        Button(onClick = { viewModel.logout() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
            Text("Batal & Keluar")
        }
    }
}