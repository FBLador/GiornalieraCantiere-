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
import com.idroambiente.giornalieradicantiere.database.entities.Personale

@Composable
fun PersonaleListScreen(
    viewModel: PersonaleViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val personaleList by viewModel.personaleList.collectAsState()
    val showCreateDialog by viewModel.showCreateDialog.collectAsState()
    val personaleToEdit by viewModel.personaleToEdit.collectAsState()
    val duplicateDialogState by viewModel.duplicateDialogState.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Lista Persone",
                showBack = true,
                onBackClick = onNavigateBack
            )
        },
        floatingActionButton = {
            AppFAB(onClick = { viewModel.showCreateDialog() }) {
                Icon(Icons.Default.Add, contentDescription = "Aggiungi persona")
            }
        }
    ) { paddingValues ->
        if (personaleList.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Nessuna persona",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Premi + per aggiungere una nuova persona",
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
                items(personaleList, key = { it.id }) { personale ->
                    PersonaleItem(
                        personale = personale,
                        onEdit = { viewModel.showEditDialog(personale) },
                        onDelete = { viewModel.deletePersonale(personale) }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        if (showCreateDialog) {
            CreatePersonaleDialog(
                onConfirm = { nome, cognome ->
                    viewModel.createPersonale(nome, cognome)
                },
                onDismiss = { viewModel.hideCreateDialog() }
            )
        }

        personaleToEdit?.let { personale ->
            EditPersonaleDialog(
                personale = personale,
                onConfirm = { nome, cognome -> viewModel.updatePersonale(nome, cognome) },
                onDismiss = { viewModel.hideEditDialog() }
            )
        }

        // Duplicate Dialog
        duplicateDialogState?.let { state ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissDuplicateDialog() },
                containerColor = MaterialTheme.colorScheme.surface,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = { Text("Persona già esistente") },
                text = {
                    Column {
                        Text(
                            text = "Esiste già una persona con nome \"${state.nome}\" e cognome \"${state.cognome}\".",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Modifica il nome per evitare confusione.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                confirmButton = {
                    PrimaryButton(
                        onClick = { viewModel.confirmDuplicateAndModify() },
                        content = { Text("Modifica nome") }
                    )
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissDuplicateDialog() }) {
                        Text("Annulla")
                    }
                }
            )
        }
    }
}

@Composable
fun PersonaleItem(
    personale: Personale,
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
            Text(
                text = "${personale.nome} ${personale.cognome}",
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
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
fun CreatePersonaleDialog(
    onConfirm: (nome: String, cognome: String) -> Unit,
    onDismiss: () -> Unit
) {
    var nome by remember { mutableStateOf("") }
    var cognome by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Nuova Persona") },
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
                    value = cognome,
                    onValueChange = { cognome = it.take(50) },
                    label = { Text("Cognome") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            PrimaryButton(
                onClick = { onConfirm(nome, cognome) },
                enabled = nome.isNotBlank() && cognome.isNotBlank(),
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
fun EditPersonaleDialog(
    personale: Personale,
    onConfirm: (nome: String, cognome: String) -> Unit,
    onDismiss: () -> Unit
) {
    var nome by remember(personale.id) { mutableStateOf(personale.nome) }
    var cognome by remember(personale.id) { mutableStateOf(personale.cognome) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Modifica persona") },
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
                    value = cognome,
                    onValueChange = { cognome = it.take(50) },
                    label = { Text("Cognome") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            PrimaryButton(
                onClick = { onConfirm(nome, cognome) },
                enabled = nome.isNotBlank() && cognome.isNotBlank(),
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
