package com.example.crashcourse.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle // 🚀 Tambahkan ini
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crashcourse.ui.theme.AzuraPrimary
import com.example.crashcourse.ui.theme.AzuraText

@Composable
fun AzuraInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, fontWeight = FontWeight.Medium) },
            modifier = Modifier.fillMaxWidth(),
            // 🚀 MEMAKSA TEKS MENJADI HITAM PEKAT & TEBAL
            textStyle = TextStyle(
                color = AzuraText, 
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold 
            ),
            leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null, tint = AzuraPrimary) } },
            trailingIcon = trailingIcon,
            isError = isError,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            singleLine = singleLine,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                // 🚀 WARNA TEKS SAAT DIKETIK
                focusedTextColor = AzuraText,
                unfocusedTextColor = AzuraText,
                
                // WARNA BORDER
                focusedBorderColor = AzuraPrimary,
                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                
                // WARNA LABEL (TEXT DI ATAS)
                focusedLabelColor = AzuraPrimary,
                unfocusedLabelColor = AzuraText.copy(alpha = 0.6f),
                
                // WARNA KURSOR
                cursorColor = AzuraPrimary
            )
        )
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
            )
        }
    }
}