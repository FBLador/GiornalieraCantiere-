package com.idroambiente.giornalieradicantiere.ui.impostazioni

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.idroambiente.giornalieradicantiere.ui.components.AppFAB
import com.idroambiente.giornalieradicantiere.ui.components.AppTopBar
import com.idroambiente.giornalieradicantiere.ui.components.PrimaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListeGestioneScreen(
    onNavigateBack: () -> Unit = {},
    personaleViewModel: PersonaleViewModel = hiltViewModel(),
    mezzoViewModel: MezzoViewModel = hiltViewModel(),
    attrezzaturaViewModel: AttrezzaturaViewModel = hiltViewModel()
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Persone", "Mezzi", "Attrezzature")

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Gestione Liste",
                showBack = true,
                onBackClick = onNavigateBack
            )
        },
        floatingActionButton = {
            AppFAB(
                onClick = {
                    when (selectedTabIndex) {
                        0 -> personaleViewModel.showCreateDialog()
                        1 -> mezzoViewModel.showCreateDialog()
                        2 -> attrezzaturaViewModel.showCreateDialog()
                    }
                }
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = when (selectedTabIndex) {
                        0 -> "Aggiungi persona"
                        1 -> "Aggiungi mezzo"
                        else -> "Aggiungi attrezzatura"
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> PersonaleTabContent(personaleViewModel)
                1 -> MezzoTabContent(mezzoViewModel)
                2 -> AttrezzaturaTabContent(attrezzaturaViewModel)
            }
        }
    }
}

@Composable
private fun PersonaleTabContent(viewModel: PersonaleViewModel) {
    val personaleList by viewModel.personaleList.collectAsState()
    val showCreateDialog by viewModel.showCreateDialog.collectAsState()
    val personaleToEdit by viewModel.personaleToEdit.collectAsState()
    val duplicateDialogState by viewModel.duplicateDialogState.collectAsState()

    if (personaleList.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
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
            onConfirm = { nome, cognome -> viewModel.createPersonale(nome, cognome) },
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

@Composable
private fun MezzoTabContent(viewModel: MezzoViewModel) {
    val mezzoList by viewModel.mezzoList.collectAsState()
    val showCreateDialog by viewModel.showCreateDialog.collectAsState()
    val mezzoToEdit by viewModel.mezzoToEdit.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    if (mezzoList.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
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

@Composable
private fun AttrezzaturaTabContent(viewModel: AttrezzaturaViewModel) {
    val attrezzaturaList by viewModel.attrezzaturaList.collectAsState()
    val showCreateDialog by viewModel.showCreateDialog.collectAsState()
    val attrezzaturaToEdit by viewModel.attrezzaturaToEdit.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    if (attrezzaturaList.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
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
            onConfirm = { nome, codiceArticolo ->
                viewModel.createAttrezzatura(nome, codiceArticolo)
            },
            onDismiss = { viewModel.hideCreateDialog() }
        )
    }

    attrezzaturaToEdit?.let { attrezzatura ->
        EditAttrezzaturaDialog(
            attrezzatura = attrezzatura,
            onConfirm = { nome, codiceArticolo ->
                viewModel.updateAttrezzatura(nome, codiceArticolo)
            },
            onDismiss = { viewModel.hideEditDialog() }
        )
    }

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
