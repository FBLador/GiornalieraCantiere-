package com.idroambiente.giornalieradicantiere.services

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.idroambiente.giornalieradicantiere.data.preferences.PreferencesRepository
import com.idroambiente.giornalieradicantiere.data.preferences.StorageMode
import com.idroambiente.giornalieradicantiere.database.entities.ArchivioFile
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
        val webViewLink: String? = null,
        /** Riferimento per ritrovare il file: ID Drive, URI SAF o percorso locale. */
        val ref: String? = null,
        val archivio: ArchivioFile? = null
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
        const val PDF_MIME_TYPE = "application/pdf"
        const val CSV_MIME_TYPE = "text/csv"
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
                            webViewLink = uploadResult.webViewLink,
                            ref = uploadResult.fileId,
                            archivio = ArchivioFile.DRIVE
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

            SaveResult.Success(
                uri = newFile.uri,
                fileName = newFile.name ?: pdfFile.name,
                ref = newFile.uri.toString(),
                archivio = ArchivioFile.LOCALE
            )
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
                            webViewLink = uploadResult.webViewLink,
                            ref = uploadResult.fileId,
                            archivio = ArchivioFile.DRIVE
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
     * Genera il nome file CSV usando la naming convention: {CommessaNome}_{YYYY-MM-DD}.csv
     */
    fun generateCsvFileName(commessaNome: String, data: Long): String {
        val sanitizedNome = commessaNome
            .replace(Regex("[\\\\/:*?\"<>|]"), "")
            .replace(Regex("\\s+"), "_")
            .trim()
            .take(50)
            .ifEmpty { "Commessa" }
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.ITALY).format(Date(data))
        return "${sanitizedNome}_${dateStr}.csv"
    }

    /**
     * Salva il CSV in base alla modalità configurata (Google Drive o Locale)
     */
    suspend fun saveCsv(
        csvFile: File,
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

                val result = googleDriveService.uploadCsvForCommessa(
                    csvFile = csvFile,
                    commessaNome = commessaNome,
                    data = giornalieraData,
                    rootFolderId = rootFolderId
                )

                result.fold(
                    onSuccess = { uploadResult ->
                        csvFile.delete()
                        SaveResult.Success(
                            uri = null,
                            fileName = uploadResult.fileName,
                            folderName = uploadResult.folderName,
                            webViewLink = uploadResult.webViewLink,
                            ref = uploadResult.fileId,
                            archivio = ArchivioFile.DRIVE
                        )
                    },
                    onFailure = { error ->
                        SaveResult.Error(error.message ?: "Errore durante l'upload CSV su Google Drive")
                    }
                )
            }
            StorageMode.LOCAL -> {
                val newFileName = generateCsvFileName(commessaNome, giornalieraData)
                val renamedFile = File(csvFile.parent, newFileName)

                if (csvFile.name != newFileName) {
                    csvFile.renameTo(renamedFile)
                }

                saveCsvToConfiguredFolder(renamedFile)
            }
        }
    }

    /**
     * Salva il CSV nella cartella configurata via SAF
     */
    suspend fun saveCsvToConfiguredFolder(csvFile: File): SaveResult = withContext(Dispatchers.IO) {
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

            val newFile = documentFile.createFile(CSV_MIME_TYPE, csvFile.nameWithoutExtension)

            if (newFile == null) {
                return@withContext SaveResult.Error("Impossibile creare il file CSV nella cartella")
            }

            context.contentResolver.openOutputStream(newFile.uri)?.use { outputStream ->
                csvFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return@withContext SaveResult.Error("Impossibile aprire il file CSV per la scrittura")

            csvFile.delete()

            SaveResult.Success(
                uri = newFile.uri,
                fileName = newFile.name ?: csvFile.name,
                ref = newFile.uri.toString(),
                archivio = ArchivioFile.LOCALE
            )
        } catch (e: SecurityException) {
            SaveResult.Error("Permessi insufficienti per scrivere nella cartella")
        } catch (e: Exception) {
            SaveResult.Error("Errore durante il salvataggio CSV: ${e.message}")
        }
    }

    /**
     * Salva il CSV riepilogo in base alla modalità configurata (Google Drive o Locale)
     */
    suspend fun saveRiepilogoCsv(
        csvFile: File,
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

                val result = googleDriveService.uploadRiepilogoCsvForCommessa(
                    csvFile = csvFile,
                    commessaNome = commessaNome,
                    fileName = fileName,
                    rootFolderId = rootFolderId
                )

                result.fold(
                    onSuccess = { uploadResult ->
                        csvFile.delete()
                        SaveResult.Success(
                            uri = null,
                            fileName = uploadResult.fileName,
                            folderName = uploadResult.folderName,
                            webViewLink = uploadResult.webViewLink,
                            ref = uploadResult.fileId,
                            archivio = ArchivioFile.DRIVE
                        )
                    },
                    onFailure = { error ->
                        SaveResult.Error(error.message ?: "Errore durante l'upload CSV su Google Drive")
                    }
                )
            }
            StorageMode.LOCAL -> {
                val renamedFile = File(csvFile.parent, fileName)
                if (csvFile.name != fileName) {
                    csvFile.renameTo(renamedFile)
                }
                saveCsvToConfiguredFolder(renamedFile)
            }
        }
    }

    /**
     * Salva il CSV nella cartella dell'app come fallback
     */
    suspend fun saveCsvToAppStorage(csvFile: File): SaveResult = withContext(Dispatchers.IO) {
        try {
            val appDir = File(context.getExternalFilesDir(null), "exports")
            if (!appDir.exists()) {
                appDir.mkdirs()
            }

            val destFile = File(appDir, csvFile.name)
            csvFile.copyTo(destFile, overwrite = true)

            csvFile.delete()

            SaveResult.Success(uri = null, fileName = destFile.name, ref = destFile.absolutePath, archivio = ArchivioFile.APP)
        } catch (e: Exception) {
            SaveResult.Error("Errore durante il salvataggio CSV locale: ${e.message}")
        }
    }

    // ───────────────────────── Sostituzione file (modifica giornaliere e report) ─────────────────────────

    /** Sottocartella di destinazione dentro l'archivio. */
    sealed interface Cartella {
        data class Giornaliere(val commessaNome: String) : Cartella
        data class Riepiloghi(val commessaNome: String) : Cartella
    }

    /** Archivio in uso adesso, in base alle impostazioni. */
    suspend fun archivioCorrente(): ArchivioFile = when (preferencesRepository.storageMode.first()) {
        StorageMode.GOOGLE_DRIVE -> ArchivioFile.DRIVE
        StorageMode.LOCAL -> ArchivioFile.LOCALE
    }

    /**
     * Sostituisce un file già inviato senza creare doppioni (vedi [FileReplacer]).
     *
     * @param knownRef riferimento salvato all'invio; viene usato solo se il file è nello stesso archivio
     * @param oldName nome attuale del file (per ritrovare i file inviati prima di questa versione)
     * @param newName nome che deve avere (diverso da oldName se è cambiata la data)
     * @param fallbackName nome da usare se il file originale non si può modificare
     */
    suspend fun sostituisciFile(
        file: File,
        mimeType: String,
        archivio: ArchivioFile,
        knownRef: String?,
        knownArchivio: ArchivioFile?,
        oldName: String,
        newName: String,
        fallbackName: String,
        cartella: Cartella
    ): ReplaceResult = withContext(Dispatchers.IO) {
        val ref = knownRef?.takeIf { knownArchivio == archivio }
        val ops: RemoteOps = when (archivio) {
            ArchivioFile.DRIVE -> {
                val rootFolderId = preferencesRepository.googleDriveRootFolderId.first()
                    ?: return@withContext ReplaceResult.Failed("Cartella Google Drive non configurata")
                val folderId = when (cartella) {
                    is Cartella.Giornaliere -> googleDriveService.getGiornaliereFolderId(cartella.commessaNome, rootFolderId)
                    is Cartella.Riepiloghi -> googleDriveService.getRiepiloghiFolderId(cartella.commessaNome, rootFolderId)
                }.getOrElse { return@withContext ReplaceResult.Failed(it.message ?: "Errore Google Drive") }
                DriveOps(folderId, file, mimeType)
            }
            ArchivioFile.LOCALE -> {
                val folderUri = preferencesRepository.exportFolderUri.first()
                    ?: return@withContext ReplaceResult.Failed("Nessuna cartella di esportazione configurata")
                SafOps(Uri.parse(folderUri), file, mimeType)
            }
            ArchivioFile.APP, ArchivioFile.CONDIVISO -> AppOps(appExportsDir(), file)
        }
        FileReplacer.replace(ref, oldName, newName, fallbackName, ops)
    }

    /**
     * Scrive un file nel registro modifiche (cartella "Modifiche").
     * @return dove è stato scritto, per mostrarlo all'utente
     */
    suspend fun scriviRegistro(fileName: String, contenuto: String): Result<String> = withContext(Dispatchers.IO) {
        val tmp = File(context.cacheDir, fileName)
        try {
            tmp.writeText(contenuto, Charsets.UTF_8)
            when (archivioCorrente()) {
                ArchivioFile.DRIVE -> {
                    val rootFolderId = preferencesRepository.googleDriveRootFolderId.first()
                        ?: return@withContext Result.failure(Exception("Cartella Google Drive non configurata"))
                    val folderId = googleDriveService.getModificheFolderId(rootFolderId).getOrThrow()
                    googleDriveService.createFile(fileName, folderId, tmp, CSV_MIME_TYPE).getOrThrow()
                    Result.success("Google Drive › Modifiche")
                }
                else -> {
                    val folderUri = preferencesRepository.exportFolderUri.first()
                    val tree = folderUri?.let { DocumentFile.fromTreeUri(context, Uri.parse(it)) }
                    if (tree != null && tree.canWrite()) {
                        val dir = tree.findFile("Modifiche")?.takeIf { it.isDirectory } ?: tree.createDirectory("Modifiche")
                        val doc = dir?.createFile(CSV_MIME_TYPE, FileNaming.senzaEstensione(fileName))
                            ?: throw Exception("Impossibile creare il file del registro")
                        context.contentResolver.openOutputStream(doc.uri)?.use { out -> tmp.inputStream().use { it.copyTo(out) } }
                        Result.success("${tree.name ?: "Cartella"} › Modifiche")
                    } else {
                        val dir = File(appExportsDir(), "Modifiche").apply { mkdirs() }
                        tmp.copyTo(File(dir, fileName), overwrite = true)
                        Result.success("Memoria dell'app › Modifiche")
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            tmp.delete()
        }
    }

    private fun appExportsDir(): File = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }

    /** Google Drive: aggiorna lo stesso file (ID invariato) oppure lo crea nella cartella. */
    private inner class DriveOps(
        private val folderId: String,
        private val file: File,
        private val mimeType: String
    ) : RemoteOps {
        override suspend fun update(ref: String, newName: String): OpResult =
            googleDriveService.updateFile(ref, newName, file, mimeType).fold(
                onSuccess = { OpResult.Ok(it.fileId, it.fileName, it.webViewLink) },
                onFailure = { it.toOpResult() }
            )

        override suspend fun findByName(name: String): FindResult =
            googleDriveService.findFilesByName(name, folderId).fold(
                onSuccess = { FindResult.Found(it) },
                onFailure = { FindResult.Failed(it.message ?: "Errore Google Drive") }
            )

        override suspend fun create(name: String): OpResult =
            googleDriveService.createFile(name, folderId, file, mimeType).fold(
                onSuccess = { OpResult.Ok(it.fileId, it.fileName, it.webViewLink) },
                onFailure = { it.toOpResult() }
            )

        private fun Throwable.toOpResult(): OpResult = when ((this as? DriveHttpException)?.statusCode) {
            404 -> OpResult.NotFound
            403 -> OpResult.Forbidden
            else -> OpResult.Failed(message ?: "Errore Google Drive")
        }
    }

    /** Cartella scelta sul telefono (SAF): rinomina il documento e lo sovrascrive. */
    private inner class SafOps(
        private val folderUri: Uri,
        private val file: File,
        private val mimeType: String
    ) : RemoteOps {
        override suspend fun update(ref: String, newName: String): OpResult {
            return try {
                var uri = Uri.parse(ref)
                val doc = DocumentFile.fromSingleUri(context, uri)
                if (doc == null || !doc.exists()) return OpResult.NotFound
                if (doc.name != newName) {
                    uri = DocumentsContract.renameDocument(context.contentResolver, uri, newName)
                        ?: return OpResult.Failed("Impossibile rinominare il file")
                }
                writeTo(uri)
                OpResult.Ok(uri.toString(), DocumentFile.fromSingleUri(context, uri)?.name ?: newName)
            } catch (e: SecurityException) {
                OpResult.Forbidden
            } catch (e: java.io.FileNotFoundException) {
                OpResult.NotFound
            } catch (e: Exception) {
                OpResult.Failed(e.message ?: "Errore durante la scrittura")
            }
        }

        override suspend fun findByName(name: String): FindResult = try {
            val tree = DocumentFile.fromTreeUri(context, folderUri)
            FindResult.Found(listOfNotNull(tree?.findFile(name)?.uri?.toString()))
        } catch (e: Exception) {
            FindResult.Failed(e.message ?: "Cartella non accessibile")
        }

        override suspend fun create(name: String): OpResult = try {
            val tree = DocumentFile.fromTreeUri(context, folderUri)
            val doc = tree?.createFile(mimeType, FileNaming.senzaEstensione(name))
            if (doc == null) {
                OpResult.Failed("Impossibile creare il file nella cartella")
            } else {
                writeTo(doc.uri)
                OpResult.Ok(doc.uri.toString(), doc.name ?: name)
            }
        } catch (e: SecurityException) {
            OpResult.Forbidden
        } catch (e: Exception) {
            OpResult.Failed(e.message ?: "Errore durante la scrittura")
        }

        private fun writeTo(uri: Uri) {
            // "wt" = scrivi troncando: il file vecchio viene sostituito completamente
            context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                file.inputStream().use { it.copyTo(out) }
            } ?: throw java.io.FileNotFoundException("Impossibile aprire il file")
        }
    }

    /** Cartella interna dell'app (fallback e modalità "Condividi"). */
    private class AppOps(private val dir: File, private val file: File) : RemoteOps {
        override suspend fun update(ref: String, newName: String): OpResult {
            val current = File(ref)
            if (!current.exists()) return OpResult.NotFound
            val target = File(current.parentFile ?: dir, newName)
            if (current != target) current.delete()
            file.copyTo(target, overwrite = true)
            return OpResult.Ok(target.absolutePath, target.name)
        }

        override suspend fun findByName(name: String): FindResult =
            FindResult.Found(listOfNotNull(File(dir, name).takeIf { it.exists() }?.absolutePath))

        override suspend fun create(name: String): OpResult {
            val target = File(dir, name)
            file.copyTo(target, overwrite = true)
            return OpResult.Ok(target.absolutePath, target.name)
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

            SaveResult.Success(uri = uri, fileName = destFile.name, ref = destFile.absolutePath, archivio = ArchivioFile.APP)
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
