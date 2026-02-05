package com.idroambiente.giornalieradicantiere.services

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.idroambiente.giornalieradicantiere.data.preferences.PreferencesRepository
import com.idroambiente.giornalieradicantiere.data.preferences.StorageMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Risultato dell'operazione di salvataggio
 */
sealed class SaveResult {
    data class Success(
        val uri: Uri?,
        val fileName: String,
        val folderName: String? = null,
        val webViewLink: String? = null
    ) : SaveResult()
    data class Error(val message: String) : SaveResult()
}

@Singleton
class FileStorageService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesRepository: PreferencesRepository,
    private val googleDriveService: GoogleDriveService
) {
    companion object {
        private const val PDF_MIME_TYPE = "application/pdf"
    }

    /**
     * Salva il PDF in base alla modalità configurata (Google Drive o Locale)
     * @param pdfFile Il file PDF da salvare (dalla cache)
     * @param commessaNome Nome della commessa (per organizzazione Google Drive e naming)
     * @param giornalieraData Timestamp della giornaliera (per nome file)
     * @return Risultato dell'operazione
     */
    suspend fun savePdf(
        pdfFile: File,
        commessaNome: String,
        giornalieraData: Long
    ): SaveResult = withContext(Dispatchers.IO) {
        val mode = preferencesRepository.storageMode.first()

        when (mode) {
            StorageMode.GOOGLE_DRIVE -> {
                val rootFolderId = preferencesRepository.googleDriveRootFolderId.first()

                if (rootFolderId.isNullOrEmpty()) {
                    return@withContext SaveResult.Error("Cartella Google Drive non configurata")
                }

                // Upload su Google Drive con organizzazione per commessa
                val result = googleDriveService.uploadPdfForCommessa(
                    pdfFile = pdfFile,
                    commessaNome = commessaNome,
                    data = giornalieraData,
                    rootFolderId = rootFolderId
                )

                result.fold(
                    onSuccess = { uploadResult ->
                        pdfFile.delete()
                        SaveResult.Success(
                            uri = null,
                            fileName = uploadResult.fileName,
                            folderName = uploadResult.folderName,
                            webViewLink = uploadResult.webViewLink
                        )
                    },
                    onFailure = { error ->
                        SaveResult.Error(error.message ?: "Errore durante l'upload su Google Drive")
                    }
                )
            }
            StorageMode.LOCAL -> {
                // Rinomina il file con la naming convention
                val newFileName = generateFileName(commessaNome, giornalieraData)
                val renamedFile = File(pdfFile.parent, newFileName)
                
                if (pdfFile.name != newFileName) {
                    pdfFile.renameTo(renamedFile)
                }
                
                // Salva localmente via SAF
                savePdfToConfiguredFolder(renamedFile)
            }
        }
    }

    /**
     * Genera il nome file usando la naming convention: {CommessaNome}_{YYYY-MM-DD}.pdf
     */
    private fun generateFileName(commessaNome: String, data: Long): String {
        val sanitizedNome = commessaNome
            .replace(Regex("[\\\\/:*?\"<>|]"), "") // Rimuovi caratteri illegali
            .replace(Regex("\\s+"), "_") // Spazi in underscore
            .trim()
            .take(50) // Limita lunghezza
            .ifEmpty { "Commessa" }
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.ITALY).format(Date(data))
        return "${sanitizedNome}_${dateStr}.pdf"
    }

    /**
     * Salva il PDF nella cartella configurata via SAF
     * @param pdfFile Il file PDF da salvare (dalla cache)
     * @return Risultato dell'operazione con URI del file salvato
     */
    suspend fun savePdfToConfiguredFolder(pdfFile: File): SaveResult = withContext(Dispatchers.IO) {
        try {
            val folderUriString = preferencesRepository.exportFolderUri.first()
            
            if (folderUriString.isNullOrEmpty()) {
                return@withContext SaveResult.Error("Nessuna cartella di esportazione configurata")
            }

            val folderUri = Uri.parse(folderUriString)
            val documentFile = DocumentFile.fromTreeUri(context, folderUri)
            
            if (documentFile == null || !documentFile.canWrite()) {
                return@withContext SaveResult.Error("Impossibile scrivere nella cartella selezionata")
            }

            // Crea il file nella cartella di destinazione
            val newFile = documentFile.createFile(PDF_MIME_TYPE, pdfFile.nameWithoutExtension)
            
            if (newFile == null) {
                return@withContext SaveResult.Error("Impossibile creare il file nella cartella")
            }

            // Copia il contenuto
            context.contentResolver.openOutputStream(newFile.uri)?.use { outputStream ->
                pdfFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return@withContext SaveResult.Error("Impossibile aprire il file per la scrittura")

            // Elimina il file dalla cache
            pdfFile.delete()

            SaveResult.Success(uri = newFile.uri, fileName = newFile.name ?: pdfFile.name)
        } catch (e: SecurityException) {
            SaveResult.Error("Permessi insufficienti per scrivere nella cartella")
        } catch (e: Exception) {
            SaveResult.Error("Errore durante il salvataggio: ${e.message}")
        }
    }

    /**
     * Salva il PDF riepilogo in base alla modalità configurata (Google Drive o Locale)
     * @param pdfFile Il file PDF da salvare (dalla cache)
     * @param commessaNome Nome della commessa (per organizzazione Google Drive)
     * @param fileName Nome del file PDF (es: Riepilogo_Commessa_X_2024-01-15_2024-01-31.pdf)
     * @return Risultato dell'operazione
     */
    suspend fun saveRiepilogoPdf(
        pdfFile: File,
        commessaNome: String,
        fileName: String
    ): SaveResult = withContext(Dispatchers.IO) {
        val mode = preferencesRepository.storageMode.first()

        when (mode) {
            StorageMode.GOOGLE_DRIVE -> {
                val rootFolderId = preferencesRepository.googleDriveRootFolderId.first()

                if (rootFolderId.isNullOrEmpty()) {
                    return@withContext SaveResult.Error("Cartella Google Drive non configurata")
                }

                val result = googleDriveService.uploadRiepilogoPdfForCommessa(
                    pdfFile = pdfFile,
                    commessaNome = commessaNome,
                    fileName = fileName,
                    rootFolderId = rootFolderId
                )

                result.fold(
                    onSuccess = { uploadResult ->
                        pdfFile.delete()
                        SaveResult.Success(
                            uri = null,
                            fileName = uploadResult.fileName,
                            folderName = uploadResult.folderName,
                            webViewLink = uploadResult.webViewLink
                        )
                    },
                    onFailure = { error ->
                        SaveResult.Error(error.message ?: "Errore durante l'upload su Google Drive")
                    }
                )
            }
            StorageMode.LOCAL -> {
                // Rinomina il file con il nome desiderato
                val renamedFile = File(pdfFile.parent, fileName)
                if (pdfFile.name != fileName) {
                    pdfFile.renameTo(renamedFile)
                }
                savePdfToConfiguredFolder(renamedFile)
            }
        }
    }

    /**
     * Crea un Intent per condividere il PDF
     * @param pdfFile Il file PDF da condividere (dalla cache)
     * @return Intent configurato per la condivisione
     */
    fun createShareIntent(pdfFile: File): Intent {
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = PDF_MIME_TYPE
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "Rapporto Giornaliero - ${pdfFile.nameWithoutExtension}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Crea un Intent chooser per la condivisione
     * @param pdfFile Il file PDF da condividere
     * @return Intent chooser
     */
    fun createShareChooserIntent(pdfFile: File): Intent {
        val shareIntent = createShareIntent(pdfFile)
        return Intent.createChooser(shareIntent, "Condividi rapporto giornaliero")
    }

    /**
     * Verifica se la cartella configurata è ancora accessibile
     * @return true se la cartella è accessibile e scrivibile
     */
    suspend fun isConfiguredFolderAccessible(): Boolean = withContext(Dispatchers.IO) {
        try {
            val folderUriString = preferencesRepository.exportFolderUri.first()
            
            if (folderUriString.isNullOrEmpty()) {
                return@withContext false
            }

            val folderUri = Uri.parse(folderUriString)
            val documentFile = DocumentFile.fromTreeUri(context, folderUri)
            
            documentFile?.exists() == true && documentFile.canWrite()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Ottiene il nome visualizzato della cartella da un URI SAF
     * @param uri URI della cartella
     * @return Nome della cartella o null se non disponibile
     */
    fun getFolderDisplayName(uri: Uri): String? {
        return try {
            val documentFile = DocumentFile.fromTreeUri(context, uri)
            documentFile?.name
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Salva il PDF nella cartella Downloads dell'app come fallback
     * @param pdfFile Il file PDF da salvare
     * @return URI del file salvato
     */
    suspend fun savePdfToAppStorage(pdfFile: File): SaveResult = withContext(Dispatchers.IO) {
        try {
            val appDir = File(context.getExternalFilesDir(null), "exports")
            if (!appDir.exists()) {
                appDir.mkdirs()
            }

            val destFile = File(appDir, pdfFile.name)
            pdfFile.copyTo(destFile, overwrite = true)
            
            // Elimina il file dalla cache
            pdfFile.delete()

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                destFile
            )

            SaveResult.Success(uri = uri, fileName = destFile.name)
        } catch (e: Exception) {
            SaveResult.Error("Errore durante il salvataggio locale: ${e.message}")
        }
    }

    /**
     * Crea un Intent per aprire il file PDF
     * @param uri URI del file PDF
     * @return Intent per aprire il file
     */
    fun createOpenFileIntent(uri: Uri): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, PDF_MIME_TYPE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
