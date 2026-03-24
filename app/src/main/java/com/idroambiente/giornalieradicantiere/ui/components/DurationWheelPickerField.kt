package com.idroambiente.giornalieradicantiere.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

private fun floatHoursToParts(hours: Float, maxTotalHours: Float): Pair<Int, Int> {
    val maxMinutes = (maxTotalHours * 60f).roundToInt().coerceAtLeast(0)
    val totalMinutes = (hours * 60f).roundToInt().coerceIn(0, maxMinutes)
    return Pair(totalMinutes / 60, totalMinutes % 60)
}

private fun partsToFloatHours(h: Int, m: Int, maxTotalHours: Float): Float {
    val maxMinutes = (maxTotalHours * 60f).roundToInt().coerceAtLeast(0)
    var total = h * 60 + m
    total = total.coerceIn(0, maxMinutes)
    return total / 60f
}

private fun formatDurationDisplay(hours: Float, maxTotalHours: Float = 24f): String {
    val (h, m) = floatHoursToParts(hours, maxTotalHours)
    return when {
        h == 0 && m == 0 -> "0 h"
        m == 0 -> "$h h"
        else -> "$h h $m min"
    }
}

private fun LazyListState.snappedItemIndex(): Int {
    val info = layoutInfo
    val visible = info.visibleItemsInfo
    if (visible.isEmpty()) return firstVisibleItemIndex
    val start = info.viewportStartOffset
    val end = info.viewportEndOffset
    val center = start + (end - start) / 2
    return visible.minByOrNull { item ->
        abs(item.offset + item.size / 2 - center)
    }?.index ?: firstVisibleItemIndex
}

@Composable
fun DurationWheelPickerField(
    value: Float,
    onValueChange: (Float) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    maxTotalHours: Float = 24f,
) {
    var showDialog by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Box(
        modifier = modifier
            .semantics {
                contentDescription = "$label: ${formatDurationDisplay(value, maxTotalHours)}"
            }
            .clickable {
                focusManager.clearFocus()
                showDialog = true
            }
    ) {
        OutlinedTextField(
            value = formatDurationDisplay(value, maxTotalHours),
            onValueChange = { },
            label = { Text(label) },
            readOnly = true,
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }

    if (showDialog) {
        val maxH = floor(maxTotalHours).toInt().coerceAtLeast(0)
        val (ih, im) = floatHoursToParts(value, maxTotalHours)
        var draftHour by remember { mutableIntStateOf(ih.coerceIn(0, maxH)) }
        var draftMinute by remember { mutableIntStateOf(im.coerceIn(0, if (ih >= maxH) 0 else 59)) }

        LaunchedEffect(draftHour, maxH) {
            if (draftHour >= maxH) {
                draftMinute = 0
            }
        }

        Dialog(onDismissRequest = { showDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
            ) {
                DurationWheelDialogBody(
                    draftHour = draftHour,
                    draftMinute = draftMinute,
                    maxHourInclusive = maxH,
                    title = label,
                    onHourChange = { draftHour = it },
                    onMinuteChange = { draftMinute = it },
                    onConfirm = {
                        onValueChange(partsToFloatHours(draftHour, draftMinute, maxTotalHours))
                        showDialog = false
                    },
                    onDismiss = { showDialog = false },
                    onReset = {
                        draftHour = 0
                        draftMinute = 0
                    }
                )
            }
        }
    }
}

@Composable
private fun DurationWheelDialogBody(
    draftHour: Int,
    draftMinute: Int,
    maxHourInclusive: Int,
    title: String,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onReset: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WheelColumn(
                label = "Ore",
                range = 0..maxHourInclusive,
                value = draftHour,
                onValueChange = onHourChange,
                contentDescription = "Selettore ore viaggio",
                modifier = Modifier.weight(1f)
            )
            key(draftHour >= maxHourInclusive) {
                WheelColumn(
                    label = "Minuti",
                    range = if (draftHour >= maxHourInclusive) 0..0 else 0..59,
                    value = if (draftHour >= maxHourInclusive) 0 else draftMinute,
                    onValueChange = onMinuteChange,
                    contentDescription = "Selettore minuti viaggio",
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
            TextButton(onClick = onReset) {
                Text("Azzera")
            }
            Button(onClick = onConfirm) {
                Text("Conferma")
            }
        }
    }
}

@Composable
private fun WheelColumn(
    label: String,
    range: IntRange,
    value: Int,
    onValueChange: (Int) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val itemHeight = 48.dp
    val visibleRows = 5
    val listHeight = itemHeight * visibleRows
    val verticalPad = itemHeight * ((visibleRows - 1) / 2)
    val maxIndex = (range.count() - 1).coerceAtLeast(0)

    val initialIndex = ((value - range.first) / range.step).coerceIn(0, maxIndex)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val snap = rememberSnapFlingBehavior(lazyListState = listState)

    LaunchedEffect(value, range.first, range.last) {
        val idx = ((value - range.first) / range.step).coerceIn(0, maxIndex)
        if (!listState.isScrollInProgress) {
            val snapped = listState.snappedItemIndex().coerceIn(0, maxIndex)
            if (snapped != idx) {
                listState.scrollToItem(idx)
            }
        }
    }

    LaunchedEffect(listState, range.first, range.last) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .filter { !it }
            .collect {
                delay(32)
                val listIdx = listState.snappedItemIndex().coerceIn(0, maxIndex)
                val newVal = range.first + listIdx * range.step
                if (newVal != value) {
                    onValueChange(newVal)
                }
            }
    }

    Column(
        modifier = modifier.semantics { this.contentDescription = contentDescription }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(listHeight)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .align(Alignment.Center)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        RoundedCornerShape(8.dp)
                    )
            )
            LazyColumn(
                state = listState,
                flingBehavior = snap,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = verticalPad),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                items(range.count()) { i ->
                    val n = range.first + i * range.step
                    val isSelected = n == value
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(itemHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = n.toString(),
                            style = if (isSelected) {
                                MaterialTheme.typography.titleLarge
                            } else {
                                MaterialTheme.typography.bodyLarge
                            },
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                            }
                        )
                    }
                }
            }
        }
    }
}
