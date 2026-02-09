package com.example.crashcourse.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width // 🚀 PASTIKAN IMPORT INI ADA
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.example.crashcourse.ui.theme.AzuraPrimary
import com.example.crashcourse.ui.theme.AzuraText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> AzuraDropdown(
    label: String,
    options: List<T>,
    selected: T?,
    onSelected: (T?) -> Unit,
    itemLabel: (T) -> String,
    modifier: Modifier = Modifier,
    showClearOption: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    var textFieldSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
    val density = LocalDensity.current // Pindahkan ke atas biar rapi

    Box(modifier = modifier) {
        OutlinedTextField(
            value = selected?.let { itemLabel(it) } ?: "",
            onValueChange = {},
            label = { Text(label, fontWeight = FontWeight.Medium) },
            readOnly = true,
            textStyle = TextStyle(
                color = AzuraText,
                fontWeight = FontWeight.SemiBold
            ),
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = AzuraPrimary
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    textFieldSize = coordinates.size.toSize()
                },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = AzuraText,
                unfocusedTextColor = AzuraText,
                focusedBorderColor = AzuraPrimary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedLabelColor = AzuraPrimary
            )
        )

        // 🚀 SOLUSI: Gunakan modifier terpisah untuk menghindari ambiguitas
        val dropdownModifier = Modifier.width(
            with(density) { textFieldSize.width.toDp() }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = dropdownModifier // Pakai modifier yang sudah kita buat
        ) {
            if (showClearOption) {
                DropdownMenuItem(
                    text = { Text("Semua $label", color = AzuraPrimary, fontWeight = FontWeight.Bold) },
                    onClick = {
                        onSelected(null)
                        expanded = false
                    }
                )
            }
            options.forEach { option ->
                DropdownMenuItem(
                    text = { 
                        Text(
                            text = itemLabel(option),
                            color = AzuraText,
                            fontWeight = FontWeight.Medium
                        ) 
                    },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}