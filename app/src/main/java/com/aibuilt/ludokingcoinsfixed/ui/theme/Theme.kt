package com.aibuilt.ludokingcoinsfixed.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFFB3261E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410002),
    secondary = Color(0xFF2E6B31),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFB5F2B4),
    onSecondaryContainer = Color(0xFF00210A),
    tertiary = Color(0xFF7A5900),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE08A),
    onTertiaryContainer = Color(0xFF261A00),
    background = Color(0xFFFFFBF7),
    onBackground = Color(0xFF201A18),
    surface = Color(0xFFFFFBF7),
    onSurface = Color(0xFF201A18),
    surfaceVariant = Color(0xFFF3DED8),
    onSurfaceVariant = Color(0xFF52443F),
    outline = Color(0xFF85736E),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB4AB),
    onPrimary = Color(0xFF690005),
    primaryContainer = Color(0xFF8B1A16),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFF9BD69B),
    onSecondary = Color(0xFF00390D),
    secondaryContainer = Color(0xFF14521C),
    onSecondaryContainer = Color(0xFFB5F2B4),
    tertiary = Color(0xFFEFC04B),
    onTertiary = Color(0xFF402D00),
    tertiaryContainer = Color(0xFF5C4300),
    onTertiaryContainer = Color(0xFFFFE08A),
    background = Color(0xFF16120F),
    onBackground = Color(0xFFEDE0DC),
    surface = Color(0xFF16120F),
    onSurface = Color(0xFFEDE0DC),
    surfaceVariant = Color(0xFF52443F),
    onSurfaceVariant = Color(0xFFD7C2BC),
    outline = Color(0xFFA08C86),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val AppTypography = Typography(
    headlineSmall = Typography().headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = Typography().titleLarge.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = Typography().labelLarge.copy(letterSpacing = 0.4.sp)
)

val MonoFamily = FontFamily.Monospace

@Composable
fun LudoKingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
