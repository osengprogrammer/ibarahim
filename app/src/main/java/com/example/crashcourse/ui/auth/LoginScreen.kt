package com.example.crashcourse.ui.auth

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.crashcourse.viewmodel.AuthViewModel
import com.example.crashcourse.viewmodel.AuthState

/**
 * 🔐 Azura Tech Login Screen
 * Synchronized with NavGraph for reactive session handling.
 */
@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit, // 🚀 REQUIRED: Matches the NavGraph contract
    viewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.authState.collectAsState()
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // 🎯 THE NAVIGATOR: Fires the moment AuthState becomes Active
    LaunchedEffect(state) {
        if (state is AuthState.Active) {
            onLoginSuccess() 
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp), 
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            is AuthState.Loading -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Memverifikasi Kredensial...", style = MaterialTheme.typography.bodySmall)
                }
            }
            
            else -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // --- HEADER ---
                    Text(
                        text = "Azura Attendance", 
                        fontSize = 30.sp, 
                        fontWeight = FontWeight.ExtraBold, 
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Sistem Absensi Wajah & Smart E-Wallet", 
                        style = MaterialTheme.typography.labelMedium, 
                        color = Color.Gray
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))

                    // --- ERROR FEEDBACK ---
                    if (state is AuthState.Error) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
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

                    // --- INPUT FIELDS ---
                    OutlinedTextField(
                        value = email, 
                        onValueChange = { email = it },
                        label = { Text("Email Resmi") },
                        leadingIcon = { Icon(Icons.Default.Email, null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = password, 
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, null) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    TextButton(
                        onClick = { 
                            if (email.isNotBlank()) {
                                viewModel.sendPasswordReset(email)
                                Toast.makeText(context, "Cek email reset password", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Masukkan email dahulu", Toast.LENGTH_SHORT).show()
                            }
                        }, 
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Lupa Password?", fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- ACTION BUTTON ---
                    Button(
                        onClick = { viewModel.login(email, password) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Text("MASUK KE DASHBOARD", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(onClick = onNavigateToRegister) {
                        Text("Belum punya akun? Registrasi Baru")
                    }
                }
            }
        }
    }
}