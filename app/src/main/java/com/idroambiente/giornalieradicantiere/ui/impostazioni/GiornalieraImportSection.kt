package com.idroambiente.giornalieradicantiere.ui.impostazioni

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun GiornalieraImportSection(
    modifier: Modifier = Modifier,
    viewModel: GiornalieraImportViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val exportFolderUri by viewModel.exportFolderUri.collectAsState()
    val context = LocalContext.current

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            viewModel.scanFolder(it, context)
        }
    }

    LaunchedEffect(state) {
        if (state is GiornalieraImportState.Success) {
            Toast.makeText(
                context,
                (state as GiornalieraImportState.Success).summary,
                Toast.LENGTH_LONG
            ).show()
            viewModel.resetState()
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Importa Giornaliere",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(end = 8.dp, top = 2.dp)
                            .size(16.dp)
                    )
                    Text(
                        text = "Importa giornaliere da una cartella contenente file CSV esportati dall'app. " +
                                "Le giornaliere importate avranno stato INVIATA. " +
                                "Personale, mezzi e attrezzature non presenti verranno creati automaticamente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { folderPickerLauncher.launch(exportFolderUri) },
                enabled = state !is GiornalieraImportState.Scanning &&
                        state !is GiornalieraImportState.Importing
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Seleziona cartella")
            }

            if (state is GiornalieraImportState.Scanning || state is GiornalieraImportState.Importing) {
                Spacer(modifier = Modifier.width(16.dp))
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                if (state is GiornalieraImportState.Importing) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = (state as GiornalieraImportState.Importing).progress,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    // Confirmation dialog
    if (state is GiornalieraImportState.Parsed) {
        val result = (state as GiornalieraImportState.Parsed).result
        AlertDialog(
            onDismissRequest = { viewModel.resetState() },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Conferma importazione") },
            text = {
                Column {
                    Text(
                        "Trovati ${result.fileCount} file CSV con ${result.giornaliere.size} giornaliere."
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val totalPersonale = result.giornaliere.sumOf { it.personaleRows.size }
                    val totalMezzi = result.giornaliere.sumOf { it.mezzoRows.size }
                    val totalAttrezzature = result.giornaliere.sumOf { it.attrezzatureNomi.size }

                    Text(
                        "Risorse:\n" +
                                "- $totalPersonale personale\n" +
                                "- $totalMezzi mezzi\n" +
                                "- $totalAttrezzature attrezzature"
                    )

                    if (result.newPersonale.isNotEmpty() || result.newMezzi.isNotEmpty() || result.newAttrezzature.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val newItems = mutableListOf<String>()
                        if (result.newPersonale.isNotEmpty()) {
                            newItems.add("${result.newPersonale.size} personale")
                        }
                        if (result.newMezzi.isNotEmpty()) {
                            newItems.add("${result.newMezzi.size} mezzi")
                        }
                        if (result.newAttrezzature.isNotEmpty()) {
                            newItems.add("${result.newAttrezzature.size} attrezzature")
                        }
                        Text(
                            text = "Verranno creati: ${newItems.joinToString(", ")}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Le giornaliere duplicate verranno sovrascritte.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (result.errors.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Avvisi (${result.errors.size}):",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = result.errors.take(10).joinToString("\n") +
                                    if (result.errors.size > 10) "\n... e altri ${result.errors.size - 10}" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmImport() }) {
                    Text("Conferma")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.resetState() }) {
                    Text("Annulla")
                }
            }
        )
    }

    // Error dialog
    if (state is GiornalieraImportState.Error) {
        val errorMessage = (state as GiornalieraImportState.Error).message
        AlertDialog(
            onDismissRequest = { viewModel.resetState() },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Errore") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = { viewModel.resetState() }) {
                    Text("OK")
                }
            }
        )
    }
}
