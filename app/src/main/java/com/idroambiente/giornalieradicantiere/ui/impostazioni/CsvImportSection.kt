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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.UploadFile
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material3.RadioButton
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun CsvImportSection(
    modifier: Modifier = Modifier,
    viewModel: CsvImportViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.parseCsv(it, context.contentResolver)
        }
    }

    LaunchedEffect(state) {
        if (state is CsvImportState.Success) {
            Toast.makeText(context, "Importazione completata", Toast.LENGTH_SHORT).show()
            viewModel.resetState()
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Importa Dati",
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
                        text = "Importa persone, mezzi e attrezzature da un file CSV con formato:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "personale,Nome,Cognome\nmezzo,Nome,Targa,CodArtKm,CodArtGiorni\nattrezzatura,Nome,CodiceArticolo",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                onClick = { filePickerLauncher.launch(arrayOf("text/*")) },
                enabled = state !is CsvImportState.Importing
            ) {
                Icon(
                    imageVector = Icons.Default.UploadFile,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Seleziona file CSV")
            }

            if (state is CsvImportState.Importing) {
                Spacer(modifier = Modifier.width(16.dp))
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    // Dialogo conferma importazione
    if (state is CsvImportState.Parsed) {
        val result = (state as CsvImportState.Parsed).result
        val replaceExisting = remember { mutableStateOf(true) }
        AlertDialog(
            onDismissRequest = { viewModel.resetState() },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Conferma importazione") },
            text = {
                Column {
                    Text(
                        "Verranno importate:\n" +
                        "- ${result.personale.size} persone\n" +
                        "- ${result.mezzi.size} mezzi\n" +
                        "- ${result.attrezzature.size} attrezzature"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = replaceExisting.value,
                                onClick = { replaceExisting.value = true }
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = replaceExisting.value,
                            onClick = { replaceExisting.value = true }
                        )
                        Text(
                            text = "Sostituisci i dati esistenti",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = !replaceExisting.value,
                                onClick = { replaceExisting.value = false }
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = !replaceExisting.value,
                            onClick = { replaceExisting.value = false }
                        )
                        Text(
                            text = "Aggiungi ai dati esistenti",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (result.errors.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Righe ignorate (${result.errors.size}):",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = result.errors.joinToString("\n"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmImport(replaceExisting.value) }) {
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

    // Dialogo errore
    if (state is CsvImportState.Error) {
        val errorMessage = (state as CsvImportState.Error).message
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
