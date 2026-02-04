package com.idroambiente.giornalieradicantiere.ui.impostazioni

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Section for Google Drive configuration in settings.
 * Shows sign-in status and folder selection.
 */
@Composable
fun GoogleDriveSettingsSection(
    modifier: Modifier = Modifier,
    viewModel: GoogleDriveSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Launcher for Google Sign-In
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleSignInResult(result.data)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Google Drive",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Offline banner
        if (uiState.isOffline) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(20.dp)
                    )
                    Text(
                        text = "Nessuna connessione internet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }

        if (uiState.isLoading) {
            // Loading state
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Connessione in corso...")
                }
            }
        } else if (uiState.isSignedIn) {
            // Signed in state
            SignedInContent(
                userDisplayName = uiState.userDisplayName,
                userEmail = uiState.userEmail,
                rootFolderName = uiState.rootFolderName,
                onSignOut = { viewModel.signOut() },
                onSelectFolder = { viewModel.openFolderPicker() }
            )
        } else {
            // Not signed in state
            NotSignedInContent(
                onSignIn = { signInLauncher.launch(viewModel.getSignInIntent()) },
                enabled = !uiState.isOffline
            )
        }

        // Error card
        val errorText = uiState.error
        if (errorText != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(20.dp)
                    )
                    Text(
                        text = errorText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("OK")
                    }
                }
            }
        }

        // Info card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(end = 8.dp, top = 2.dp)
                        .size(16.dp)
                )
                Text(
                    text = "Le giornaliere verranno organizzate automaticamente:\n\n" +
                            "Cartella selezionata/\n" +
                            "  └── Nome Commessa/\n" +
                            "        ├── NomeCommessa_2024-01-15.pdf\n" +
                            "        └── NomeCommessa_2024-01-16.pdf",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    
    // Folder picker dialog
    if (uiState.showFolderPicker) {
        GoogleDriveFolderPickerDialog(
            state = uiState.folderPickerState,
            onNavigateToFolder = { viewModel.navigateToFolder(it) },
            onNavigateToBreadcrumb = { viewModel.navigateToBreadcrumb(it) },
            onSelectFolder = { viewModel.selectCurrentFolder() },
            onCreateFolder = { viewModel.createFolder(it) },
            onShowCreateFolderDialog = { viewModel.showCreateFolderDialog() },
            onHideCreateFolderDialog = { viewModel.hideCreateFolderDialog() },
            onDismiss = { viewModel.closeFolderPicker() }
        )
    }
}

@Composable
private fun SignedInContent(
    userDisplayName: String?,
    userEmail: String?,
    rootFolderName: String?,
    onSignOut: () -> Unit,
    onSelectFolder: () -> Unit
) {
    // Account info
    ListItem(
        headlineContent = { Text(userDisplayName ?: "Account connesso") },
        supportingContent = {
            Text(
                text = userEmail ?: "Email non disponibile",
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        leadingContent = {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            TextButton(onClick = onSignOut) {
                Text("Esci")
            }
        }
    )
    
    HorizontalDivider()
    
    // Folder selection
    ListItem(
        headlineContent = { Text("Cartella principale") },
        supportingContent = {
            Text(
                text = rootFolderName ?: "Nessuna cartella selezionata",
                color = if (rootFolderName != null) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        },
        leadingContent = {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier.clickable(onClick = onSelectFolder)
    )
    
    HorizontalDivider()
}

@Composable
private fun NotSignedInContent(
    onSignIn: () -> Unit,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Cloud,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Connetti il tuo account Google per salvare automaticamente le giornaliere su Drive",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(onClick = onSignIn, enabled = enabled) {
                Text("Connetti con Google")
            }
        }
    }
}
