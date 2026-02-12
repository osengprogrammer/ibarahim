package com.example.crashcourse.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.crashcourse.db.MasterClassWithNames

@Composable
fun RombelSelectionDialog(
    allClasses: List<MasterClassWithNames>,
    alreadySelected: List<MasterClassWithNames>,
    onDismiss: () -> Unit,
    onSave: (List<MasterClassWithNames>) -> Unit
) {
    // State lokal untuk menampung pilihan sementara sebelum klik 'Simpan'
    val tempSelected = remember { 
        mutableStateListOf<MasterClassWithNames>().apply { addAll(alreadySelected) } 
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih Rombel / Kelas") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp) // Batasi tinggi dialog
                    .verticalScroll(rememberScrollState())
            ) {
                if (allClasses.isEmpty()) {
                    Text("Tidak ada data kelas tersedia.", modifier = Modifier.padding(16.dp))
                }

                allClasses.forEach { rombel ->
                    val isSelected = tempSelected.any { it.classId == rombel.classId }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isSelected) {
                                    tempSelected.removeIf { it.classId == rombel.classId }
                                } else {
                                    tempSelected.add(rombel)
                                }
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                if (checked == true) tempSelected.add(rombel)
                                else tempSelected.removeIf { it.classId == rombel.classId }
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = rombel.className)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(tempSelected.toList()) }) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}