package com.idroambiente.giornalieradicantiere.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.idroambiente.giornalieradicantiere.R

/** Archivo (SIL Open Font License, vedi assets/licenze). */
val Archivo = FontFamily(
    Font(R.font.archivo_regular, FontWeight.Normal),
    Font(R.font.archivo_medium, FontWeight.Medium),
    Font(R.font.archivo_semibold, FontWeight.SemiBold),
    Font(R.font.archivo_bold, FontWeight.Bold)
)

private fun style(weight: FontWeight, size: Int, line: Int, spacing: TextUnit = 0.sp) = TextStyle(
    fontFamily = Archivo,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = spacing
)

/** Stili di testo del prototipo (nome = uso). */
object AppText {
    /** Titolo grande dell'intestazione ("Commesse") */
    val headerLarge = style(FontWeight.Bold, 26, 30)
    /** Titolo intestazione delle altre schermate */
    val header = style(FontWeight.Bold, 22, 26)
    /** Titolo intestazione wizard */
    val headerSmall = style(FontWeight.Bold, 18, 22)
    /** Sottotitolo intestazione */
    val headerSub = style(FontWeight.SemiBold, 14, 18)
    /** Titolo di pagina ("Chi c'è oggi?") e codice commessa nelle card */
    val pageTitle = style(FontWeight.Bold, 26, 30)
    /** Titolo dei fogli dal basso */
    val sheetTitle = style(FontWeight.Bold, 22, 26)
    val cardTitle = style(FontWeight.Bold, 20, 24)
    val rowTitle = style(FontWeight.Bold, 18, 22)
    val itemTitle = style(FontWeight.Bold, 17, 20)
    val bodyLarge = style(FontWeight.SemiBold, 17, 22)
    val body = style(FontWeight.SemiBold, 16, 22)
    val bodySmall = style(FontWeight.SemiBold, 15, 20)
    val meta = style(FontWeight.SemiBold, 14, 18)
    val metaStrong = style(FontWeight.Bold, 14, 16)
    /** Etichette di sezione in maiuscolo */
    val section = style(FontWeight.Bold, 13, 16, 1.2.sp)
    val chip = style(FontWeight.Bold, 12, 14, 0.8.sp)
    val button = style(FontWeight.Bold, 17, 20)
    val buttonLarge = style(FontWeight.Bold, 18, 20)
    val buttonSmall = style(FontWeight.Bold, 15, 18)
    /** Valori grandi nelle tessere (orari, km) */
    val value = style(FontWeight.Bold, 26, 30)
    val valueLarge = style(FontWeight.Bold, 28, 32)
    val input = style(FontWeight.Bold, 30, 36)
}

val Typography = Typography(
    displayLarge = style(FontWeight.Bold, 32, 40),
    displayMedium = style(FontWeight.Bold, 28, 36),
    displaySmall = style(FontWeight.Bold, 26, 30),
    headlineLarge = style(FontWeight.Bold, 24, 30),
    headlineMedium = style(FontWeight.Bold, 22, 26),
    headlineSmall = style(FontWeight.Bold, 20, 24),
    titleLarge = style(FontWeight.Bold, 20, 26),
    titleMedium = style(FontWeight.Bold, 17, 22),
    titleSmall = style(FontWeight.Bold, 15, 20),
    bodyLarge = style(FontWeight.SemiBold, 16, 22),
    bodyMedium = style(FontWeight.SemiBold, 15, 20),
    bodySmall = style(FontWeight.SemiBold, 13, 18),
    labelLarge = style(FontWeight.Bold, 15, 18),
    labelMedium = style(FontWeight.Bold, 13, 16),
    labelSmall = style(FontWeight.Bold, 12, 14)
)
