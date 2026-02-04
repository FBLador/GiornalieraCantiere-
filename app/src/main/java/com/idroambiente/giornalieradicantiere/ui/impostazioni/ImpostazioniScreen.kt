package com.idroambiente.giornalieradicantiere.ui.impostazioni

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.idroambiente.giornalieradicantiere.ui.components.AppTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.idroambiente.giornalieradicantiere.data.preferences.StorageMode

@Composable
fun ImpostazioniScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToPersonaleList: () -> Unit = {},
    onNavigateToMezzoList: () -> Unit = {},
    onNavigateToAttrezzaturaList: () -> Unit = {},
    exportSettingsViewModel: ExportSettingsViewModel = hiltViewModel()
) {
    val exportSettingsState by exportSettingsViewModel.uiState.collectAsState()
    var pendingModeChange by remember { mutableStateOf<StorageMode?>(null) }

    // Confirmation dialog for mode change
    pendingModeChange?.let { newMode ->
        AlertDialog(
            onDismissRequest = { pendingModeChange = null },
            title = { Text("Cambiare modalità?") },
            text = {
                Text(
                    if (newMode == StorageMode.LOCAL) {
                        "Passando alla modalità locale, la configurazione Google Drive rimarrà salvata ma non verrà utilizzata."
                    } else {
                        "Passando a Google Drive, il comportamento all'invio verrà impostato su \"Salva automaticamente\"."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    exportSettingsViewModel.setStorageMode(newMode)
                    pendingModeChange = null
                }) {
                    Text("Conferma")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingModeChange = null }) {
                    Text("Annulla")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Impostazioni",
                showBack = true,
                onBackClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Sezione Dati
            Text(
                text = "Gestione Dati",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            ListItem(
                headlineContent = { Text("Lista persone") },
                leadingContent = {
                    Icon(Icons.Default.Person, contentDescription = null)
                },
                trailingContent = {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToPersonaleList() }
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text("Lista mezzi") },
                leadingContent = {
                    Icon(Icons.Default.Build, contentDescription = null)
                },
                trailingContent = {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToMezzoList() }
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text("Lista attrezzature") },
                leadingContent = {
                    Icon(Icons.Default.Settings, contentDescription = null)
                },
                trailingContent = {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToAttrezzaturaList() }
            )
            HorizontalDivider()

            Spacer(modifier = Modifier.height(24.dp))

            // Sezione Salvataggio PDF - Selettore modalità
            /*StorageModeSelector(
                currentMode = exportSettingsState.storageMode,
                onModeSelected = { mode ->
                    if (mode == exportSettingsState.storageMode) return@StorageModeSelector
                    // Check if current mode is configured
                    val isCurrentConfigured = when (exportSettingsState.storageMode) {
                        StorageMode.GOOGLE_DRIVE -> true // always show confirmation leaving Drive
                        StorageMode.LOCAL -> exportSettingsState.folderUri != null
                    }
                    if (isCurrentConfigured) {
                        pendingModeChange = mode
                    } else {
                        exportSettingsViewModel.setStorageMode(mode)
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
            */

            // Sezione configurazione specifica per modalità
            when (exportSettingsState.storageMode) {
                StorageMode.GOOGLE_DRIVE -> {
                    // Google Drive settings
                    GoogleDriveSettingsSection()
                }
                StorageMode.LOCAL -> {
                    // Local folder settings (SAF)
                    ExportSettingsSection(
                        folderUri = exportSettingsState.folderUri,
                        folderName = exportSettingsState.folderName,
                        exportBehavior = exportSettingsState.exportBehavior,
                        isFolderAccessible = exportSettingsState.isFolderAccessible,
                        onFolderSelected = { uri ->
                            exportSettingsViewModel.setExportFolder(uri)
                        },
                        onClearFolder = {
                            exportSettingsViewModel.clearExportFolder()
                        },
                        onBehaviorChanged = { behavior ->
                            exportSettingsViewModel.setExportBehavior(behavior)
                        }
                    )
                }
            }
        }
    }
}
