package com.idroambiente.giornalieradicantiere.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    // ==================== Storage Mode ====================
    
    /**
     * Modalità di salvataggio corrente: LOCAL o GOOGLE_DRIVE
     */
    val tema: Flow<TemaApp> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.TEMA]?.let { runCatching { TemaApp.valueOf(it) }.getOrNull() } ?: TemaApp.SISTEMA
    }

    suspend fun setTema(tema: TemaApp) {
        dataStore.edit { it[PreferencesKeys.TEMA] = tema.name }
    }

    val storageMode: Flow<StorageMode> = dataStore.data.map { preferences ->
        val value = preferences[PreferencesKeys.STORAGE_MODE] ?: StorageMode.LOCAL.value
        StorageMode.fromValue(value)
    }

    /**
     * Imposta la modalità di salvataggio
     */
    suspend fun setStorageMode(mode: StorageMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.STORAGE_MODE] = mode.value
        }
    }

    // ==================== Local Folder (SAF) ====================
    
    /**
     * URI della cartella selezionata (persistente grazie a takePersistableUriPermission)
     */
    val exportFolderUri: Flow<String?> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.EXPORT_FOLDER_URI]
    }

    /**
     * Nome visualizzato della cartella
     */
    val exportFolderName: Flow<String?> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.EXPORT_FOLDER_NAME]
    }

    /**
     * Imposta la cartella di esportazione locale
     */
    suspend fun setExportFolder(uri: String, name: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.EXPORT_FOLDER_URI] = uri
            preferences[PreferencesKeys.EXPORT_FOLDER_NAME] = name
        }
    }

    /**
     * Cancella la configurazione della cartella locale
     */
    suspend fun clearExportFolder() {
        dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.EXPORT_FOLDER_URI)
            preferences.remove(PreferencesKeys.EXPORT_FOLDER_NAME)
        }
    }

    // ==================== Google Drive ====================
    
    /**
     * ID della cartella root su Google Drive
     */
    val googleDriveRootFolderId: Flow<String?> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.GOOGLE_DRIVE_ROOT_FOLDER_ID]
    }

    /**
     * Nome della cartella root su Google Drive
     */
    val googleDriveRootFolderName: Flow<String?> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.GOOGLE_DRIVE_ROOT_FOLDER_NAME]
    }

    /**
     * Email dell'account Google connesso
     */
    val googleUserEmail: Flow<String?> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.GOOGLE_USER_EMAIL]
    }

    /**
     * Imposta la cartella root di Google Drive
     */
    suspend fun setGoogleDriveRootFolder(folderId: String, folderName: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.GOOGLE_DRIVE_ROOT_FOLDER_ID] = folderId
            preferences[PreferencesKeys.GOOGLE_DRIVE_ROOT_FOLDER_NAME] = folderName
        }
    }

    /**
     * Imposta l'email dell'utente Google
     */
    suspend fun setGoogleUserEmail(email: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.GOOGLE_USER_EMAIL] = email
        }
    }

    /**
     * Cancella tutte le configurazioni Google Drive
     */
    suspend fun clearGoogleDrive() {
        dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.GOOGLE_DRIVE_ROOT_FOLDER_ID)
            preferences.remove(PreferencesKeys.GOOGLE_DRIVE_ROOT_FOLDER_NAME)
            preferences.remove(PreferencesKeys.GOOGLE_USER_EMAIL)
            preferences.remove(PreferencesKeys.GOOGLE_USER_DISPLAY_NAME)
        }
    }

    // ==================== Setup / Onboarding ====================

    /**
     * Se il setup iniziale è stato completato
     */
    val setupCompleted: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SETUP_COMPLETED] ?: false
    }

    /**
     * Imposta lo stato di completamento del setup
     */
    suspend fun setSetupCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SETUP_COMPLETED] = completed
        }
    }

    /**
     * Display name dell'account Google connesso
     */
    val googleUserDisplayName: Flow<String?> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.GOOGLE_USER_DISPLAY_NAME]
    }

    /**
     * Imposta il display name dell'utente Google
     */
    suspend fun setGoogleUserDisplayName(name: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.GOOGLE_USER_DISPLAY_NAME] = name
        }
    }

    // ==================== Daily Reminder Notification ====================

    /**
     * Se il promemoria giornaliero è attivo.
     */
    val dailyReminderEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DAILY_REMINDER_ENABLED] ?: false
    }

    /**
     * Ora del promemoria giornaliero (0..23).
     */
    val dailyReminderHour: Flow<Int> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DAILY_REMINDER_HOUR] ?: 18
    }

    /**
     * Minuto del promemoria giornaliero (0..59).
     */
    val dailyReminderMinute: Flow<Int> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DAILY_REMINDER_MINUTE] ?: 0
    }

    /**
     * Giorni della settimana in cui il promemoria è attivo (Calendar.DAY_OF_WEEK: 2=LUN..6=VEN).
     */
    val dailyReminderDays: Flow<Set<Int>> = dataStore.data.map { preferences ->
        val stringSet = preferences[PreferencesKeys.DAILY_REMINDER_DAYS]
        stringSet?.mapNotNull { it.toIntOrNull() }?.toSet() ?: DEFAULT_REMINDER_DAYS
    }

    /**
     * Imposta i giorni della settimana per il promemoria.
     */
    suspend fun setDailyReminderDays(days: Set<Int>) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DAILY_REMINDER_DAYS] = days.map { it.toString() }.toSet()
        }
    }

    /**
     * Abilita/disabilita il promemoria giornaliero.
     */
    suspend fun setDailyReminderEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DAILY_REMINDER_ENABLED] = enabled
        }
    }

    /**
     * Imposta l'orario del promemoria giornaliero.
     */
    suspend fun setDailyReminderTime(hour: Int, minute: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DAILY_REMINDER_HOUR] = hour.coerceIn(0, 23)
            preferences[PreferencesKeys.DAILY_REMINDER_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    // ==================== Codici Articolo CSV ====================

    val codiceArticoloOreLavoro: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CODICE_ARTICOLO_ORE_LAVORO] ?: "IAPERSINT"
    }

    val codiceArticoloOreViaggio: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CODICE_ARTICOLO_ORE_VIAGGIO] ?: "IAPERSVIAGGI"
    }

    suspend fun setCodiceArticoloOreLavoro(codice: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.CODICE_ARTICOLO_ORE_LAVORO] = codice
        }
    }

    suspend fun setCodiceArticoloOreViaggio(codice: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.CODICE_ARTICOLO_ORE_VIAGGIO] = codice
        }
    }

    // ==================== Export Behavior ====================
    
    /**
     * Comportamento dopo invio: "save" | "share" | "both"
     */
    val exportBehavior: Flow<ExportBehavior> = dataStore.data.map { preferences ->
        val value = preferences[PreferencesKeys.EXPORT_BEHAVIOR] ?: ExportBehavior.SHARE.value
        ExportBehavior.fromValue(value)
    }

    /**
     * Imposta il comportamento all'invio
     */
    suspend fun setExportBehavior(behavior: ExportBehavior) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.EXPORT_BEHAVIOR] = behavior.value
        }
    }

    companion object {
        /** Lun-Ven (Calendar.MONDAY..Calendar.FRIDAY = 2..6) */
        val DEFAULT_REMINDER_DAYS = setOf(2, 3, 4, 5, 6)
    }
}
