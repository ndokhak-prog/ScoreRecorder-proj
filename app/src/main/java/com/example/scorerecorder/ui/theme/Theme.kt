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
 * Sovereign Modernism — Blue 테마
 *
 * navy 스케일(50 #F0F4F8 ~ 900 #0D1B2A) 위에 딥 블루(#002B5B)와 코발트(#2563EB)를 얹은 구성
 * - 캔버스: 블루 그라데이션(#E2EBF4 → #EDF2F7 → #F4F7FA)
 * - 카드: 화이트 + 24dp 라운드 + 소프트 섀도 + 화이트 헤어라인
 * - 상태: 긍정 제이드(#059669), 경고 앰버(#D97706), 오류 레드(#F04438)
 */
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF002B5B),              // 딥 블루 (브랜드/제목/주 버튼)
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD9E2EC),     // navy-100 (선택/배지 배경)
    onPrimaryContainer = Color(0xFF0F2C4C),
    inversePrimary = Color(0xFF9FB3C8),       // navy-300
    secondary = Color(0xFF2563EB),            // 코발트 액센트 (확인/활성)
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDBEAFE),
    onSecondaryContainer = Color(0xFF1E3A8A),
    tertiary = Color(0xFF059669),             // 제이드 (긍정/우승)
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Color(0xFF065F46),
    background = Color(0xFFEFF4F9),           // 캔버스 그라데이션 중간값 (단색 대체)
    onBackground = Color(0xFF111827),
    surface = Color(0xFFFFFFFF),              // 카드/모듈
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFF0F4F8),       // navy-50 서브 컨테이너
    onSurfaceVariant = Color(0xFF627D98),     // navy-400 뮤트 텍스트
    surfaceTint = Color(0xFF002B5B),
    inverseSurface = Color(0xFF112240),       // navy-800
    inverseOnSurface = Color(0xFFF0F4F8),
    outline = Color(0xFFBCCCDC),              // navy-200 입력/구분선 (강)
    outlineVariant = Color(0xFFD9E2EC),       // navy-100 고스트 보더 (약)
    error = Color(0xFFF04438),                // 레드 (삭제/경고)
    onError = Color.White,
    errorContainer = Color(0xFFFEE4E2),
    onErrorContainer = Color(0xFF912018),
    scrim = Color(0xFF0A192F)
)

/** 앱 캔버스 그라데이션 (위 → 아래) */
val CanvasGradient = listOf(
    Color(0xFFE2EBF4),
    Color(0xFFEDF2F7),
    Color(0xFFF4F7FA)
)

/** 카드 24dp / 컨트롤 12~16dp / 배지 풀필 */
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
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
            // 블루 캔버스에 맞춘 라이트 시스템 바 (다크 아이콘)
            window.statusBarColor = Color(0xFFE9EFF6).toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
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
