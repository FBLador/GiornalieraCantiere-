package com.idroambiente.giornalieradicantiere.ui.giornaliere

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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.idroambiente.giornalieradicantiere.ui.components.AppCard
import com.idroambiente.giornalieradicantiere.ui.components.AppFAB
import com.idroambiente.giornalieradicantiere.ui.components.AppTopBar
import com.idroambiente.giornalieradicantiere.ui.components.PrimaryButton
import com.idroambiente.giornalieradicantiere.ui.components.SecondaryButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.idroambiente.giornalieradicantiere.database.entities.Giornaliera
import com.idroambiente.giornalieradicantiere.database.entities.StatoGiornaliera
import com.idroambiente.giornalieradicantiere.util.formatCommessaNome
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GiornaliereScreen(
    viewModel: GiornaliereViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToGiornaliera: (Long, Long) -> Unit = { _, _ -> },
    onNavigateToWizard: (Long) -> Unit = {},
    onNavigateToRiepilogoCommessa: (Long) -> Unit = {}
) {
    val commessa by viewModel.commessa.collectAsState()
    val giornaliere by viewModel.giornaliere.collectAsState()
    val cantiereId by viewModel.cantiereId.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Giornaliere - ${commessa?.let { formatCommessaNome(it.nome) } ?: ""}",
                showBack = true,
                onBackClick = onNavigateBack
            )
        },
        floatingActionButton = {
            cantiereId?.let { id ->
                AppFAB(onClick = { onNavigateToWizard(id) }) {
                    Icon(Icons.Default.Add, contentDescription = "Nuova Giornaliera")
                }
            }
        }
    ) { paddingValues ->
        if (giornaliere.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Nessuna giornaliera",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Premi + per creare una nuova giornaliera",
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
                item {
                    SecondaryButton(
                        onClick = { onNavigateToRiepilogoCommessa(viewModel.commessaId) },
                        modifier = Modifier.fillMaxWidth(),
                        content = { Text("Riepilogo Commessa") }
                    )
                }
                items(giornaliere, key = { it.id }) { giornaliera ->
                    GiornalieraItem(
                        giornaliera = giornaliera,
                        onOpen = {
                            viewModel.openGiornaliera(giornaliera.id)
                            onNavigateToGiornaliera(giornaliera.cantiereId, giornaliera.id)
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
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
fun GiornalieraItem(
    giornaliera: Giornaliera,
    onOpen: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY)
    val formattedDate = dateFormat.format(Date(giornaliera.data))

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpen
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatoGiornalieraChip(stato = giornaliera.stato)
            }
            PrimaryButton(onClick = onOpen) {
                Text("Apri")
            }
        }
    }
}

@Composable
fun StatoGiornalieraChip(stato: StatoGiornaliera) {
    val (backgroundColor, textColor) = when (stato) {
        StatoGiornaliera.BOZZA -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.onError
        StatoGiornaliera.COMPLETATA -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        StatoGiornaliera.INVIATA -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
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
