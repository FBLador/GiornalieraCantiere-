package com.idroambiente.giornalieradicantiere.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.idroambiente.giornalieradicantiere.database.entities.StatoGiornaliera
import com.idroambiente.giornalieradicantiere.ui.theme.AppText
import com.idroambiente.giornalieradicantiere.ui.theme.AppTheme

// Componenti del redesign "Prototipo 3d": grandi, ad alto contrasto, usabili coi guanti.

private val R10 = RoundedCornerShape(10.dp)
private val R12 = RoundedCornerShape(12.dp)

/** Ombra leggera delle card (solo tema chiaro, come nel prototipo). */
fun Modifier.cardShadow(shape: RoundedCornerShape = R12, dark: Boolean): Modifier =
    if (dark) this else this.shadow(1.dp, shape, ambientColor = Color(0x14001E32), spotColor = Color(0x14001E32))

// ───────────────────────────── Intestazione ─────────────────────────────

/**
 * Intestazione colorata delle schermate.
 * [onBack] != null mostra la freccia (o [backIcon]) a sinistra.
 */
@Composable
fun AppHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    titleStyle: TextStyle = AppText.header,
    onBack: (() -> Unit)? = null,
    backIcon: ImageVector = Icons.AutoMirrored.Filled.ArrowBack,
    backDescription: String = "Indietro",
    leading: (@Composable RowScope.() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    bottom: (@Composable ColumnScope.() -> Unit)? = null
) {
    val c = AppTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .background(c.hdr)
            .statusBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = if (onBack != null) 4.dp else 12.dp, end = 10.dp, top = 4.dp, bottom = if (bottom == null) 12.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (onBack != null) 4.dp else 12.dp)
        ) {
            if (onBack != null) {
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(R10)
                        .clickable(role = Role.Button, onClick = onBack)
                        .semantics { contentDescription = backDescription },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(backIcon, contentDescription = null, tint = c.onHdr, modifier = Modifier.size(28.dp))
                }
            }
            leading?.invoke(this)
            Column(Modifier.weight(1f)) {
                Text(title, style = titleStyle, color = c.onHdr, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) {
                    Text(subtitle, style = AppText.headerSub, color = c.hdrSub, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            trailing?.invoke(this)
        }
        bottom?.invoke(this)
    }
}

/** Pulsante quadrato 52dp nell'intestazione (impostazioni, riepilogo). */
@Composable
fun HeaderIconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    val c = AppTheme.colors
    Box(
        Modifier
            .size(52.dp)
            .clip(R10)
            .background(c.hdrBtn)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = c.onHdr)
    }
}

// ───────────────────────────── Pulsanti ─────────────────────────────

/** Pulsante grande pieno. [hint] = seconda riga piccola (es. "Serve il PIN del responsabile"). */
@Composable
fun BigButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    container: Color = AppTheme.colors.pri,
    content: Color = AppTheme.colors.onPri,
    height: Dp = 60.dp,
    hint: String? = null,
    textStyle: TextStyle = AppText.button,
    enabled: Boolean = true
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(R10)
            .background(container)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(25.dp))
        Column(horizontalAlignment = Alignment.Start) {
            Text(text, style = textStyle, color = content, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (hint != null) Text(hint, style = AppText.meta.copy(fontSize = AppText.meta.fontSize * 0.93f), color = content, maxLines = 1)
        }
    }
}

/** Pulsante con bordo (es. "Squadra di ieri", "Usa dati precedenti"). */
@Composable
fun OutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    height: Dp = 52.dp
) {
    val c = AppTheme.colors
    Row(
        modifier
            .height(height)
            .clip(R10)
            .border(BorderStroke(2.dp, c.pri), R10)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = c.pri, modifier = Modifier.size(22.dp))
        Text(text, style = AppText.buttonSmall, color = c.pri, maxLines = 1)
    }
}

/** Pulsante solo testo (es. "Modifica", "Tutti"). */
@Composable
fun TextLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = AppTheme.colors.pri) {
    Box(
        modifier
            .height(40.dp)
            .clip(R10)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = AppText.buttonSmall, color = color)
    }
}

/** Quadrato 52dp chiaro con icona (es. grafico riepilogo nelle card). */
@Composable
fun SoftIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    container: Color = AppTheme.colors.soft,
    tint: Color = AppTheme.colors.pri
) {
    Box(
        modifier
            .size(size)
            .clip(R10)
            .background(container)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint)
    }
}

// ───────────────────────────── Contenitori ─────────────────────────────

@Composable
fun AppPanel(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: PaddingValues = PaddingValues(14.dp),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val c = AppTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .cardShadow(R12, c.isDark)
            .clip(R12)
            .background(c.card)
            .then(if (border != null) Modifier.border(border, R12) else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content
    )
}

/** Etichetta di sezione in maiuscolo, con icona opzionale. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val c = AppTheme.colors
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (icon != null) Icon(icon, contentDescription = null, tint = c.sub, modifier = Modifier.size(20.dp))
        Text(text.uppercase(), style = AppText.section, color = c.sub)
    }
}

enum class InfoKind { WARN, ERROR, SOFT, OK }

/** Riquadro di avviso colorato. */
@Composable
fun InfoBox(text: String, kind: InfoKind, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val c = AppTheme.colors
    val (bg, fg) = when (kind) {
        InfoKind.WARN -> c.warnBg to c.warn
        InfoKind.ERROR -> c.errBg to c.err
        InfoKind.SOFT -> c.soft to c.ink
        InfoKind.OK -> c.okBg to c.ok
    }
    Row(
        modifier
            .fillMaxWidth()
            .clip(R10)
            .background(bg)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(22.dp))
        Text(text, style = if (kind == InfoKind.SOFT) AppText.bodySmall else AppText.bodySmall.copy(fontWeight = AppText.button.fontWeight), color = fg)
    }
}

// ───────────────────────────── Stato ─────────────────────────────

@Composable
fun StatusChip(stato: StatoGiornaliera, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    val (bg, fg) = when (stato) {
        StatoGiornaliera.BOZZA -> c.warnBg to c.warn
        StatoGiornaliera.COMPLETATA -> c.soft to c.pri
        StatoGiornaliera.INVIATA -> c.okBg to c.ok
    }
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(stato.name, style = AppText.chip, color = fg)
    }
}

// ───────────────────────────── Selezione ─────────────────────────────

/** Casella quadrata: piena con spunta se selezionata, solo bordo altrimenti. */
@Composable
fun CheckSquare(selected: Boolean, selectedBg: Color, selectedFg: Color, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    if (selected) {
        Box(
            modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(selectedBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = selectedFg, modifier = Modifier.size(22.dp))
        }
    } else {
        Box(
            modifier
                .size(30.dp)
                .border(3.dp, c.sub, RoundedCornerShape(8.dp))
        )
    }
}

/** Tessera selezionabile (persone, mezzi, attrezzature): diventa blu quando scelta. */
@Composable
fun SelectTile(
    selected: Boolean,
    onClick: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    initials: String? = null,
    icon: ImageVector? = null,
    height: Dp = 104.dp
) {
    val c = AppTheme.colors
    val bg = if (selected) c.tileSel else c.tile
    val fg = if (selected) c.onTileSel else c.ink
    val avBg = if (selected) c.avSel else c.soft
    Column(
        modifier
            .height(height)
            .cardShadow(R12, c.isDark)
            .clip(R12)
            .background(bg)
            .clickable(role = Role.Checkbox, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(R10)
                    .background(avBg),
                contentAlignment = Alignment.Center
            ) {
                if (initials != null) Text(initials, style = AppText.metaStrong.copy(fontSize = AppText.body.fontSize), color = fg)
                if (icon != null) Icon(icon, contentDescription = null, tint = fg)
            }
            CheckSquare(selected, selectedBg = c.yel, selectedFg = c.onYel)
        }
        Column {
            Text(title, style = if (subtitle == null) AppText.rowTitle.copy(lineHeight = AppText.itemTitle.lineHeight) else AppText.itemTitle, color = fg, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, style = AppText.meta, color = fg, maxLines = 1)
        }
    }
}

/** Tessera con etichetta piccola e valore grande (orari, pausa, viaggi). */
@Composable
fun ValueTile(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val c = AppTheme.colors
    Column(
        modifier
            .heightIn(min = 72.dp)
            .clip(R10)
            .background(if (filled) c.soft else Color.Transparent)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, tint = c.sub, modifier = Modifier.size(18.dp))
            Text(label, style = AppText.section.copy(letterSpacing = AppText.chip.letterSpacing * 0), color = c.sub, maxLines = 1)
        }
        Text(value, style = AppText.value, color = c.ink, maxLines = 1)
    }
}

/** Riga "Applica a": casella + nome + stato a destra. */
@Composable
fun ApplyRow(
    title: String,
    subtitle: String?,
    selected: Boolean,
    statusText: String,
    statusIcon: ImageVector,
    statusColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = AppTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .cardShadow(R12, c.isDark)
            .clip(R12)
            .background(c.card)
            .clickable(role = Role.Checkbox, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CheckSquare(selected, selectedBg = c.pri, selectedFg = c.onPri)
        Column(Modifier.weight(1f)) {
            Text(title, style = AppText.itemTitle, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) Text(subtitle, style = AppText.meta, color = c.sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(18.dp))
            Text(statusText, style = AppText.metaStrong, color = statusColor)
        }
    }
}

/** Interruttore grande 60×34 del prototipo. */
@Composable
fun AppSwitch(checked: Boolean, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    Box(
        modifier
            .size(width = 60.dp, height = 34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(if (checked) c.pri else c.line)
    ) {
        Box(
            Modifier
                .offset(x = if (checked) 30.dp else 4.dp, y = 4.dp)
                .size(26.dp)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

/** Riga con interruttore (es. Pernotto). */
@Composable
fun SwitchRow(
    text: String,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val c = AppTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(R10)
            .clickable(role = Role.Switch, onClick = onToggle)
            .semantics { selected = checked }
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (icon != null) Icon(icon, contentDescription = null, tint = c.ink)
            Text(text, style = AppText.button, color = c.ink)
        }
        AppSwitch(checked)
    }
}

@Immutable
data class SegItem(val label: String, val icon: ImageVector? = null)

/** Controllo a segmenti (filtri, tema, Drive/telefono). */
@Composable
fun SegmentedControl(
    items: List<SegItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 50.dp
) {
    val c = AppTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .clip(R12)
            .background(c.soft)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEachIndexed { i, item ->
            val on = i == selectedIndex
            Row(
                Modifier
                    .weight(1f)
                    .height(height)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (on) c.card else Color.Transparent)
                    .clickable(role = Role.Tab, onClick = { onSelect(i) })
                    .semantics { selected = on },
                horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val fg = if (on) c.pri else c.sub
                if (item.icon != null) Icon(item.icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
                Text(item.label, style = AppText.buttonSmall, color = fg, maxLines = 1)
            }
        }
    }
}

/** Riga delle impostazioni: icona, titolo, sottotitolo, elemento a destra. */
@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    subtitleFirst: Boolean = false,
    showDivider: Boolean = true,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val c = AppTheme.colors
    Column(modifier.alpha(if (enabled) 1f else 0.4f)) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 68.dp)
                .then(if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier)
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = c.sub)
            Column(Modifier.weight(1f)) {
                if (subtitleFirst && subtitle != null) Text(subtitle, style = AppText.meta, color = c.sub)
                Text(title, style = AppText.itemTitle, color = c.ink)
                if (!subtitleFirst && subtitle != null) Text(subtitle, style = AppText.meta, color = c.sub)
            }
            trailing?.invoke()
        }
        if (showDivider) Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
    }
}

// ───────────────────────────── Campi di testo ─────────────────────────────

/** Campo grande con etichetta sopra (Nuova commessa, liste, km). */
@Composable
fun BigTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    textStyle: TextStyle = AppText.rowTitle,
    keyboardType: KeyboardType = KeyboardType.Text,
    height: Dp = 58.dp
) {
    val c = AppTheme.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = AppText.metaStrong, color = c.sub)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = textStyle.copy(color = c.ink),
            cursorBrush = SolidColor(c.pri),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(R10)
                .background(c.bg)
                .border(2.dp, c.line, R10),
            decorationBox = { inner ->
                Box(Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) Text(placeholder, style = textStyle, color = c.sub.copy(alpha = 0.6f))
                    inner()
                }
            }
        )
    }
}

/** Area di testo (note, motivo) con contatore caratteri. */
@Composable
fun TextArea(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    maxLength: Int,
    modifier: Modifier = Modifier,
    minHeight: Dp = 96.dp,
    footerStart: String? = null
) {
    val c = AppTheme.colors
    Column(modifier) {
        BasicTextField(
            value = value,
            onValueChange = { onValueChange(it.take(maxLength)) },
            textStyle = AppText.body.copy(color = c.ink),
            cursorBrush = SolidColor(c.pri),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = minHeight)
                .clip(R10)
                .background(c.bg)
                .border(2.dp, c.line, R10),
            decorationBox = { inner ->
                Box(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    if (value.isEmpty()) Text(placeholder, style = AppText.body, color = c.sub.copy(alpha = 0.7f))
                    inner()
                }
            }
        )
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(footerStart ?: "", style = AppText.meta.copy(fontSize = AppText.section.fontSize), color = c.sub)
            Text("${value.length}/$maxLength", style = AppText.meta.copy(fontSize = AppText.section.fontSize), color = c.sub)
        }
    }
}

// ───────────────────────────── Barre ─────────────────────────────

/** Barra in fondo alla schermata con i pulsanti principali. */
@Composable
fun BottomBar(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    val c = AppTheme.colors
    Column(modifier.fillMaxWidth().background(c.bg)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

/** Spazio sotto il contenuto per non finire dietro la barra di navigazione. */
@Composable
fun BottomSpacer(height: Dp = 16.dp) = Spacer(Modifier.height(height))

/** Riga "sezione" con etichetta e azione a destra (es. "Personale … Modifica"). */
@Composable
fun SectionHeaderRow(text: String, icon: ImageVector?, action: String?, onAction: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        SectionLabel(text, icon = icon)
        if (action != null && onAction != null) TextLink(action, onAction) else Spacer(Modifier.width(1.dp).height(40.dp))
    }
}
