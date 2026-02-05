package com.idroambiente.giornalieradicantiere.ui.impostazioni

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.idroambiente.giornalieradicantiere.services.DriveFolder
import com.idroambiente.giornalieradicantiere.ui.components.AppFAB
import com.idroambiente.giornalieradicantiere.ui.components.PrimaryButton

/**
 * Full-screen dialog for navigating and selecting a Google Drive folder.
 */
@Composable
fun GoogleDriveFolderPickerDialog(
    state: FolderPickerState,
    onNavigateToFolder: (DriveFolder) -> Unit,
    onNavigateToBreadcrumb: (Int) -> Unit,
    onSelectFolder: () -> Unit,
    onCreateFolder: (String) -> Unit,
    onShowCreateFolderDialog: () -> Unit,
    onHideCreateFolderDialog: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                // Header
                FolderPickerHeader(
                    folderName = state.currentFolderName,
                    onClose = onDismiss
                )
                
                HorizontalDivider()
                
                // Breadcrumbs
                BreadcrumbsRow(
                    breadcrumbs = state.breadcrumbs,
                    onBreadcrumbClick = onNavigateToBreadcrumb
                )
                
                HorizontalDivider()
                
                // Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else if (state.currentFolderId == "root") {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                Text(
                                    text = "Condivisi con me",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                            if (state.sharedFolders.isEmpty()) {
                                item {
                                    Text(
                                        text = "Nessuna cartella",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            } else {
                                items(state.sharedFolders) { folder ->
                                    FolderRow(folder = folder, onFolderClick = onNavigateToFolder)
                                }
                            }
                            item {
                                Text(
                                    text = "Il mio Drive",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                            if (state.folders.isEmpty()) {
                                item {
                                    Text(
                                        text = "Nessuna cartella",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            } else {
                                items(state.folders) { folder ->
                                    FolderRow(folder = folder, onFolderClick = onNavigateToFolder)
                                }
                            }
                        }
                    } else if (state.folders.isEmpty()) {
                        Text(
                            text = "Nessuna sottocartella",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                        )
                    } else {
                        FoldersList(
                            folders = state.folders,
                            onFolderClick = onNavigateToFolder
                        )
                    }
                    
                    AppFAB(
                        onClick = onShowCreateFolderDialog,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Crea cartella"
                        )
                    }
                }
                
                HorizontalDivider()
                
                // Footer with selection button
                FolderPickerFooter(
                    currentFolderName = state.currentFolderName,
                    onSelectFolder = onSelectFolder
                )
            }
        }
    }
    
    // Create folder dialog
    if (state.showCreateFolderDialog) {
        CreateFolderDialog(
            onConfirm = onCreateFolder,
            onDismiss = onHideCreateFolderDialog
        )
    }
}

@Composable
private fun FolderPickerHeader(
    folderName: String,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Chiudi"
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = "Seleziona cartella",
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
private fun BreadcrumbsRow(
    breadcrumbs: List<Pair<String, String>>,
    onBreadcrumbClick: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        breadcrumbs.forEachIndexed { index, (_, name) ->
            if (index > 0) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
            
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                color = if (index == breadcrumbs.lastIndex) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clickable(enabled = index < breadcrumbs.lastIndex) {
                        onBreadcrumbClick(index)
                    }
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun FolderRow(
    folder: DriveFolder,
    onFolderClick: (DriveFolder) -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = folder.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
        modifier = Modifier.clickable { onFolderClick(folder) }
    )
    HorizontalDivider()
}

@Composable
private fun FoldersList(
    folders: List<DriveFolder>,
    onFolderClick: (DriveFolder) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(folders) { folder ->
            FolderRow(folder = folder, onFolderClick = onFolderClick)
        }
    }
}

@Composable
private fun FolderPickerFooter(
    currentFolderName: String,
    onSelectFolder: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Le sottocartelle per commessa verranno create automaticamente",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        PrimaryButton(
            onClick = onSelectFolder,
            modifier = Modifier.fillMaxWidth(),
            content = { Text("Seleziona \"$currentFolderName\"") }
        )
    }
}

@Composable
private fun CreateFolderDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var folderName by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuova cartella") },
        text = {
            OutlinedTextField(
                value = folderName,
                onValueChange = { folderName = it },
                label = { Text("Nome cartella") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(folderName) },
                enabled = folderName.isNotBlank()
            ) {
                Text("Crea")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}
