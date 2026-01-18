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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.idroambiente.giornalieradicantiere.database.entities.Giornaliera
import com.idroambiente.giornalieradicantiere.database.entities.StatoGiornaliera
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GiornaliereScreen(
    viewModel: GiornaliereViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToGiornaliera: (Long) -> Unit = {},
    onNavigateToWizard: (Long) -> Unit = {}
) {
    val commessa by viewModel.commessa.collectAsState()
    val giornaliere by viewModel.giornaliere.collectAsState()
    val cantiereId by viewModel.cantiereId.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Giornaliere - ${commessa?.nome ?: ""}") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Indietro"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            cantiereId?.let { id ->
                FloatingActionButton(
                    onClick = { onNavigateToWizard(id) }
                ) {
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
                items(giornaliere, key = { it.id }) { giornaliera ->
                    GiornalieraItem(
                        giornaliera = giornaliera,
                        onOpen = {
                            viewModel.openGiornaliera(giornaliera.id)
                            onNavigateToGiornaliera(giornaliera.id)
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { viewModel.generateRiepilogoCommessa() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Riepilogo Commessa")
                    }
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
            Button(onClick = onOpen) {
                Text("Apri")
            }
        }
    }
}

@Composable
fun StatoGiornalieraChip(stato: StatoGiornaliera) {
    val (backgroundColor, textColor) = when (stato) {
        StatoGiornaliera.BOZZA -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
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
