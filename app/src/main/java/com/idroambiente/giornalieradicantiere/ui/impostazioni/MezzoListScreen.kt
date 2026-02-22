package com.idroambiente.giornalieradicantiere.ui.impostazioni

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.idroambiente.giornalieradicantiere.ui.components.AppCard
import com.idroambiente.giornalieradicantiere.ui.components.AppFAB
import com.idroambiente.giornalieradicantiere.ui.components.AppTopBar
import com.idroambiente.giornalieradicantiere.ui.components.PrimaryButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.idroambiente.giornalieradicantiere.database.entities.Mezzo

@Composable
fun MezzoListScreen(
    viewModel: MezzoViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val mezzoList by viewModel.mezzoList.collectAsState()
    val showCreateDialog by viewModel.showCreateDialog.collectAsState()
    val mezzoToEdit by viewModel.mezzoToEdit.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Lista Mezzi",
                showBack = true,
                onBackClick = onNavigateBack
            )
        },
        floatingActionButton = {
            AppFAB(onClick = { viewModel.showCreateDialog() }) {
                Icon(Icons.Default.Add, contentDescription = "Aggiungi mezzo")
            }
        }
    ) { paddingValues ->
        if (mezzoList.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Nessun mezzo",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Premi + per aggiungere un nuovo mezzo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                items(mezzoList, key = { it.id }) { mezzo ->
                    MezzoItem(
                        mezzo = mezzo,
                        onEdit = { viewModel.showEditDialog(mezzo) },
                        onDelete = { viewModel.deleteMezzo(mezzo) }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        if (showCreateDialog) {
            CreateMezzoDialog(
                onConfirm = { nome, targa, codiceArticoloKm, codiceArticoloGiorni ->
                    viewModel.createMezzo(nome, targa, codiceArticoloKm, codiceArticoloGiorni)
                },
                onDismiss = { viewModel.hideCreateDialog() }
            )
        }

        mezzoToEdit?.let { mezzo ->
            EditMezzoDialog(
                mezzo = mezzo,
                onConfirm = { nome, targa, codiceArticoloKm, codiceArticoloGiorni ->
                    viewModel.updateMezzo(nome, targa, codiceArticoloKm, codiceArticoloGiorni)
                },
                onDismiss = { viewModel.hideEditDialog() }
            )
        }

        // Error Dialog
        errorMessage?.let { error ->
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                containerColor = MaterialTheme.colorScheme.surface,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = { Text("Errore") },
                text = { Text(error) },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@Composable
fun MezzoItem(
    mezzo: Mezzo,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AppCard(modifier = Modifier.fillMaxWidth(), onClick = null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mezzo.nome,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = mezzo.targa,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val codiciInfo = buildList {
                    if (mezzo.codiceArticoloKm.isNotBlank()) add("Km: ${mezzo.codiceArticoloKm}")
                    if (mezzo.codiceArticoloGiorni.isNotBlank()) add("Giorni: ${mezzo.codiceArticoloGiorni}")
                }
                if (codiciInfo.isNotEmpty()) {
                    Text(
                        text = codiciInfo.joinToString(" | "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Modifica",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Elimina",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun CreateMezzoDialog(
    onConfirm: (nome: String, targa: String, codiceArticoloKm: String, codiceArticoloGiorni: String) -> Unit,
    onDismiss: () -> Unit
) {
    var nome by remember { mutableStateOf("") }
    var targa by remember { mutableStateOf("") }
    var codiceArticoloKm by remember { mutableStateOf("") }
    var codiceArticoloGiorni by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Nuovo Mezzo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it.take(50) },
                    label = { Text("Nome") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = targa,
                    onValueChange = { targa = it.uppercase().take(10) },
                    label = { Text("Targa") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = codiceArticoloKm,
                    onValueChange = { codiceArticoloKm = it.uppercase().take(20) },
                    label = { Text("Codice Articolo Km") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = codiceArticoloGiorni,
                    onValueChange = { codiceArticoloGiorni = it.uppercase().take(20) },
                    label = { Text("Codice Articolo Giorni (opzionale)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            PrimaryButton(
                onClick = { onConfirm(nome, targa, codiceArticoloKm, codiceArticoloGiorni) },
                enabled = nome.isNotBlank() && targa.isNotBlank() && codiceArticoloKm.isNotBlank(),
                content = { Text("Crea") }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}

@Composable
fun EditMezzoDialog(
    mezzo: Mezzo,
    onConfirm: (nome: String, targa: String, codiceArticoloKm: String, codiceArticoloGiorni: String) -> Unit,
    onDismiss: () -> Unit
) {
    var nome by remember(mezzo.id) { mutableStateOf(mezzo.nome) }
    var targa by remember(mezzo.id) { mutableStateOf(mezzo.targa) }
    var codiceArticoloKm by remember(mezzo.id) { mutableStateOf(mezzo.codiceArticoloKm) }
    var codiceArticoloGiorni by remember(mezzo.id) { mutableStateOf(mezzo.codiceArticoloGiorni) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Modifica mezzo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it.take(50) },
                    label = { Text("Nome") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = targa,
                    onValueChange = { targa = it.uppercase().take(10) },
                    label = { Text("Targa") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = codiceArticoloKm,
                    onValueChange = { codiceArticoloKm = it.uppercase().take(20) },
                    label = { Text("Codice Articolo Km") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = codiceArticoloGiorni,
                    onValueChange = { codiceArticoloGiorni = it.uppercase().take(20) },
                    label = { Text("Codice Articolo Giorni (opzionale)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            PrimaryButton(
                onClick = { onConfirm(nome, targa, codiceArticoloKm, codiceArticoloGiorni) },
                enabled = nome.isNotBlank() && targa.isNotBlank() && codiceArticoloKm.isNotBlank(),
                content = { Text("Salva") }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}
