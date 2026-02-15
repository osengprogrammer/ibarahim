package com.example.crashcourse.ui.auth

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crashcourse.viewmodel.AuthViewModel
import com.example.crashcourse.viewmodel.AuthState

private const val TAG = "AzuraLoginUI"

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    viewModel: AuthViewModel
) {
    val context = LocalContext.current
    val state by viewModel.authState.collectAsStateWithLifecycle()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

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
            Text(
                text = "Azura Attendance",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(40.dp))

            // --- DEBUG INFO (Akan muncul di Logcat setiap Re-composition) ---
            LaunchedEffect(state) {
                Log.d(TAG, "UI Observed State: ${state::class.java.simpleName}")
            }

            if (state is AuthState.Error) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Text(
                        text = (state as AuthState.Error).message,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = state !is AuthState.Loading
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = state !is AuthState.Loading
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (state is AuthState.Loading) {
                CircularProgressIndicator()
                Text(
                    text = (state as? AuthState.Loading)?.message ?: "Sedang masuk...",
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                Button(
                    onClick = {
                        // 🔥 TRIGGER LOGCAT MANUAL
                        Log.i(TAG, "🔘 Login Button Clicked! Email: $email")
                        
                        if (email.isBlank() || password.isBlank()) {
                            Log.w(TAG, "⚠️ Validation failed: Empty fields")
                            Toast.makeText(context, "Isi email dan password!", Toast.LENGTH_SHORT).show()
                        } else {
                            Log.d(TAG, "🚀 Calling viewModel.login()...")
                            viewModel.login(email.trim(), password.trim())
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("MASUK")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onNavigateToRegister) {
                Text("Belum punya akun? Daftar")
            }
        }
    }
}