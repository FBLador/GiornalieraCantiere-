package com.idroambiente.giornalieradicantiere.ui.impostazioni

import com.idroambiente.giornalieradicantiere.services.DriveFolder

data class FolderPickerState(
    val currentFolderId: String = "root",
    val currentFolderName: String = "Il mio Drive",
    val folders: List<DriveFolder> = emptyList(),
    val sharedFolders: List<DriveFolder> = emptyList(),
    val isLoading: Boolean = false,
    val breadcrumbs: List<Pair<String, String>> = listOf("root" to "Il mio Drive"),
    val showCreateFolderDialog: Boolean = false
)
