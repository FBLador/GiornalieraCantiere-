package com.idroambiente.giornalieradicantiere.ui.impostazioni

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idroambiente.giornalieradicantiere.data.preferences.ExportBehavior
import com.idroambiente.giornalieradicantiere.data.preferences.PreferencesRepository
import com.idroambiente.giornalieradicantiere.data.preferences.StorageMode
import com.idroambiente.giornalieradicantiere.services.FileStorageService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExportSettingsUiState(
    val storageMode: StorageMode = StorageMode.GOOGLE_DRIVE,
    val folderUri: String? = null,
    val folderName: String? = null,
    val exportBehavior: ExportBehavior = ExportBehavior.SHARE,
    val isFolderAccessible: Boolean = true,
    val isLoading: Boolean = true
)

@HiltViewModel
class ExportSettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val fileStorageService: FileStorageService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExportSettingsUiState())
    val uiState: StateFlow<ExportSettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            combine(
                preferencesRepository.storageMode,
                preferencesRepository.exportFolderUri,
                preferencesRepository.exportFolderName,
                preferencesRepository.exportBehavior
            ) { mode, uri, name, behavior ->
                data class Settings(
                    val mode: StorageMode,
                    val uri: String?,
                    val name: String?,
                    val behavior: ExportBehavior
                )
                Settings(mode, uri, name, behavior)
            }.collect { settings ->
                val isAccessible = if (settings.uri != null) {
                    fileStorageService.isConfiguredFolderAccessible()
                } else {
                    true
                }
                
                _uiState.update {
                    it.copy(
                        storageMode = settings.mode,
                        folderUri = settings.uri,
                        folderName = settings.name,
                        exportBehavior = settings.behavior,
                        isFolderAccessible = isAccessible,
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Imposta la modalità di salvataggio.
     * Se l'utente sceglie Google Drive, imposta anche il comportamento su "Salva automaticamente"
     * così il PDF viene salvato su Drive senza mostrare il chooser di condivisione.
     */
    fun setStorageMode(mode: StorageMode) {
        viewModelScope.launch {
            preferencesRepository.setStorageMode(mode)
            if (mode == StorageMode.GOOGLE_DRIVE) {
                preferencesRepository.setExportBehavior(ExportBehavior.SAVE)
            }
        }
    }

    /**
     * Imposta la cartella di esportazione da un URI SAF
     */
    fun setExportFolder(uri: Uri) {
        viewModelScope.launch {
            val folderName = fileStorageService.getFolderDisplayName(uri) ?: "Cartella selezionata"
            preferencesRepository.setExportFolder(uri.toString(), folderName)
        }
    }

    /**
     * Cancella la cartella di esportazione configurata
     */
    fun clearExportFolder() {
        viewModelScope.launch {
            preferencesRepository.clearExportFolder()
        }
    }

    /**
     * Imposta il comportamento all'invio
     */
    fun setExportBehavior(behavior: ExportBehavior) {
        viewModelScope.launch {
            preferencesRepository.setExportBehavior(behavior)
        }
    }
}
