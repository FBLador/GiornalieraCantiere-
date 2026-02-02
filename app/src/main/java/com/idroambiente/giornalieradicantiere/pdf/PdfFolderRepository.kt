package com.idroambiente.giornalieradicantiere.pdf

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class PdfFolderRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getFolderUri(): Uri? {
        val uriString = prefs.getString(KEY_FOLDER_URI, null) ?: return null
        return Uri.parse(uriString)
    }

    fun getFolderLabel(uri: Uri?): String? {
        if (uri == null) return null
        val document = DocumentFile.fromTreeUri(context, uri)
        return document?.name
    }

    fun saveFolderUri(uri: Uri) {
        prefs.edit().putString(KEY_FOLDER_URI, uri.toString()).apply()
    }

    fun persistAccess(uri: Uri) {
        val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
            android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            context.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (_: SecurityException) {
            // Ignore if we cannot persist permissions; we'll fall back to local storage.
        }
    }

    companion object {
        private const val PREFS_NAME = "pdf_preferences"
        private const val KEY_FOLDER_URI = "pdf_folder_uri"
    }
}
