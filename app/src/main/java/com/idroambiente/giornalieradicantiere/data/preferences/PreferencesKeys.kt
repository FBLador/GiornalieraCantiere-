package com.idroambiente.giornalieradicantiere.data.preferences

import androidx.datastore.preferences.core.stringPreferencesKey

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
