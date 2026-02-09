package com.example.crashcourse.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.crashcourse.ui.theme.AzuraPrimary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AzuraDatePicker(
    label: String,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    // 🚀 TAMBAHKAN PARAMETER INI:
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null
) {
    var showDialog by remember { mutableStateOf(false) }
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

    OutlinedButton(
        onClick = { showDialog = true },
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = AzuraPrimary)
    ) {
        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = selectedDate?.format(formatter) ?: label,
            style = MaterialTheme.typography.bodyMedium
        )
    }

    if (showDialog) {
        // 🚀 LOGIKA PEMBATASAN KALENDER
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val date = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneId.of("UTC")).toLocalDate()
                    
                    val isAfterMin = minDate == null || !date.isBefore(minDate)
                    val isBeforeMax = maxDate == null || !date.isAfter(maxDate)
                    
                    return isAfterMin && isBeforeMax
                }

                override fun isSelectableYear(year: Int): Boolean {
                    val minYear = minDate?.year ?: 0
                    val maxYear = maxDate?.year ?: 3000
                    return year in minYear..maxYear
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val date = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                        onDateSelected(date)
                    }
                    showDialog = false
                }) { Text("Pilih", color = AzuraPrimary) }
            },
            dismissButton = {
                TextButton(onClick = { 
                    onDateSelected(null) 
                    showDialog = false 
                }) { Text("Hapus") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}