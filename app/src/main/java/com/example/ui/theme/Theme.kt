package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = CyanPulse,
    onPrimary = OnCyanPulse,
    primaryContainer = CyanPulseContainer,
    onPrimaryContainer = Color(0xFFA6F7FC),
    secondary = EmeraldPaid,
    onSecondary = Color(0xFF003824),
    secondaryContainer = EmeraldPaidDarkContainer,
    onSecondaryContainer = EmeraldPaidContainer,
    tertiary = AmberWarning,
    onTertiary = Color(0xFF3E2200),
    tertiaryContainer = AmberWarningDarkContainer,
    onTertiaryContainer = AmberWarningContainer,
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = CoralPendingDarkContainer,
    onErrorContainer = CoralPendingContainer,
    background = MidnightSlate,
    onBackground = DarkOnSurface,
    surface = SurfaceDark,
    onSurface = DarkOnSurface,
    surfaceVariant = SurfaceContainerDark,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceElevatedDark,
    outline = DarkOutline,
    outlineVariant = Color(0xFF223B52),
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryTealLight,
    onPrimary = Color.White,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = EmeraldPaid,
    onSecondary = Color.White,
    secondaryContainer = EmeraldPaidContainer,
    onSecondaryContainer = Color(0xFF065F46),
    tertiary = AmberWarning,
    onTertiary = Color.White,
    tertiaryContainer = AmberWarningContainer,
    onTertiaryContainer = Color(0xFF92400E),
    error = CoralPending,
    onError = Color.White,
    errorContainer = CoralPendingContainer,
    onErrorContainer = Color(0xFF991B1B),
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainer = LightSurface,
    surfaceContainerHigh = Color(0xFFEEF3F8),
    outline = LightOutline,
    outlineVariant = Color(0xFFE2E8F0),
)

val NetLedgerShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun NetLedgerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = NetLedgerShapes,
        content = content
    )
}
