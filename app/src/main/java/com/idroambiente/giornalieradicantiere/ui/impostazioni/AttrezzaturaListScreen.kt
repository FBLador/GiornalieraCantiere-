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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.idroambiente.giornalieradicantiere.database.entities.Attrezzatura

@Composable
fun AttrezzaturaListScreen(
    viewModel: AttrezzaturaViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val attrezzaturaList by viewModel.attrezzaturaList.collectAsState()
    val showCreateDialog by viewModel.showCreateDialog.collectAsState()
    val attrezzaturaToEdit by viewModel.attrezzaturaToEdit.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Lista Attrezzature",
                showBack = true,
                onBackClick = onNavigateBack
            )
        },
        floatingActionButton = {
            AppFAB(onClick = { viewModel.showCreateDialog() }) {
                Icon(Icons.Default.Add, contentDescription = "Aggiungi attrezzatura")
            }
        }
    ) { paddingValues ->
        if (attrezzaturaList.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Nessuna attrezzatura",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Premi + per aggiungere una nuova attrezzatura",
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
                items(attrezzaturaList, key = { it.id }) { attrezzatura ->
                    AttrezzaturaItem(
                        attrezzatura = attrezzatura,
                        onEdit = { viewModel.showEditDialog(attrezzatura) },
                        onDelete = { viewModel.deleteAttrezzatura(attrezzatura) }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        if (showCreateDialog) {
            CreateAttrezzaturaDialog(
                onConfirm = { nome ->
                    viewModel.createAttrezzatura(nome)
                },
                onDismiss = { viewModel.hideCreateDialog() }
            )
        }

        attrezzaturaToEdit?.let { attrezzatura ->
            EditAttrezzaturaDialog(
                attrezzatura = attrezzatura,
                onConfirm = { nome -> viewModel.updateAttrezzatura(nome) },
                onDismiss = { viewModel.hideEditDialog() }
            )
        }
    }
}

@Composable
fun AttrezzaturaItem(
    attrezzatura: Attrezzatura,
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
                text = attrezzatura.nome,
                style = MaterialTheme.typography.bodyLarge
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
fun CreateAttrezzaturaDialog(
    onConfirm: (nome: String) -> Unit,
    onDismiss: () -> Unit
) {
    var nome by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuova Attrezzatura") },
        text = {
            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it },
                label = { Text("Nome") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            PrimaryButton(
                onClick = { onConfirm(nome) },
                enabled = nome.isNotBlank(),
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
fun EditAttrezzaturaDialog(
    attrezzatura: Attrezzatura,
    onConfirm: (nome: String) -> Unit,
    onDismiss: () -> Unit
) {
    var nome by remember(attrezzatura.id) { mutableStateOf(attrezzatura.nome) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifica attrezzatura") },
        text = {
            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it },
                label = { Text("Nome") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            PrimaryButton(
                onClick = { onConfirm(nome) },
                enabled = nome.isNotBlank(),
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
