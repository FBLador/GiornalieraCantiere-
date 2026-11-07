package com.idroambiente.giornalieradicantiere.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Colori dell'app (redesign "Prototipo 3d").
 * I nomi corrispondono alle variabili CSS del prototipo, così è facile confrontarli.
 */
@Immutable
data class AppColors(
    val bg: Color,
    val card: Color,
    val ink: Color,
    val sub: Color,
    val line: Color,
    val soft: Color,
    val pri: Color,
    val onPri: Color,
    val hdr: Color,
    val onHdr: Color,
    val hdrSub: Color,
    val hdrBtn: Color,
    val strong: Color,
    val onStrong: Color,
    val okBg: Color,
    val ok: Color,
    val warnBg: Color,
    val warn: Color,
    val warnInk: Color,
    val yel: Color,
    val onYel: Color,
    val tile: Color,
    val tileSel: Color,
    val onTileSel: Color,
    val avSel: Color,
    val err: Color,
    val errBg: Color,
    val scrim: Color,
    val dlg: Color,
    val isDark: Boolean
)

val LightAppColors = AppColors(
    bg = Color(0xFFF0F2F4),
    card = Color(0xFFFFFFFF),
    ink = Color(0xFF1B2530),
    sub = Color(0xFF3D5060),
    line = Color(0xFFE3E8EC),
    soft = Color(0xFFE6ECF0),
    pri = Color(0xFF00527E),
    onPri = Color(0xFFFFFFFF),
    hdr = Color(0xFF00527E),
    onHdr = Color(0xFFFFFFFF),
    hdrSub = Color(0xFFC5E3F0),
    hdrBtn = Color(0x24FFFFFF),
    strong = Color(0xFF1B2530),
    onStrong = Color(0xFFFFFFFF),
    okBg = Color(0xFFDDEFDC),
    ok = Color(0xFF1E5A22),
    warnBg = Color(0xFFFFF0C7),
    warn = Color(0xFF5A3E00),
    warnInk = Color(0xFF7A5600),
    yel = Color(0xFFE8C440),
    onYel = Color(0xFF1B2530),
    tile = Color(0xFFFFFFFF),
    tileSel = Color(0xFF00527E),
    onTileSel = Color(0xFFFFFFFF),
    avSel = Color(0x2EFFFFFF),
    err = Color(0xFFB00020),
    errBg = Color(0xFFFDEDED),
    scrim = Color(0x8C0A141E),
    dlg = Color(0xFFFFFFFF),
    isDark = false
)

val DarkAppColors = AppColors(
    bg = Color(0xFF161412),
    card = Color(0xFF24211E),
    ink = Color(0xFFF3EFE9),
    sub = Color(0xFFCFC7BC),
    line = Color(0xFF38332E),
    soft = Color(0xFF2E2A26),
    pri = Color(0xFF7CC4EC),
    onPri = Color(0xFF0E2433),
    hdr = Color(0xFF211E1B),
    onHdr = Color(0xFFF3EFE9),
    hdrSub = Color(0xFFCFC7BC),
    hdrBtn = Color(0xFF2E2A26),
    strong = Color(0xFFF3EFE9),
    onStrong = Color(0xFF161412),
    okBg = Color(0xFF263321),
    ok = Color(0xFFA8DBA0),
    warnBg = Color(0xFF3D3219),
    warn = Color(0xFFF2D27A),
    warnInk = Color(0xFFF2D27A),
    yel = Color(0xFFE8C440),
    onYel = Color(0xFF1B1B1B),
    tile = Color(0xFF24211E),
    tileSel = Color(0xFF7CC4EC),
    onTileSel = Color(0xFF0E2433),
    avSel = Color(0x24000000),
    err = Color(0xFFFF8A80),
    errBg = Color(0xFF3A1F1C),
    scrim = Color(0x99000000),
    dlg = Color(0xFF24211E),
    isDark = true
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }
