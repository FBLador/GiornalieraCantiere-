package com.idroambiente.giornalieradicantiere.ui.impostazioni

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsSection(
    isEnabled: Boolean,
    hour: Int,
    minute: Int,
    onEnabledChange: (Boolean) -> Unit,
    onTimeChange: (Int, Int) -> Unit,
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
                Text(
                    if (isEnabled) {
                        "Attivo ogni giorno alle $timeLabel"
                    } else {
                        "Disattivato"
                    }
                )
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
                    onCheckedChange = onEnabledChange
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
        HorizontalDivider()
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = hour,
            initialMinute = minute,
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Seleziona orario") },
            text = {
                TimePicker(state = timePickerState)
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
