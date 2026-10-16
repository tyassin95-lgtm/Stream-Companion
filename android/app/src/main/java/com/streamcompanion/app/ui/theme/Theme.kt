package com.streamcompanion.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

object Palette {
    val Background = Color(0xFF0B0D12)
    val Surface = Color(0xFF12151C)
    val SurfaceHigh = Color(0xFF1A1E29)
    val Outline = Color(0xFF262B3A)
    val Accent = Color(0xFF8B5CF6)
    val Accent2 = Color(0xFF6366F1)
    val Text = Color(0xFFE8EAF0)
    val Muted = Color(0xFF8B92A5)
    val Ok = Color(0xFF34D399)
    val Warn = Color(0xFFFBBF24)
    val Bad = Color(0xFFF87171)
    val AccentBrush = Brush.linearGradient(listOf(Accent2, Accent))
}

private val Dark: ColorScheme = darkColorScheme(
    primary = Palette.Accent,
    onPrimary = Color.White,
    secondary = Palette.Accent2,
    background = Palette.Background,
    onBackground = Palette.Text,
    surface = Palette.Surface,
    onSurface = Palette.Text,
    surfaceVariant = Palette.SurfaceHigh,
    onSurfaceVariant = Palette.Muted,
    outline = Palette.Outline,
    error = Palette.Bad,
    surfaceContainer = Palette.Surface,
    surfaceContainerHigh = Palette.SurfaceHigh,
)

private val AppTypography = Typography(
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 15.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    bodySmall = TextStyle(fontSize = 12.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
)

@Composable
fun StreamTheme(content: @Composable () -> Unit) {
    // The app is dark-only on purpose: it is a video app and the player is always dark.
    @Suppress("UNUSED_VARIABLE") val unused = isSystemInDarkTheme()
    MaterialTheme(colorScheme = Dark, typography = AppTypography, shapes = AppShapes, content = content)
}
