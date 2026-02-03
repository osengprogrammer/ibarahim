package com.example.crashcourse.ui

import android.util.Log // ✅ FIX: Added missing Log import
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditUserScopeScreen(
    userId: String,
    onBack: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var email by remember { mutableStateOf("Loading...") }
    var selectedRole by remember { mutableStateOf("USER") }
    
    val allClassNames = remember { mutableStateListOf<String>() }
    val selectedClasses = remember { mutableStateListOf<String>() }
    
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    val filteredClassNames = if (searchQuery.isEmpty()) {
        allClassNames
    } else {
        allClassNames.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    // LaunchedEffect(userId) {
    //     try {
    //         // 1. Ambil SEMUA dokumen dari koleksi 'options' tanpa filter dulu
    //         val optionsSnapshot = db.collection("options").get().await()
            
    //         // Log untuk debug di Logcat
    //         Log.d("EditScope", "Total dokumen ditemukan di 'options': ${optionsSnapshot.size()}")

    //         val classes = optionsSnapshot.documents.map { doc ->
    //             val name = doc.getString("name") ?: ""
    //             val type = doc.getString("type") ?: "Kosong"
    //             Log.d("EditScope", "Dokumen: $name, Type: $type")
    //             name
    //         }.filter { it.isNotEmpty() }.sorted()

    //         allClassNames.clear()
    //         allClassNames.addAll(classes)

    //         // 2. Ambil Data User
    //         val userDoc = db.collection("users").document(userId).get().await()
    //         email = userDoc.getString("email") ?: ""
    //         selectedRole = userDoc.getString("role") ?: "USER"
            
    //         val assigned = userDoc.get("assigned_classes") as? List<String> ?: emptyList()
    //         selectedClasses.clear()
    //         selectedClasses.addAll(assigned)
            
    //         isLoading = false
    //     } catch (e: Exception) {
    //         Log.e("EditScope", "Error loading data", e)
    //         Toast.makeText(context, "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
    //         isLoading = false
    //     }
    // }


    LaunchedEffect(userId) {
        try {
            // 1. Ambil Semua Data Murid untuk Ekstrak Nama Kelas Unik
            val studentsSnapshot = db.collection("students").get().await()
            
            // Mengambil field 'className', menghilangkan duplikat, dan mengurutkan A-Z
            val classes = studentsSnapshot.documents
                .mapNotNull { it.getString("className") }
                .filter { it.isNotEmpty() }
                .distinct() // 🔥 Mengambil nilai unik (tidak ada kelas ganda di list)
                .sorted()

            Log.d("EditScope", "Daftar kelas unik ditemukan: $classes")

            allClassNames.clear()
            allClassNames.addAll(classes)

            // 2. Ambil Data User (Guru) yang akan diedit
            val userDoc = db.collection("users").document(userId).get().await()
            email = userDoc.getString("email") ?: ""
            selectedRole = userDoc.getString("role") ?: "USER"
            
            val assigned = userDoc.get("assigned_classes") as? List<String> ?: emptyList()
            selectedClasses.clear()
            selectedClasses.addAll(assigned)
            
            isLoading = false
        } catch (e: Exception) {
            Log.e("EditScope", "Error loading data from students", e)
            Toast.makeText(context, "Gagal memuat kelas: ${e.message}", Toast.LENGTH_SHORT).show()
            isLoading = false
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assign Kelas") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                },
                actions = {
                    if (!isLoading) {
                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        db.collection("users").document(userId).update(
                                            mapOf(
                                                "role" to selectedRole,
                                                "assigned_classes" to selectedClasses.toList()
                                            )
                                        ).await()
                                        Toast.makeText(context, "Berhasil diupdate!", Toast.LENGTH_SHORT).show()
                                        onBack()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Gagal simpan: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Simpan")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                Text("Guru: $email", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Cari Nama Kelas...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, null)
                            }
                        }
                    },
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Terpilih: ${selectedClasses.size} Kelas",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredClassNames) { className ->
                        val isChecked = selectedClasses.contains(className)
                        
                        ListItem(
                            headlineContent = { Text(className) },
                            leadingContent = {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (checked) selectedClasses.add(className)
                                        else selectedClasses.remove(className)
                                    }
                                )
                            },
                            modifier = Modifier.clickable {
                                if (isChecked) selectedClasses.remove(className)
                                else selectedClasses.add(className)
                            }
                        )
                    }
                    
                    if (filteredClassNames.isEmpty() && searchQuery.isNotEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("Kelas '$searchQuery' tidak ditemukan.", color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}