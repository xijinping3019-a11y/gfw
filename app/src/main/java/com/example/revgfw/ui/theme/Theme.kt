package com.example.revgfw.ui.theme

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

/** 品牌色：深青绿系，象征"防护/审计" */
private val Seed = Color(0xFF00897B)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4DB6AC),
    onPrimary = Color(0xFF00332E),
    primaryContainer = Color(0xFF005049),
    secondary = Color(0xFF80CBC4),
    tertiary = Color(0xFFFFB74D),
    background = Color(0xFF101416),
    surface = Color(0xFF161B1D),
    surfaceVariant = Color(0xFF29302F),
    error = Color(0xFFFF6E6E),
)

private val LightColors = lightColorScheme(
    primary = Seed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB2DFDB),
    secondary = Color(0xFF4DB6AC),
    tertiary = Color(0xFFEF6C00),
    background = Color(0xFFF7FAF9),
    surface = Color.White,
    surfaceVariant = Color(0xFFE3EDEA),
    error = Color(0xFFC62828),
)

@Composable
fun RevGfwTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = scheme, content = content)
}