package com.example.crashcourse.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.crashcourse.viewmodel.AuthViewModel
import com.example.crashcourse.viewmodel.AuthState

@Composable
fun AuthScreen(viewModel: AuthViewModel) {
    val state by viewModel.authState.collectAsState()
    var isRegisterMode by remember { mutableStateOf(false) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var schoolName by remember { mutableStateOf("") }

    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            is AuthState.Loading -> CircularProgressIndicator()
            
            is AuthState.StatusWaiting -> {
                // Tampilan saat user menunggu Aktivasi Admin
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Lock, null, modifier = Modifier.size(60.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Status Akun", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Text(
                            text = (state as AuthState.StatusWaiting).message,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { viewModel.logout() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                        Text("Logout")
                    }
                }
            }
            
            else -> {
                // Form Login / Register
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isRegisterMode) "Registrasi Baru" else "Azura Login",
                        fontSize = 28.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    if (state is AuthState.Error) {
                        Text(
                            text = (state as AuthState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    if (isRegisterMode) {
                        OutlinedTextField(
                            value = schoolName, onValueChange = { schoolName = it },
                            label = { Text("Nama Sekolah") },
                            leadingIcon = { Icon(Icons.Default.School, null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    OutlinedTextField(
                        value = email, onValueChange = { email = it },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.Email, null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = password, onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, null) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Tombol Lupa Password
                    if (!isRegisterMode) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                            TextButton(onClick = { viewModel.sendPasswordReset(email) }) {
                                Text("Lupa Password?", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (isRegisterMode) viewModel.register(email, password, schoolName)
                            else viewModel.login(email, password)
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (isRegisterMode) "DAFTAR SEKARANG" else "MASUK", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { isRegisterMode = !isRegisterMode }) {
                        Text(if (isRegisterMode) "Sudah punya akun? Login" else "Belum punya akun? Daftar")
                    }
                }
            }
        }
    }
}