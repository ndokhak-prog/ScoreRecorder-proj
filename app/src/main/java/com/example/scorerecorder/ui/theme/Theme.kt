package com.example.scorerecorder.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat

/** 세련된 네이비 + 골드 팔레트 */
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF123A63),          // 딥 네이비
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E4F4),
    onPrimaryContainer = Color(0xFF0B2440),
    secondary = Color(0xFFC9A24B),        // 골드
    onSecondary = Color(0xFF2B2303),
    secondaryContainer = Color(0xFFF3E7C9),
    onSecondaryContainer = Color(0xFF332802),
    tertiary = Color(0xFF5E7A9B),         // 차분한 스틸블루
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDCE8F2),
    onTertiaryContainer = Color(0xFF1E3A55),
    background = Color(0xFFF6F4EE),       // 따뜻한 페이퍼
    onBackground = Color(0xFF1C1C1A),
    surface = Color.White,
    onSurface = Color(0xFF1C1C1A),
    surfaceVariant = Color(0xFFECE7DC),
    onSurfaceVariant = Color(0xFF5F5B52),
    outline = Color(0xFF8B867B),
    outlineVariant = Color(0xFFD8D3C7),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B)
)

/** 글자 크기 확대 배율 (개행/스크롤이 생기지 않는 범위에서 최대한 크게) */
private const val TEXT_SCALE = 1.2f

@Composable
fun ScoreRecorderTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, density.fontScale * TEXT_SCALE)
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography(),
            content = content
        )
    }
}
