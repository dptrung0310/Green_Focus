package com.example.greenfocus.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── Green-themed colour scheme ──────────────────────────────────────
private val GreenLightColorScheme = lightColorScheme(
    primary          = Color(0xFF2E7D32),   // xanh lá đậm — nút, outline, indicator
    onPrimary        = Color.White,
    primaryContainer = Color(0xFFA5D6A7),   // nền chip / container nhạt
    onPrimaryContainer = Color(0xFF003300),

    secondary        = Color(0xFF4CAF50),   // xanh lá tươi
    onSecondary      = Color.White,
    secondaryContainer = Color(0xFFC8E6C9),
    onSecondaryContainer = Color(0xFF002200),

    tertiary         = Color(0xFF81C784),
    onTertiary       = Color.White,

    background       = Color(0xFFFAF7EC),   // nền kem nhẹ (đồng bộ toàn app)
    onBackground     = Color(0xFF1C1B1F),

    surface          = Color(0xFFFFFFFF),
    onSurface        = Color(0xFF1C1B1F),
    surfaceVariant   = Color(0xFFE8F5E9),
    onSurfaceVariant = Color(0xFF4A4A4A),

    outline          = Color(0xFF2E7D32),   // viền TextField khi focused
    outlineVariant   = Color(0xFFB0BEC5),   // viền TextField khi unfocused

    error            = Color(0xFFD32F2F),
    onError          = Color.White,
)

private val GreenDarkColorScheme = darkColorScheme(
    primary          = Color(0xFF81C784),
    onPrimary        = Color(0xFF003300),
    primaryContainer = Color(0xFF2E7D32),
    onPrimaryContainer = Color(0xFFC8E6C9),

    secondary        = Color(0xFFA5D6A7),
    onSecondary      = Color(0xFF002200),

    background       = Color(0xFF121212),
    onBackground     = Color(0xFFE6E1E5),

    surface          = Color(0xFF1E1E1E),
    onSurface        = Color(0xFFE6E1E5),

    error            = Color(0xFFCF6679),
    onError          = Color(0xFF370001),
)

@Composable
fun GreenFocusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is DISABLED: always use the app's own green palette
    // regardless of wallpaper on Android 12+ devices.
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) GreenDarkColorScheme else GreenLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.primary.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}