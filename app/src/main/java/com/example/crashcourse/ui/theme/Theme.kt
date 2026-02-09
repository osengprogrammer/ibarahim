package com.example.crashcourse.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// 🚀 Menggunakan palet warna Azura yang kita buat sebelumnya
private val DarkColorScheme = darkColorScheme(
    primary = AzuraPrimary,
    secondary = AzuraSecondary,
    tertiary = AzuraAccent,
    background = Color(0xFF121212), // Gelap modern
    surface = Color(0xFF1E1E1E),
    error = AzuraError
)

private val LightColorScheme = lightColorScheme(
    primary = AzuraPrimary,
    secondary = AzuraSecondary,
    tertiary = AzuraAccent,
    background = AzuraBg,
    surface = Color.White,
    error = AzuraError,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = AzuraText,
    onSurface = AzuraText
)

@Composable
fun CrashcourseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Set ke false jika ingin warna Azura murni (tidak mengikuti wallpaper HP)
    dynamicColor: Boolean = false, 
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}