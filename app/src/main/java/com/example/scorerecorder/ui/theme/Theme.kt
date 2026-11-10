package com.example.scorerecorder.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

/**
 * Sovereign Modernism — Deep Navy 테마
 *
 * - 캔버스: 쿨 포슬린(#F8FAFC), 카드: 화이트 + 1px 보더(#E2E8F0)
 * - 포인트: 딥네이비(#0D1B2A) + 코발트(#2563EB)
 * - 상태: 긍정 제이드(#059669), 경고 앰버(#D97706), 오류 카민(#DC2626)
 */
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0D1B2A),              // 딥 네이비 (브랜드/제목/주 버튼)
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF1B2A4A),     // 네이비 라이트 (선택 상태)
    onPrimaryContainer = Color(0xFFD6E4F9),
    inversePrimary = Color(0xFFBAC8DC),
    secondary = Color(0xFF2563EB),            // 코발트 액센트 (확인/활성)
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDBEAFE),
    onSecondaryContainer = Color(0xFF1E3A8A),
    tertiary = Color(0xFF059669),             // 제이드 (긍정/우승)
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Color(0xFF065F46),
    background = Color(0xFFF8FAFC),           // 쿨 포슬린 캔버스
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),              // 카드/모듈
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),       // 서브 컨테이너
    onSurfaceVariant = Color(0xFF64748B),     // 뮤트 텍스트
    surfaceTint = Color(0xFF0D1B2A),
    inverseSurface = Color(0xFF0D1B2A),
    inverseOnSurface = Color(0xFFF8FAFC),
    outline = Color(0xFFCBD5E1),              // 입력/구분선 (강)
    outlineVariant = Color(0xFFE2E8F0),       // 고스트 보더 (약)
    error = Color(0xFFDC2626),                // 카민 레드
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B),
    scrim = Color(0xFF0A192F)
)

/** 카드 16dp / 컨트롤 12dp / 배지 풀필 */
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
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
            // 라이트 서피스 + 다크 아이콘 (상단바가 화이트 톤으로 바뀌었으므로)
            window.statusBarColor = colorScheme.surface.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }
    }

    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, density.fontScale * TEXT_SCALE)
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = AppShapes,
            typography = Typography(),
            content = content
        )
    }
}
