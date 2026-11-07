package com.idroambiente.giornalieradicantiere.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.idroambiente.giornalieradicantiere.ui.theme.AppText
import com.idroambiente.giornalieradicantiere.ui.theme.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlin.math.abs

private val R10 = RoundedCornerShape(10.dp)

// ───────────────────────────── Foglio dal basso ─────────────────────────────

/**
 * Foglio che sale dal basso con sfondo scuro (dialoghi del prototipo).
 * Va messo dentro il Box radice della schermata, sopra al contenuto.
 */
@Composable
fun BoxScope.AppSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    dismissible: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val c = AppTheme.colors
    BackHandler(enabled = visible) { if (dismissible) onDismiss() }
    AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.matchParentSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(c.scrim)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    if (dismissible) onDismiss()
                }
        )
    }
    AnimatedVisibility(
        visible,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier = Modifier.align(Alignment.BottomCenter)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                .background(c.dlg)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 40.dp, height = 5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(c.line)
            )
            content()
        }
    }
}

@Composable
fun SheetTitle(text: String, subtitle: String? = null) {
    val c = AppTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text, style = AppText.sheetTitle, color = c.ink)
        if (subtitle != null) Text(subtitle, style = AppText.body, color = c.sub)
    }
}

enum class SheetButtonKind { PRIMARY, DANGER, DANGER_SOLID, SOFT, SOFT_PRIMARY, WARN }

@Composable
fun SheetButton(
    text: String,
    onClick: () -> Unit,
    kind: SheetButtonKind = SheetButtonKind.PRIMARY,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val c = AppTheme.colors
    val (bg, fg, h) = when (kind) {
        SheetButtonKind.PRIMARY -> Triple(c.pri, c.onPri, 60.dp)
        SheetButtonKind.DANGER -> Triple(c.errBg, c.err, 56.dp)
        SheetButtonKind.DANGER_SOLID -> Triple(c.err, Color.White, 60.dp)
        SheetButtonKind.SOFT -> Triple(c.soft, c.ink, 54.dp)
        SheetButtonKind.SOFT_PRIMARY -> Triple(c.soft, c.pri, 56.dp)
        SheetButtonKind.WARN -> Triple(c.yel, c.onYel, 60.dp)
    }
    BigButton(
        text = text,
        onClick = onClick,
        icon = icon,
        container = bg,
        content = fg,
        height = h,
        modifier = modifier.alpha(if (enabled) 1f else 0.45f),
        enabled = enabled
    )
}

/** Riquadro grigio con nome file e cartella (dialoghi di conferma). */
@Composable
fun FileBox(lines: List<Pair<String, Boolean>>) {
    val c = AppTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(R10)
            .background(c.soft)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        lines.forEach { (text, secondary) ->
            Text(text, style = AppText.bodySmall, color = if (secondary) c.sub else c.ink)
        }
    }
}

@Composable
fun SheetIcon(icon: ImageVector, tint: Color = AppTheme.colors.ok) {
    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(44.dp))
}

// ───────────────────────────── Invio in corso ─────────────────────────────

@Composable
fun BoxScope.ProgressOverlay(visible: Boolean, label: String, progress: Float) {
    val c = AppTheme.colors
    BackHandler(enabled = visible) { /* non si esce durante l'invio */ }
    AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.matchParentSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(c.scrim)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            val animated by animateFloatAsState(progress.coerceIn(0f, 1f), label = "progress")
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(c.dlg)
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = c.pri, modifier = Modifier.size(44.dp))
                Text(label, style = AppText.cardTitle, color = c.ink, textAlign = TextAlign.Center)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(c.soft)
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animated)
                            .clip(RoundedCornerShape(5.dp))
                            .background(c.pri)
                    )
                }
                Text(
                    "Non chiudere l'app. Ci vogliono pochi secondi.",
                    style = AppText.bodySmall,
                    color = c.sub,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ───────────────────────────── Messaggi brevi ─────────────────────────────

@Stable
class ToastState {
    var message by mutableStateOf<String?>(null)
        private set
    private var serial by mutableStateOf(0)
    val key: Int get() = serial

    fun show(text: String) {
        message = text
        serial++
    }

    fun hide() {
        message = null
    }
}

@Composable
fun rememberToastState() = remember { ToastState() }

@Composable
fun BoxScope.ToastHost(state: ToastState, bottomPadding: Dp = 104.dp) {
    val c = AppTheme.colors
    val message = state.message
    LaunchedEffect(state.key) {
        if (message != null) {
            delay(2600)
            state.hide()
        }
    }
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut(),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(start = 14.dp, end = 14.dp, bottom = bottomPadding)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .shadow(8.dp, R10)
                .clip(R10)
                .background(c.strong)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = c.onStrong)
            Text(message ?: "", style = AppText.bodySmall.copy(fontWeight = AppText.button.fontWeight), color = c.onStrong)
        }
    }
}

// ───────────────────────────── Tastierino PIN ─────────────────────────────

/** Contenuto del foglio PIN: icona, titolo, 6 pallini, errore, tastierino grande (usabile coi guanti). */
@Composable
fun PinSheetContent(
    title: String,
    subtitle: String,
    icon: ImageVector,
    digits: Int,
    error: String?,
    locked: Boolean,
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit
) {
    val c = AppTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(c.soft),
            contentAlignment = Alignment.Center
        ) { Icon(icon, contentDescription = null, tint = c.pri) }
        Column(Modifier.weight(1f)) {
            Text(title, style = AppText.sheetTitle, color = c.ink)
            Text(subtitle, style = AppText.bodySmall, color = c.sub)
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp)
            .semantics { contentDescription = "$digits cifre inserite su 6" },
        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally)
    ) {
        repeat(6) { i ->
            val filled = i < digits
            Box(
                Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(if (filled) c.pri else Color.Transparent)
                    .border(2.dp, if (filled) c.pri else c.sub, CircleShape)
            )
        }
    }
    if (error != null) InfoBox(error, InfoKind.ERROR, icon = Icons.Default.Error)
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "del")
    Column(Modifier.alpha(if (locked) 0.4f else 1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { k ->
                    val clickable = k.isNotEmpty() && !locked
                    Box(
                        Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (k.isEmpty()) Color.Transparent else c.soft)
                            .clickable(enabled = clickable, role = Role.Button) {
                                if (k == "del") onDelete() else onDigit(k[0])
                            }
                            .semantics { if (k == "del") contentDescription = "Cancella" },
                        contentAlignment = Alignment.Center
                    ) {
                        when (k) {
                            "del" -> Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = null, tint = c.ink)
                            "" -> Unit
                            else -> Text(k, style = AppText.valueLarge, color = c.ink)
                        }
                    }
                }
            }
        }
    }
    SheetButton("Annulla", onCancel, SheetButtonKind.SOFT)
}

// ───────────────────────────── Rotella ore/minuti ─────────────────────────────

private val ITEM_H = 48.dp

private fun LazyListState.centeredIndex(): Int {
    val info = layoutInfo
    val visible = info.visibleItemsInfo
    if (visible.isEmpty()) return firstVisibleItemIndex
    val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
    return visible.minByOrNull { abs(it.offset + it.size / 2 - center) }?.index ?: firstVisibleItemIndex
}

/** Una colonna della rotella (5 righe visibili, quella centrale è la scelta). */
@Composable
fun WheelColumn(
    label: String,
    values: List<Int>,
    value: Int,
    onValueChange: (Int) -> Unit,
    format: (Int) -> String,
    modifier: Modifier = Modifier
) {
    val c = AppTheme.colors
    val startIndex = values.indexOf(value).coerceAtLeast(0)
    val state = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    val fling = rememberSnapFlingBehavior(lazyListState = state)

    LaunchedEffect(state, values) {
        snapshotFlow { state.isScrollInProgress }
            .distinctUntilChanged()
            .filter { !it }
            .collect {
                delay(32)
                val v = values.getOrNull(state.centeredIndex().coerceIn(0, values.lastIndex))
                if (v != null && v != value) onValueChange(v)
            }
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = AppText.metaStrong, color = c.sub, modifier = Modifier.padding(start = 4.dp))
        Box(Modifier.fillMaxWidth().height(ITEM_H * 5)) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(ITEM_H)
                    .clip(R10)
                    .background(c.soft)
                    .border(2.dp, c.pri, R10)
            )
            LazyColumn(
                state = state,
                flingBehavior = fling,
                contentPadding = PaddingValues(vertical = ITEM_H * 2),
                modifier = Modifier.fillMaxSize().semantics { contentDescription = "$label: ${format(value)}" },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(values.size) { i ->
                    val v = values[i]
                    val sel = v == value
                    Box(Modifier.fillMaxWidth().height(ITEM_H), contentAlignment = Alignment.Center) {
                        Text(
                            format(v),
                            style = if (sel) AppText.valueLarge else AppText.sheetTitle.copy(fontWeight = AppText.body.fontWeight),
                            color = if (sel) c.ink else c.sub
                        )
                    }
                }
            }
        }
    }
}

/**
 * Foglio con due rotelle (ore e minuti).
 * Orario: ore 0–23, minuti 0–59. Durata: ore 0–12, minuti a passi di 5.
 */
@Composable
fun WheelPickerContent(
    label: String,
    isTime: Boolean,
    initialMinutes: Int,
    onConfirm: (Int) -> Unit,
    onCancel: () -> Unit
) {
    val c = AppTheme.colors
    val hours = if (isTime) (0..23).toList() else (0..12).toList()
    val minutes = if (isTime) (0..59).toList() else (0..55 step 5).toList()
    var h by remember(label) { mutableStateOf((initialMinutes / 60).coerceIn(hours.first(), hours.last())) }
    var m by remember(label) { mutableStateOf(if (isTime) initialMinutes % 60 else (initialMinutes % 60) - (initialMinutes % 5)) }
    val preview = if (isTime) "%02d:%02d".format(h, m) else formatDurata(h * 60 + m)

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = AppText.sheetTitle, color = c.ink)
        SoftIconButton(Icons.Default.Close, "Chiudi", onCancel, size = 48.dp, tint = c.ink)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        WheelColumn("Ore", hours, h, { h = it }, { "%02d".format(it) }, Modifier.weight(1f))
        WheelColumn("Minuti", minutes, m, { m = it }, { "%02d".format(it) }, Modifier.weight(1f))
    }
    Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SheetButton("Annulla", onCancel, SheetButtonKind.SOFT, modifier = Modifier.weight(1f))
        SheetButton("Conferma $preview", { onConfirm(h * 60 + m) }, icon = Icons.Default.Check, modifier = Modifier.weight(2f))
    }
}

/** "30 min", "1 h", "1 h 30" come nel prototipo. */
fun formatDurata(minuti: Int): String {
    val h = minuti / 60
    val m = minuti % 60
    return when {
        h == 0 -> "$m min"
        m == 0 -> "$h h"
        else -> "$h h $m"
    }
}

@Composable
fun HSpacer(w: Dp) = Spacer(Modifier.width(w))
