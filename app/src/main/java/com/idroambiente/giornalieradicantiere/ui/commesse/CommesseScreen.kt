package com.idroambiente.giornalieradicantiere.ui.commesse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.idroambiente.giornalieradicantiere.ui.components.AppCard
import com.idroambiente.giornalieradicantiere.ui.components.AppFAB
import com.idroambiente.giornalieradicantiere.ui.components.AppTopBar
import com.idroambiente.giornalieradicantiere.ui.components.PrimaryButton
import com.idroambiente.giornalieradicantiere.ui.components.SecondaryButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.idroambiente.giornalieradicantiere.database.entities.Commessa
import com.idroambiente.giornalieradicantiere.database.entities.StatoCommessa

@Composable
fun CommesseScreen(
    viewModel: CommesseViewModel = hiltViewModel(),
    onNavigateToCommessa: (Long) -> Unit = {},
    onNavigateToImpostazioni: () -> Unit = {}
) {
    val commesse by viewModel.commesse.collectAsState()
    val showCreateDialog by viewModel.showCreateDialog.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Commesse",
                actions = {
                    IconButton(onClick = onNavigateToImpostazioni) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Impostazioni",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            AppFAB(onClick = { viewModel.showCreateDialog() }) {
                Icon(Icons.Default.Add, contentDescription = "Nuova Commessa")
            }
        }
    ) { paddingValues ->
        if (commesse.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Nessuna commessa",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Premi + per creare una nuova commessa",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    items(commesse, key = { it.id }) { commessa ->
                        CommessaItem(
                            commessa = commessa,
                            onOpen = {
                                viewModel.openCommessa(commessa.id)
                                onNavigateToCommessa(commessa.id)
                            },
                            onRiepilogo = { viewModel.generateRiepilogo(commessa.id) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        if (showCreateDialog) {
            CreateCommessaDialog(
                onConfirm = { codice, cliente ->
                    viewModel.createCommessa(codice, cliente, "")
                },
                onDismiss = { viewModel.hideCreateDialog() }
            )
        }
    }
}

@Composable
fun CommessaItem(
    commessa: Commessa,
    onOpen: () -> Unit,
    onRiepilogo: () -> Unit
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(48.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${commessa.nome.take(4)}-${commessa.nome.substring(4)}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Cliente: ${commessa.cliente}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatoChip(stato = commessa.stato)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton(onClick = onRiepilogo) {
                        Text("Riepilogo")
                    }
                    PrimaryButton(onClick = onOpen) {
                        Text("Apri")
                    }
                }
            }
        }
    }
}

@Composable
fun StatoChip(stato: StatoCommessa) {
    val (backgroundColor, textColor) = when (stato) {
        StatoCommessa.APERTA -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        StatoCommessa.SOSPESA -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        StatoCommessa.CHIUSA -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Text(
            text = stato.name,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = textColor
        )
    }
}

@Composable
fun CreateCommessaDialog(
    onConfirm: (codice: String, cliente: String) -> Unit,
    onDismiss: () -> Unit
) {
    var codiceParte1 by remember { mutableStateOf("") }  // prime 4 cifre
    var codiceParte2 by remember { mutableStateOf("") }  // ultime 3 cifre
    var cliente by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current
    val focusRequester1 = remember { FocusRequester() }
    val focusRequester2 = remember { FocusRequester() }
    val focusRequester3 = remember { FocusRequester() }

    // Auto-focus al primo campo all'apertura
    LaunchedEffect(Unit) {
        focusRequester1.requestFocus()
    }

    // Auto-advance: quando codiceParte1 ha 4 cifre, passa a codiceParte2
    LaunchedEffect(codiceParte1) {
        if (codiceParte1.length == 4) {
            focusRequester2.requestFocus()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuova Commessa") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = codiceParte1,
                        onValueChange = { input ->
                            codiceParte1 = input.filter(Char::isDigit).take(4)
                        },
                        label = { Text("Codice") },
                        placeholder = { Text("0000") },
                        supportingText = { Text("${codiceParte1.length}/4") },
                        isError = codiceParte1.isNotEmpty() && codiceParte1.length < 4,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusRequester2.requestFocus() }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester1)
                    )
                    Text("-", style = MaterialTheme.typography.headlineMedium)
                    OutlinedTextField(
                        value = codiceParte2,
                        onValueChange = { input ->
                            codiceParte2 = input.filter(Char::isDigit).take(3)
                        },
                        label = { Text("Suffisso") },
                        placeholder = { Text("000") },
                        supportingText = { Text("${codiceParte2.length}/3") },
                        isError = codiceParte2.isNotEmpty() && codiceParte2.length < 3,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusRequester3.requestFocus() }
                        ),
                        modifier = Modifier
                            .weight(0.75f)
                            .focusRequester(focusRequester2)
                    )
                }
                OutlinedTextField(
                    value = cliente,
                    onValueChange = { cliente = it },
                    label = { Text("Cliente") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (codiceParte1.length == 4 && codiceParte2.length == 3 && cliente.isNotBlank()) {
                                onConfirm(codiceParte1 + codiceParte2, cliente)
                            }
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester3)
                )
            }
        },
        confirmButton = {
            PrimaryButton(
                onClick = { onConfirm(codiceParte1 + codiceParte2, cliente) },
                enabled = codiceParte1.length == 4 && codiceParte2.length == 3 && cliente.isNotBlank(),
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

