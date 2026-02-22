package com.idroambiente.giornalieradicantiere.ui.impostazioni

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsSection(
    isEnabled: Boolean,
    hour: Int,
    minute: Int,
    enabledDays: Set<Int>,
    onEnabledChange: (Boolean) -> Unit,
    onTimeChange: (Int, Int) -> Unit,
    onDaysChange: (Set<Int>) -> Unit,
    onSendImmediateTestNotification: () -> Unit,
    onScheduleTestNotification: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showTimePicker by remember { mutableStateOf(false) }
    val timeLabel = remember(hour, minute) {
        String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Notifiche",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        ListItem(
            headlineContent = { Text("Promemoria giornaliero") },
            supportingContent = {
                if (isEnabled) {
                    Column {
                        Text("Attivo alle $timeLabel")
                        Text(formatEnabledDays(enabledDays))
                    }
                } else {
                    Column {
                        Text("Disattivato")
                        Text("") // placeholder per mantenere 2 righe
                    }
                }
            },
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingContent = {
                Switch(
                    checked = isEnabled,
                    onCheckedChange = onEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
        HorizontalDivider()

        ListItem(
            headlineContent = { Text("Orario promemoria") },
            supportingContent = {
                Text(
                    if (isEnabled) {
                        timeLabel
                    } else {
                        "Attiva il promemoria per impostare l'orario"
                    }
                )
            },
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = isEnabled) {
                    showTimePicker = true
                }
        )
        HorizontalDivider()

        ListItem(
            headlineContent = { Text("Giorni promemoria") },
            supportingContent = {
                DayOfWeekSelector(
                    enabledDays = enabledDays,
                    onDaysChange = onDaysChange,
                    enabled = isEnabled
                )
            },
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
        HorizontalDivider()

        /*ListItem(
            headlineContent = { Text("Mostra notifica adesso") },
            supportingContent = {
                Text("Invia subito una notifica di test (senza WorkManager)")
            },
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSendImmediateTestNotification() }
        )
        HorizontalDivider()

        ListItem(
            headlineContent = { Text("Invia notifica di test") },
            supportingContent = {
                Text("Programma una notifica di test via worker tra 20 secondi")
            },
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onScheduleTestNotification() }
        )
        HorizontalDivider()*/
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = hour,
            initialMinute = minute,
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Seleziona orario") },
            text = {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        selectorColor = MaterialTheme.colorScheme.primary,
                        containerColor = MaterialTheme.colorScheme.surface,
                        clockDialColor = MaterialTheme.colorScheme.surfaceVariant,
                        clockDialSelectedContentColor = MaterialTheme.colorScheme.onPrimary,
                        clockDialUnselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        timeSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        timeSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onTimeChange(timePickerState.hour, timePickerState.minute)
                        showTimePicker = false
                    }
                ) {
                    Text("Conferma")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Annulla")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayOfWeekSelector(
    enabledDays: Set<Int>,
    onDaysChange: (Set<Int>) -> Unit,
    enabled: Boolean
) {
    // Calendar.SUNDAY=1, MONDAY=2, ..., SATURDAY=7
    // Display order: Lun(2), Mar(3), Mer(4), Gio(5), Ven(6), Sab(7), Dom(1)
    val daysInOrder = listOf(
        Calendar.MONDAY to "L",
        Calendar.TUESDAY to "M",
        Calendar.WEDNESDAY to "M",
        Calendar.THURSDAY to "G",
        Calendar.FRIDAY to "V",
        Calendar.SATURDAY to "S",
        Calendar.SUNDAY to "D"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        daysInOrder.forEach { (dayValue, label) ->
            val isSelected = dayValue in enabledDays
            FilterChip(
                selected = isSelected,
                onClick = {
                    val newDays = if (isSelected) {
                        enabledDays - dayValue
                    } else {
                        enabledDays + dayValue
                    }
                    onDaysChange(newDays)
                },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                enabled = enabled,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun formatEnabledDays(days: Set<Int>): String {
    if (days.size == 7) return "ogni giorno"
    if (days == setOf(2, 3, 4, 5, 6)) return "Lun-Ven"
    if (days.isEmpty()) return ""

    val dayNames = mapOf(
        Calendar.MONDAY to "Lun",
        Calendar.TUESDAY to "Mar",
        Calendar.WEDNESDAY to "Mer",
        Calendar.THURSDAY to "Gio",
        Calendar.FRIDAY to "Ven",
        Calendar.SATURDAY to "Sab",
        Calendar.SUNDAY to "Dom"
    )
    val order = listOf(
        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
        Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
    )
    return order.filter { it in days }.mapNotNull { dayNames[it] }.joinToString(", ")
}
