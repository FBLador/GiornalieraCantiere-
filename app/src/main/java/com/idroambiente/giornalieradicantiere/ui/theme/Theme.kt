package com.idroambiente.giornalieradicantiere.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = OnPrimaryLight,
    primaryContainer = BlueMuted,
    onPrimaryContainer = OnSurfaceLight,
    secondary = BlueAccent,
    onSecondary = OnPrimaryLight,
    secondaryContainer = BlueLighter,
    onSecondaryContainer = OnSurfaceLight,
    tertiary = BlueLight,
    onTertiary = OnPrimaryDark,
    tertiaryContainer = BlueMuted,
    onTertiaryContainer = OnSurfaceLight,
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    surface = BackgroundLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    inverseSurface = SurfaceDark,
    inverseOnSurface = OnSurfaceDark,
    outlineVariant = BlueMuted
)

private val DarkColorScheme = darkColorScheme(
    primary = BluePrimaryDarkTheme,
    onPrimary = OnPrimaryDark,
    primaryContainer = BlueMutedDark,
    onPrimaryContainer = OnSurfaceDark,
    secondary = BlueAccentDark,
    onSecondary = OnPrimaryDark,
    secondaryContainer = BluePrimaryDarkDarkTheme,
    onSecondaryContainer = OnSurfaceDark,
    tertiary = BlueLight,
    onTertiary = OnPrimaryDark,
    tertiaryContainer = BlueMutedDark,
    onTertiaryContainer = OnSurfaceDark,
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    inverseSurface = OnSurfaceDark,
    inverseOnSurface = OnSurfaceDark,
    outlineVariant = OutlineDark
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun GiornalieraDiCantiereTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
