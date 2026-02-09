package com.example.crashcourse.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.crashcourse.db.*

@Composable
fun StudentFormBody(
    name: String, onNameChange: (String) -> Unit,
    
    // State Pilihan (Object-based agar ID aman)
    selectedClass: ClassOption?, onClassChange: (ClassOption?) -> Unit,
    selectedSubClass: SubClassOption?, onSubClassChange: (SubClassOption?) -> Unit,
    
    selectedGrade: GradeOption?, onGradeChange: (GradeOption?) -> Unit,
    selectedSubGrade: SubGradeOption?, onSubGradeChange: (SubGradeOption?) -> Unit,
    
    selectedProgram: ProgramOption?, onProgramChange: (ProgramOption?) -> Unit,
    selectedRole: RoleOption?, onRoleChange: (RoleOption?) -> Unit,

    // Sumber Data Master
    classOptions: List<ClassOption>,
    subClassOptions: List<SubClassOption>,
    gradeOptions: List<GradeOption>,
    subGradeOptions: List<SubGradeOption>,
    programOptions: List<ProgramOption>,
    roleOptions: List<RoleOption>
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        
        // Nama
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Nama Lengkap") },
            modifier = Modifier.fillMaxWidth()
        )

        // Row 1: Kelas & Sub-Kelas
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                AzuraDropdown(
                    label = "Kelas",
                    options = classOptions,
                    selected = selectedClass,
                    onSelected = { 
                        onClassChange(it)
                        onSubClassChange(null) // Reset sub-class saat kelas berubah
                    },
                    itemLabel = { it.name }
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                // 🚀 LOGIKA FILTER TERPUSAT DI SINI
                val filteredSubClasses = subClassOptions.filter { it.parentClassId == selectedClass?.id }
                AzuraDropdown(
                    label = "Sub-Kelas",
                    options = filteredSubClasses,
                    selected = selectedSubClass,
                    onSelected = onSubClassChange,
                    itemLabel = { it.name }
                )
            }
        }

        // Row 2: Grade & Sub-Grade
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                AzuraDropdown(
                    label = "Grade",
                    options = gradeOptions,
                    selected = selectedGrade,
                    onSelected = { 
                        onGradeChange(it)
                        onSubGradeChange(null) // Reset sub-grade
                    },
                    itemLabel = { it.name }
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                // 🚀 LOGIKA FILTER TERPUSAT DI SINI
                val filteredSubGrades = subGradeOptions.filter { it.parentGradeId == selectedGrade?.id }
                AzuraDropdown(
                    label = "Sub-Grade",
                    options = filteredSubGrades,
                    selected = selectedSubGrade,
                    onSelected = onSubGradeChange,
                    itemLabel = { it.name }
                )
            }
        }

        // Row 3: Program & Role
        AzuraDropdown(
            label = "Program",
            options = programOptions,
            selected = selectedProgram,
            onSelected = onProgramChange,
            itemLabel = { it.name }
        )

        AzuraDropdown(
            label = "Role / Jabatan",
            options = roleOptions,
            selected = selectedRole,
            onSelected = onRoleChange,
            itemLabel = { it.name }
        )
    }
}