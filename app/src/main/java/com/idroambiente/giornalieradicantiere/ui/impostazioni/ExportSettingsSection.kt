package com.idroambiente.giornalieradicantiere.ui.impostazioni

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.idroambiente.giornalieradicantiere.data.preferences.ExportBehavior

@Composable
fun ExportSettingsSection(
    folderUri: String?,
    folderName: String?,
    exportBehavior: ExportBehavior,
    isFolderAccessible: Boolean,
    onFolderSelected: (Uri) -> Unit,
    onClearFolder: () -> Unit,
    onBehaviorChanged: (ExportBehavior) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showBehaviorDialog by remember { mutableStateOf(false) }

    // Launcher per il folder picker SAF
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            // Ottieni permessi persistenti
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            onFolderSelected(it)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Titolo sezione
        Text(
            text = "Esportazione PDF",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Cartella di salvataggio
        ListItem(
            headlineContent = { Text("Cartella di salvataggio") },
            supportingContent = {
                Column {
                    Text(
                        text = folderName ?: "Nessuna cartella selezionata",
                        color = if (folderName == null) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    if (folderUri != null && !isFolderAccessible) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text(
                                text = "Cartella non accessibile",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            },
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingContent = {
                if (folderUri != null) {
                    IconButton(onClick = onClearFolder) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Rimuovi cartella"
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { folderPickerLauncher.launch(null) }
        )
        HorizontalDivider()

        // Comportamento all'invio
        ListItem(
            headlineContent = { Text("Comportamento all'invio") },
            supportingContent = {
                Text(
                    text = when (exportBehavior) {
                        ExportBehavior.SAVE -> "Salva automaticamente nella cartella"
                        ExportBehavior.SHARE -> "Mostra opzioni di condivisione"
                        ExportBehavior.BOTH -> "Salva e chiedi se condividere"
                    }
                )
            },
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showBehaviorDialog = true }
        )
        HorizontalDivider()

        // Info box
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(end = 8.dp, top = 2.dp)
                        .size(16.dp)
                )
                Text(
                    text = if (folderUri == null) {
                        "Seleziona una cartella locale per salvare automaticamente i PDF. " +
                        "Per salvare su Google Drive, usa la modalità \"Google Drive\" sopra."
                    } else {
                        "I PDF verranno salvati nella cartella selezionata con il formato: NomeCommessa_Data.pdf"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // Dialog per selezionare il comportamento
    if (showBehaviorDialog) {
        ExportBehaviorDialog(
            currentBehavior = exportBehavior,
            onBehaviorSelected = {
                onBehaviorChanged(it)
                showBehaviorDialog = false
            },
            onDismiss = { showBehaviorDialog = false }
        )
    }
}

@Composable
private fun ExportBehaviorDialog(
    currentBehavior: ExportBehavior,
    onBehaviorSelected: (ExportBehavior) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Comportamento all'invio") },
        text = {
            Column(Modifier.selectableGroup()) {
                ExportBehavior.entries.forEach { behavior ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .selectable(
                                selected = (behavior == currentBehavior),
                                onClick = { onBehaviorSelected(behavior) },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (behavior == currentBehavior),
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = when (behavior) {
                                    ExportBehavior.SAVE -> "Salva automaticamente"
                                    ExportBehavior.SHARE -> "Mostra condivisione"
                                    ExportBehavior.BOTH -> "Salva e condividi"
                                },
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = when (behavior) {
                                    ExportBehavior.SAVE -> "Salva il PDF nella cartella configurata"
                                    ExportBehavior.SHARE -> "Mostra le opzioni per condividere il PDF"
                                    ExportBehavior.BOTH -> "Salva nella cartella e poi chiedi se condividere"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Chiudi")
            }
        }
    )
}
