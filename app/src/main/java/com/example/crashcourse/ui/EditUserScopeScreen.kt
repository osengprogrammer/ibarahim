package com.example.crashcourse.ui

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.crashcourse.utils.Constants // 🚀 Gunakan Constants
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

    // State Data
    var email by remember { mutableStateOf("Loading...") }
    var selectedRole by remember { mutableStateOf("USER") } // Default
    
    // State List Kelas
    val allClassNames = remember { mutableStateListOf<String>() }
    val selectedClasses = remember { mutableStateListOf<String>() }
    
    // UI State
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) } // 🚀 Status Saving

    // Filter Logic
    val filteredClassNames = remember(searchQuery, allClassNames) {
        if (searchQuery.isEmpty()) allClassNames
        else allClassNames.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    // Load Data
    LaunchedEffect(userId) {
        isLoading = true
        try {
            // 1. Ambil Daftar Kelas Unik dari Koleksi Students (Bukan Options)
            // Ini memastikan kelas yang muncul adalah yang BENAR-BENAR ADA muridnya
            val studentsSnapshot = db.collection(Constants.COLL_STUDENTS).get().await()
            
            val classes = studentsSnapshot.documents
                .mapNotNull { it.getString("className") }
                .filter { it.isNotEmpty() }
                .distinct()
                .sorted()

            allClassNames.clear()
            allClassNames.addAll(classes)
            Log.d("EditScope", "Kelas ditemukan: ${classes.size}")

            // 2. Ambil Data User Target
            val userDoc = db.collection(Constants.COLL_USERS).document(userId).get().await()
            email = userDoc.getString("email") ?: "No Email"
            selectedRole = userDoc.getString("role") ?: "USER"
            
            val assigned = userDoc.get("assigned_classes") as? List<String> ?: emptyList()
            selectedClasses.clear()
            selectedClasses.addAll(assigned)
            
        } catch (e: Exception) {
            Log.e("EditScope", "Gagal load data", e)
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Akses Guru", fontWeight = FontWeight.Bold)
                        Text(email, style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali") 
                    }
                },
                actions = {
                    if (!isLoading && !isSaving) {
                        Button(
                            onClick = {
                                scope.launch {
                                    isSaving = true
                                    try {
                                        db.collection(Constants.COLL_USERS).document(userId).update(
                                            mapOf(
                                                "role" to selectedRole,
                                                "assigned_classes" to selectedClasses.toList()
                                            )
                                        ).await()
                                        
                                        Toast.makeText(context, "Akses Disimpan!", Toast.LENGTH_SHORT).show()
                                        onBack()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                                        isSaving = false
                                    }
                                }
                            },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Simpan")
                        }
                    } else if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 16.dp).size(24.dp),
                            strokeWidth = 2.dp
                        )
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
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize()
            ) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Cari Kelas...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, null)
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Status Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Kelas Terpilih: ${selectedClasses.size}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    if (selectedClasses.isNotEmpty()) {
                        TextButton(onClick = { selectedClasses.clear() }) {
                            Text("Reset")
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // List Kelas
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(items = filteredClassNames, key = { it }) { className ->
                        val isChecked = selectedClasses.contains(className)
                        
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = if (isChecked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isChecked) selectedClasses.remove(className)
                                    else selectedClasses.add(className)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (checked) selectedClasses.add(className)
                                        else selectedClasses.remove(className)
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = className,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                    
                    if (filteredClassNames.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if(searchQuery.isEmpty()) "Tidak ada data kelas." else "Kelas '$searchQuery' tidak ditemukan.",
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}