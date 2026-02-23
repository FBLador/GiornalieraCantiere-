package com.idroambiente.giornalieradicantiere.data.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

object PreferencesKeys {
    // Local folder settings (SAF)
    val EXPORT_FOLDER_URI = stringPreferencesKey("export_folder_uri")
    val EXPORT_FOLDER_NAME = stringPreferencesKey("export_folder_name")
    
    // Export behavior
    val EXPORT_BEHAVIOR = stringPreferencesKey("export_behavior")
    
    // Storage mode
    val STORAGE_MODE = stringPreferencesKey("storage_mode")
    
    // Google Drive settings
    val GOOGLE_DRIVE_ROOT_FOLDER_ID = stringPreferencesKey("google_drive_root_folder_id")
    val GOOGLE_DRIVE_ROOT_FOLDER_NAME = stringPreferencesKey("google_drive_root_folder_name")
    val GOOGLE_USER_EMAIL = stringPreferencesKey("google_user_email")

    // Setup / onboarding
    val SETUP_COMPLETED = booleanPreferencesKey("setup_completed")
    val GOOGLE_USER_DISPLAY_NAME = stringPreferencesKey("google_user_display_name")

    // Daily reminder notification
    val DAILY_REMINDER_ENABLED = booleanPreferencesKey("daily_reminder_enabled")
    val DAILY_REMINDER_HOUR = intPreferencesKey("daily_reminder_hour")
    val DAILY_REMINDER_MINUTE = intPreferencesKey("daily_reminder_minute")
    val DAILY_REMINDER_DAYS = stringSetPreferencesKey("daily_reminder_days")

    // Codici articolo per CSV riepilogo
    val CODICE_ARTICOLO_ORE_LAVORO = stringPreferencesKey("codice_articolo_ore_lavoro")
    val CODICE_ARTICOLO_ORE_VIAGGIO = stringPreferencesKey("codice_articolo_ore_viaggio")
}

/**
 * Modalità di salvataggio PDF
 */
enum class StorageMode(val value: String) {
    LOCAL("local"),             // Cartella locale via SAF
    GOOGLE_DRIVE("google_drive"); // Google Drive con organizzazione per commessa
    
    companion object {
        fun fromValue(value: String): StorageMode {
            return entries.find { it.value == value } ?: LOCAL
        }
    }
}

/**
 * Comportamento all'invio della giornaliera
 */
enum class ExportBehavior(val value: String) {
    SAVE("save"),           // Salva automaticamente nella cartella configurata
    SHARE("share"),         // Mostra opzioni di condivisione
    BOTH("both");           // Salva e poi chiedi se condividere
    
    companion object {
        fun fromValue(value: String): ExportBehavior {
            return entries.find { it.value == value } ?: SHARE
        }
    }
}
