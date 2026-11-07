package com.idroambiente.giornalieradicantiere.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.dp
import com.idroambiente.giornalieradicantiere.data.preferences.TemaApp

/** I componenti Material (dialoghi, campi di testo, date picker) usano gli stessi colori. */
private fun AppColors.toColorScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = pri,
        onPrimary = onPri,
        primaryContainer = soft,
        onPrimaryContainer = ink,
        secondary = pri,
        onSecondary = onPri,
        secondaryContainer = soft,
        onSecondaryContainer = ink,
        tertiary = yel,
        onTertiary = onYel,
        tertiaryContainer = warnBg,
        onTertiaryContainer = warn,
        background = bg,
        onBackground = ink,
        surface = card,
        onSurface = ink,
        surfaceVariant = soft,
        onSurfaceVariant = sub,
        surfaceTint = pri,
        outline = sub,
        outlineVariant = line,
        error = err,
        onError = if (isDark) bg else card,
        errorContainer = errBg,
        onErrorContainer = err,
        scrim = scrim,
        surfaceContainerLowest = card,
        surfaceContainerLow = card,
        surfaceContainer = dlg,
        surfaceContainerHigh = dlg,
        surfaceContainerHighest = soft,
        inverseSurface = strong,
        inverseOnSurface = onStrong,
        inversePrimary = hdrSub
    )
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(18.dp)
)

/** Accesso ai colori dell'app: `AppTheme.colors.pri` */
object AppTheme {
    val colors: AppColors
        @Composable @ReadOnlyComposable
        get() = LocalAppColors.current
}

fun TemaApp.isDark(systemDark: Boolean): Boolean = when (this) {
    TemaApp.SISTEMA -> systemDark
    TemaApp.CHIARO -> false
    TemaApp.SCURO -> true
}

@Composable
fun GiornalieraDiCantiereTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkAppColors else LightAppColors
    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(
            colorScheme = colors.toColorScheme(),
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
