package com.example.crashcourse.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crashcourse.firestore.core.FirestorePaths
import com.example.crashcourse.ui.theme.AzuraPrimary
import com.example.crashcourse.utils.Constants
import com.example.crashcourse.viewmodel.AuthState
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * 🔐 EditUserScopeScreen (Final & Stable)
 * Mengelola "Wilayah Kuasa" (Scope) akses Rombel untuk Akun Staff/Guru.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditUserScopeScreen(
    userId: String,
    authState: AuthState.Active,
    onBack: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // State Data User
    var targetDocId by remember { mutableStateOf("") }
    var targetEmail by remember { mutableStateOf("Mencari user...") }
    
    // State Kelas
    val allClassNames = remember { mutableStateListOf<String>() }
    val selectedClasses = remember { mutableStateListOf<String>() }
    
    // Debug & UI State
    var debugMessage by remember { mutableStateOf("Memuat data...") }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    // Logic Search Filter
    val filteredClasses = remember(searchQuery, allClassNames.toList()) {
        if (searchQuery.isEmpty()) allClassNames
        else allClassNames.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    // --- 📥 1. LOAD DATA ---
    LaunchedEffect(userId) {
        try {
            isLoading = true
            debugMessage = "Sekolah ID: '${authState.sekolahId}'"
            
            // A. AMBIL MASTER ROMBEL DARI SEKOLAH INI
            val masterSnapshot = db.collection(FirestorePaths.MASTER_CLASSES)
                .whereEqualTo(Constants.KEY_SEKOLAH_ID, authState.sekolahId)
                .get().await()
            
            if (masterSnapshot.isEmpty) {
                debugMessage += "\n❌ HASIL KOSONG! Admin belum membuat Rombel."
            }

            val classes = masterSnapshot.documents
                .mapNotNull { doc -> 
                    // Coba berbagai kemungkinan nama field agar tidak null
                    val rawVal = doc.get(Constants.PILLAR_CLASS) 
                        ?: doc.get("name") 
                        ?: doc.get("nama") 
                        ?: doc.id 
                    rawVal?.toString() 
                }
                .distinct()
                .sorted()

            allClassNames.clear()
            allClassNames.addAll(classes)

            // B. CARI USER DENGAN SMART SEARCH (ID atau Email)
            val userCol = db.collection(FirestorePaths.USERS)
            var userSnapshot = try {
                // 1. Coba cari pakai ID Dokumen dulu (Prioritas Utama)
                val doc = userCol.document(userId).get().await()
                if (doc.exists()) doc else null
            } catch (e: Exception) { null }

            // 2. Jika tidak ketemu, cari berdasarkan field 'email'
            if (userSnapshot == null) {
                val queryEmail = userCol.whereEqualTo("email", userId).get().await()
                if (!queryEmail.isEmpty) {
                    userSnapshot = queryEmail.documents.first()
                } else {
                    // 3. Coba cari by UID kalau-kalau userId yg dikirim adalah UID
                    val queryUid = userCol.whereEqualTo("uid", userId).get().await()
                    if (!queryUid.isEmpty) userSnapshot = queryUid.documents.first()
                }
            }

            if (userSnapshot != null && userSnapshot.exists()) {
                targetDocId = userSnapshot.id // ID Dokumen Firestore yang asli
                targetEmail = userSnapshot.getString("email") ?: "Tanpa Email"
                
                // Ambil assigned_classes yang sudah ada
                val assignedRaw = userSnapshot.get("assigned_classes") as? List<*> ?: emptyList<Any>()
                selectedClasses.clear()
                selectedClasses.addAll(assignedRaw.map { it.toString() })
            } else {
                debugMessage += "\n❌ User tidak ditemukan: $userId"
                targetEmail = "User Tidak Ditemukan"
            }
            
        } catch (e: Exception) {
            debugMessage = "CRITICAL ERROR: ${e.message}"
            Toast.makeText(context, "Error Load: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Atur Scope Guru", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            if (isLoading) "Memuat data..." else targetEmail, 
                            style = MaterialTheme.typography.labelSmall, 
                            color = AzuraPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali") 
                    }
                },
                actions = {
                    // --- TOMBOL SAVE AMAN ---
                    Button(
                        onClick = {
                            if (targetDocId.isBlank()) {
                                Toast.makeText(context, "User belum termuat!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            
                            scope.launch {
                                isSaving = true
                                try {
                                    // 🔥 FIX: Menggunakan HashMap explisit & SetOptions.merge()
                                    // Ini mencegah error type inference dan aman untuk dokumen parsial
                                    val updates = hashMapOf<String, Any>(
                                        "assigned_classes" to selectedClasses.toList()
                                    )
                                    
                                    db.collection(FirestorePaths.USERS)
                                        .document(targetDocId)
                                        .set(updates, SetOptions.merge()) 
                                        .await()
                                        
                                    Toast.makeText(context, "✅ Scope Berhasil Disimpan!", Toast.LENGTH_SHORT).show()
                                    onBack()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "❌ Gagal: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally { 
                                    isSaving = false 
                                }
                            }
                        },
                        // Disable tombol saat loading, saving, atau user tidak ditemukan
                        enabled = !isLoading && !isSaving && targetDocId.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = AzuraPrimary),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp), 
                                color = Color.White, 
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Menyimpan...")
                        } else {
                            Icon(Icons.Default.Save, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Simpan")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(16.dp)
        ) {
            // --- DIAGNOSTIK DB (Hanya muncul jika Rombel Kosong) ---
            if (allClassNames.isEmpty() && !isLoading) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2D3436)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BugReport, null, tint = Color.Yellow)
                            Spacer(Modifier.width(8.dp))
                            Text("DIAGNOSTIK SISTEM", color = Color.Yellow, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = debugMessage, 
                            color = Color.White, 
                            fontSize = 11.sp, 
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // --- SEARCH BAR ---
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Cari rombel...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White, 
                    unfocusedContainerColor = Color.White
                ),
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))
            
            // --- HEADER LIST & SELECT ALL ---
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { 
                    CircularProgressIndicator(color = AzuraPrimary) 
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (searchQuery.isEmpty()) "Pilih Unit (${selectedClasses.size})" else "Hasil Pencarian",
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (allClassNames.isNotEmpty()) {
                        TextButton(onClick = {
                            if (selectedClasses.containsAll(filteredClasses)) {
                                selectedClasses.removeAll(filteredClasses)
                            } else {
                                // Hanya tambahkan yg belum ada agar tidak duplikat
                                val newItems = filteredClasses.filter { !selectedClasses.contains(it) }
                                selectedClasses.addAll(newItems)
                            }
                        }) {
                            Text(if (selectedClasses.containsAll(filteredClasses)) "Lepas Semua" else "Pilih Semua")
                        }
                    }
                }
                
                Spacer(Modifier.height(8.dp))

                // --- DAFTAR KELAS (LAZY COLUMN) ---
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(items = filteredClasses, key = { it }) { className ->
                        val isSelected = selectedClasses.contains(className)
                        ClassSelectionItem(className, isSelected) {
                            if (isSelected) selectedClasses.remove(className)
                            else selectedClasses.add(className)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClassSelectionItem(className: String, isSelected: Boolean, onToggle: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) AzuraPrimary.copy(alpha = 0.1f) else Color.White
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        border = if (isSelected) BorderStroke(1.5.dp, AzuraPrimary) else BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Row(
            modifier = Modifier.padding(16.dp), 
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                contentDescription = null,
                tint = if (isSelected) AzuraPrimary else Color.Gray
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = className,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) AzuraPrimary else Color.Black,
                fontSize = 15.sp
            )
        }
    }
}