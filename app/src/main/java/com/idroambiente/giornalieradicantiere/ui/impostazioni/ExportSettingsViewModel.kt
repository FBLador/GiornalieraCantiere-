package com.idroambiente.giornalieradicantiere.ui.impostazioni

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idroambiente.giornalieradicantiere.data.preferences.ExportBehavior
import com.idroambiente.giornalieradicantiere.data.preferences.PreferencesRepository
import com.idroambiente.giornalieradicantiere.data.preferences.StorageMode
import com.idroambiente.giornalieradicantiere.notifications.DailyReminderScheduler
import com.idroambiente.giornalieradicantiere.services.FileStorageService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.idroambiente.giornalieradicantiere.data.preferences.PreferencesRepository.Companion.DEFAULT_REMINDER_DAYS
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExportSettingsUiState(
    val storageMode: StorageMode = StorageMode.GOOGLE_DRIVE,
    val folderUri: String? = null,
    val folderName: String? = null,
    val exportBehavior: ExportBehavior = ExportBehavior.SHARE,
    val dailyReminderEnabled: Boolean = false,
    val dailyReminderHour: Int = 18,
    val dailyReminderMinute: Int = 0,
    val dailyReminderDays: Set<Int> = DEFAULT_REMINDER_DAYS,
    val isFolderAccessible: Boolean = true,
    val isLoading: Boolean = true
)

private data class StorageSettings(
    val mode: StorageMode,
    val uri: String?,
    val name: String?,
    val behavior: ExportBehavior
)

private data class ReminderSettings(
    val enabled: Boolean,
    val hour: Int,
    val minute: Int,
    val days: Set<Int>
)

@HiltViewModel
class ExportSettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val fileStorageService: FileStorageService,
    private val dailyReminderScheduler: DailyReminderScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExportSettingsUiState())
    val uiState: StateFlow<ExportSettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val storageFlow = combine(
                preferencesRepository.storageMode,
                preferencesRepository.exportFolderUri,
                preferencesRepository.exportFolderName,
                preferencesRepository.exportBehavior
            ) { mode, uri, name, behavior ->
                StorageSettings(mode, uri, name, behavior)
            }

            val reminderFlow = combine(
                preferencesRepository.dailyReminderEnabled,
                preferencesRepository.dailyReminderHour,
                preferencesRepository.dailyReminderMinute,
                preferencesRepository.dailyReminderDays
            ) { enabled, hour, minute, days ->
                ReminderSettings(enabled, hour, minute, days)
            }

            combine(storageFlow, reminderFlow) { storage, reminder ->
                Pair(storage, reminder)
            }.collect { (storage, reminder) ->
                val isAccessible = if (storage.uri != null) {
                    fileStorageService.isConfiguredFolderAccessible()
                } else {
                    true
                }
                
                _uiState.update {
                    it.copy(
                        storageMode = storage.mode,
                        folderUri = storage.uri,
                        folderName = storage.name,
                        exportBehavior = storage.behavior,
                        dailyReminderEnabled = reminder.enabled,
                        dailyReminderHour = reminder.hour,
                        dailyReminderMinute = reminder.minute,
                        dailyReminderDays = reminder.days,
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

    fun setDailyReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDailyReminderEnabled(enabled)
            dailyReminderScheduler.syncScheduleFromPreferences()
        }
    }

    fun setDailyReminderDays(days: Set<Int>) {
        viewModelScope.launch {
            preferencesRepository.setDailyReminderDays(days)
        }
    }

    fun setDailyReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            preferencesRepository.setDailyReminderTime(hour, minute)
            dailyReminderScheduler.syncScheduleFromPreferences()
        }
    }

    fun scheduleTestReminder() {
        dailyReminderScheduler.scheduleTestReminder()
    }
}
