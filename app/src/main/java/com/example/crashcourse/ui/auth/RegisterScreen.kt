package com.example.crashcourse.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle // 🔥 Import Baru
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.crashcourse.viewmodel.AuthViewModel
import com.example.crashcourse.viewmodel.AuthState

/**
 * 🏛️ Azura Tech Smart Registration Screen (V.7.5)
 * Mendukung alur: 
 * 1. Aktivasi Akun Staff (via Undangan Email & Auto-Migration)
 * 2. Pendaftaran Sekolah Baru (Admin)
 */
@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    // ✅ Menggunakan collectAsStateWithLifecycle agar sinkronisasi state lebih stabil
    val state by viewModel.authState.collectAsStateWithLifecycle()
    
    // --- UI STATES ---
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var schoolName by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp), 
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // --- HEADER ---
            Text(
                text = "Aktivasi Akun", 
                fontSize = 32.sp, 
                fontWeight = FontWeight.ExtraBold, 
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Gunakan email yang terdaftar di sistem Azura.", 
                style = MaterialTheme.typography.bodySmall, 
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // --- ERROR FEEDBACK ---
            if (state is AuthState.Error) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Text(
                        text = (state as AuthState.Error).message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // --- INPUT: EMAIL ---
            OutlinedTextField(
                value = email, 
                onValueChange = { email = it }, 
                label = { Text("Email Resmi") }, 
                placeholder = { Text("contoh@sekolah.com") },
                leadingIcon = { Icon(Icons.Default.Email, null) }, 
                modifier = Modifier.fillMaxWidth(), 
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                enabled = state !is AuthState.Loading
            )

            Spacer(modifier = Modifier.height(12.dp))

            // --- INPUT: NAMA SEKOLAH ---
            OutlinedTextField(
                value = schoolName, 
                onValueChange = { schoolName = it }, 
                label = { Text("Nama Sekolah") }, 
                placeholder = { Text("Hanya untuk Admin baru") },
                supportingText = { 
                    Text("Kosongkan jika Anda Staff/Guru yang diundang Admin.") 
                },
                leadingIcon = { Icon(Icons.Default.School, null) }, 
                modifier = Modifier.fillMaxWidth(), 
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                enabled = state !is AuthState.Loading
            )

            Spacer(modifier = Modifier.height(12.dp))

            // --- INPUT: PASSWORD ---
            OutlinedTextField(
                value = password, 
                onValueChange = { password = it }, 
                label = { Text("Buat Password") }, 
                leadingIcon = { Icon(Icons.Default.Lock, null) }, 
                visualTransformation = PasswordVisualTransformation(), 
                modifier = Modifier.fillMaxWidth(), 
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                enabled = state !is AuthState.Loading
            )

            Spacer(modifier = Modifier.height(32.dp))

            // --- ACTION BUTTON ---
            if (state is AuthState.Loading) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = (state as? AuthState.Loading)?.message ?: "Memproses...",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            } else {
                Button(
                    onClick = { 
                        if (email.isNotBlank() && password.isNotBlank()) {
                            viewModel.register(email, password, schoolName) 
                        }
                    }, 
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp), 
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Text("AKTIFKAN AKUN SAYA", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- NAVIGASI KE LOGIN ---
            TextButton(
                onClick = onNavigateToLogin,
                enabled = state !is AuthState.Loading
            ) {
                Text("Sudah punya akun aktif? Login di sini")
            }
        }
    }
}