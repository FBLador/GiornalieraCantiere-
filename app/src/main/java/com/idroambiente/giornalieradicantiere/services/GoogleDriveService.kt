package com.idroambiente.giornalieradicantiere.services

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File as DriveFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Represents a folder on Google Drive
 */
data class DriveFolder(
    val id: String,
    val name: String
)

/**
 * Represents a CSV file on Google Drive
 */
data class DriveCsvFile(
    val id: String,
    val name: String
)

/**
 * Result of a successful file upload
 */
data class UploadResult(
    val fileId: String,
    val fileName: String,
    val folderName: String,
    val webViewLink: String?
)

/** Errore HTTP di Google Drive con il suo codice (403 = permessi, 404 = non trovato). */
class DriveHttpException(val statusCode: Int, message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Service for interacting with Google Drive API.
 * Handles folder management and file uploads with automatic
 * organization by commessa.
 */
@Singleton
class GoogleDriveService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val googleAuthService: GoogleAuthService
) {
    companion object {
        private const val MIME_TYPE_FOLDER = "application/vnd.google-apps.folder"
        private const val MIME_TYPE_PDF = "application/pdf"
        private const val MIME_TYPE_CSV = "text/csv"
        private const val APP_NAME = "GiornalieraCantiere"
    }

    /**
     * Gets a Drive service instance for the current signed-in user.
     */
    private fun getDriveService(): Drive? {
        val account = googleAuthService.getCurrentAccount() ?: return null
        
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_READONLY, DriveScopes.DRIVE_FILE)
        ).apply {
            selectedAccount = account.account
        }

        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName(APP_NAME)
            .build()
    }

    /**
     * Lists folders within a parent folder (or root if parentId is "root" or null).
     * @param parentId The ID of the parent folder, or "root" for root folder
     * @return Result containing list of folders or error
     */
    suspend fun listFolders(parentId: String? = null): Result<List<DriveFolder>> {
        return withContext(Dispatchers.IO) {
            try {
                val driveService = getDriveService()
                    ?: return@withContext Result.failure(Exception("Non autenticato con Google"))

                val actualParentId = parentId ?: "root"
                val query = "'$actualParentId' in parents and mimeType = '$MIME_TYPE_FOLDER' and trashed = false"
                
                val result = driveService.files().list()
                    .setQ(query)
                    .setSpaces("drive")
                    .setFields("files(id, name)")
                    .setOrderBy("name")
                    .execute()

                val folders = result.files?.map { file ->
                    DriveFolder(
                        id = file.id,
                        name = file.name
                    )
                } ?: emptyList()

                Result.success(folders)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Lists folders shared with the current user.
     * @return Result containing list of shared folders or error
     */
    suspend fun listSharedFolders(): Result<List<DriveFolder>> {
        return withContext(Dispatchers.IO) {
            try {
                val driveService = getDriveService()
                    ?: return@withContext Result.failure(Exception("Non autenticato con Google"))

                val query = "sharedWithMe = true and mimeType = '$MIME_TYPE_FOLDER' and trashed = false"
                val result = driveService.files().list()
                    .setQ(query)
                    .setSpaces("drive")
                    .setFields("files(id, name)")
                    .setOrderBy("name")
                    .execute()

                val folders = result.files?.map { file ->
                    DriveFolder(
                        id = file.id,
                        name = file.name
                    )
                } ?: emptyList()

                Result.success(folders)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Creates a new folder in Google Drive.
     * @param name The name of the folder
     * @param parentId The ID of the parent folder
     * @return Result containing the new folder ID or error
     */
    suspend fun createFolder(name: String, parentId: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val driveService = getDriveService()
                    ?: return@withContext Result.failure(Exception("Non autenticato con Google"))

                val folderMetadata = DriveFile().apply {
                    this.name = name
                    mimeType = MIME_TYPE_FOLDER
                    parents = listOf(parentId)
                }

                val folder = driveService.files().create(folderMetadata)
                    .setFields("id")
                    .execute()

                Result.success(folder.id)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Finds or creates a folder for a specific commessa within the root folder.
     * @param commessaNome The name of the commessa (will be sanitized)
     * @param rootFolderId The ID of the root folder where commessa folders are stored
     * @return Result containing the commessa folder ID or error
     */
    suspend fun getOrCreateCommessaFolder(
        commessaNome: String,
        rootFolderId: String
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val driveService = getDriveService()
                    ?: return@withContext Result.failure(Exception("Non autenticato con Google"))

                val sanitizedName = sanitizeFolderName(commessaNome)
                
                // Search for existing folder with this name
                val query = "'$rootFolderId' in parents and name = '$sanitizedName' and mimeType = '$MIME_TYPE_FOLDER' and trashed = false"
                
                val result = driveService.files().list()
                    .setQ(query)
                    .setSpaces("drive")
                    .setFields("files(id)")
                    .execute()

                val existingFolder = result.files?.firstOrNull()
                
                if (existingFolder != null) {
                    Result.success(existingFolder.id)
                } else {
                    // Create new folder
                    createFolder(sanitizedName, rootFolderId)
                }
            } catch (e: GoogleJsonResponseException) {
                val message = when (e.statusCode) {
                    404 -> "La cartella principale non esiste più. Selezionane una nuova nelle impostazioni."
                    403 -> "Permessi insufficienti. Prova a disconnetterti e riconnetterti."
                    else -> "Errore Google Drive (${e.statusCode}). Riprova."
                }
                Result.failure(Exception(message, e))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Finds or creates a subfolder within a parent folder.
     * @param name The name of the subfolder
     * @param parentId The ID of the parent folder
     * @return Result containing the subfolder ID or error
     */
    private suspend fun getOrCreateSubfolder(
        name: String,
        parentId: String
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val driveService = getDriveService()
                    ?: return@withContext Result.failure(Exception("Non autenticato con Google"))

                val query = "'$parentId' in parents and name = '$name' and mimeType = '$MIME_TYPE_FOLDER' and trashed = false"
                val result = driveService.files().list()
                    .setQ(query)
                    .setSpaces("drive")
                    .setFields("files(id)")
                    .execute()

                val existing = result.files?.firstOrNull()
                if (existing != null) {
                    Result.success(existing.id)
                } else {
                    createFolder(name, parentId)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Uploads a PDF file for a giornaliera, organizing it by commessa.
     * Creates the commessa folder and Giornaliere subfolder if they don't exist.
     *
     * @param pdfFile The PDF file to upload
     * @param commessaNome The name of the commessa
     * @param data Timestamp of the giornaliera (used for file naming)
     * @param rootFolderId The ID of the root folder
     * @return Result containing upload details or error
     */
    suspend fun uploadPdfForCommessa(
        pdfFile: File,
        commessaNome: String,
        data: Long,
        rootFolderId: String
    ): Result<UploadResult> {
        return withContext(Dispatchers.IO) {
            try {
                val driveService = getDriveService()
                    ?: return@withContext Result.failure(Exception("Non autenticato con Google"))

                // Get or create commessa folder
                val commessaFolderId = getOrCreateCommessaFolder(commessaNome, rootFolderId)
                    .getOrElse { return@withContext Result.failure(it) }

                // Get or create Giornaliere subfolder
                val giornaliereFolderId = getOrCreateSubfolder("Giornaliere", commessaFolderId)
                    .getOrElse { return@withContext Result.failure(it) }

                val sanitizedCommessaNome = sanitizeFolderName(commessaNome)
                val fileName = generateFileName(sanitizedCommessaNome, data)

                // Create file metadata
                val fileMetadata = DriveFile().apply {
                    name = fileName
                    parents = listOf(giornaliereFolderId)
                }

                // Upload file
                val mediaContent = FileContent(MIME_TYPE_PDF, pdfFile)
                val uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id, name, webViewLink")
                    .execute()

                Result.success(
                    UploadResult(
                        fileId = uploadedFile.id,
                        fileName = uploadedFile.name,
                        folderName = sanitizedCommessaNome,
                        webViewLink = uploadedFile.webViewLink
                    )
                )
            } catch (e: GoogleJsonResponseException) {
                val message = when (e.statusCode) {
                    404 -> "La cartella selezionata non esiste più. Selezionane una nuova nelle impostazioni."
                    403 -> "Permessi insufficienti. Prova a disconnetterti e riconnetterti."
                    507 -> "Spazio Google Drive esaurito."
                    else -> "Errore Google Drive (${e.statusCode}). Riprova."
                }
                Result.failure(Exception(message, e))
            } catch (e: java.net.UnknownHostException) {
                Result.failure(Exception("Errore di rete. Verifica la connessione e riprova.", e))
            } catch (e: java.io.IOException) {
                Result.failure(Exception("Errore di rete. Riprova.", e))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Uploads a riepilogo PDF for a commessa into the Riepiloghi subfolder.
     *
     * @param pdfFile The PDF file to upload
     * @param commessaNome The name of the commessa
     * @param fileName The desired file name for the PDF
     * @param rootFolderId The ID of the root folder
     * @return Result containing upload details or error
     */
    suspend fun uploadRiepilogoPdfForCommessa(
        pdfFile: File,
        commessaNome: String,
        fileName: String,
        rootFolderId: String
    ): Result<UploadResult> {
        return withContext(Dispatchers.IO) {
            try {
                val driveService = getDriveService()
                    ?: return@withContext Result.failure(Exception("Non autenticato con Google"))

                // Get or create commessa folder
                val commessaFolderId = getOrCreateCommessaFolder(commessaNome, rootFolderId)
                    .getOrElse { return@withContext Result.failure(it) }

                // Get or create Riepiloghi subfolder
                val riepiloghiFolderId = getOrCreateSubfolder("Riepiloghi", commessaFolderId)
                    .getOrElse { return@withContext Result.failure(it) }

                val sanitizedCommessaNome = sanitizeFolderName(commessaNome)

                // Create file metadata
                val fileMetadata = DriveFile().apply {
                    name = fileName
                    parents = listOf(riepiloghiFolderId)
                }

                // Upload file
                val mediaContent = FileContent(MIME_TYPE_PDF, pdfFile)
                val uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id, name, webViewLink")
                    .execute()

                Result.success(
                    UploadResult(
                        fileId = uploadedFile.id,
                        fileName = uploadedFile.name,
                        folderName = sanitizedCommessaNome,
                        webViewLink = uploadedFile.webViewLink
                    )
                )
            } catch (e: GoogleJsonResponseException) {
                val message = when (e.statusCode) {
                    404 -> "La cartella selezionata non esiste più. Selezionane una nuova nelle impostazioni."
                    403 -> "Permessi insufficienti. Prova a disconnetterti e riconnetterti."
                    507 -> "Spazio Google Drive esaurito."
                    else -> "Errore Google Drive (${e.statusCode}). Riprova."
                }
                Result.failure(Exception(message, e))
            } catch (e: java.net.UnknownHostException) {
                Result.failure(Exception("Errore di rete. Verifica la connessione e riprova.", e))
            } catch (e: java.io.IOException) {
                Result.failure(Exception("Errore di rete. Riprova.", e))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Uploads a CSV file for a giornaliera, organizing it by commessa.
     * Same folder structure as uploadPdfForCommessa: Root/Commessa/Giornaliere/
     */
    suspend fun uploadCsvForCommessa(
        csvFile: File,
        commessaNome: String,
        data: Long,
        rootFolderId: String
    ): Result<UploadResult> {
        return withContext(Dispatchers.IO) {
            try {
                val driveService = getDriveService()
                    ?: return@withContext Result.failure(Exception("Non autenticato con Google"))

                val commessaFolderId = getOrCreateCommessaFolder(commessaNome, rootFolderId)
                    .getOrElse { return@withContext Result.failure(it) }

                val giornaliereFolderId = getOrCreateSubfolder("Giornaliere", commessaFolderId)
                    .getOrElse { return@withContext Result.failure(it) }

                val fileMetadata = DriveFile().apply {
                    name = csvFile.name
                    parents = listOf(giornaliereFolderId)
                }

                val mediaContent = FileContent(MIME_TYPE_CSV, csvFile)
                val uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id, name, webViewLink")
                    .execute()

                Result.success(
                    UploadResult(
                        fileId = uploadedFile.id,
                        fileName = uploadedFile.name,
                        folderName = sanitizeFolderName(commessaNome),
                        webViewLink = uploadedFile.webViewLink
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Uploads a riepilogo CSV for a commessa into the Riepiloghi subfolder.
     * Same folder structure as uploadRiepilogoPdfForCommessa: Root/Commessa/Riepiloghi/
     */
    suspend fun uploadRiepilogoCsvForCommessa(
        csvFile: File,
        commessaNome: String,
        fileName: String,
        rootFolderId: String
    ): Result<UploadResult> {
        return withContext(Dispatchers.IO) {
            try {
                val driveService = getDriveService()
                    ?: return@withContext Result.failure(Exception("Non autenticato con Google"))

                val commessaFolderId = getOrCreateCommessaFolder(commessaNome, rootFolderId)
                    .getOrElse { return@withContext Result.failure(it) }

                val riepiloghiFolderId = getOrCreateSubfolder("Riepiloghi", commessaFolderId)
                    .getOrElse { return@withContext Result.failure(it) }

                val fileMetadata = DriveFile().apply {
                    name = fileName
                    parents = listOf(riepiloghiFolderId)
                }

                val mediaContent = FileContent(MIME_TYPE_CSV, csvFile)
                val uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id, name, webViewLink")
                    .execute()

                Result.success(
                    UploadResult(
                        fileId = uploadedFile.id,
                        fileName = uploadedFile.name,
                        folderName = sanitizeFolderName(commessaNome),
                        webViewLink = uploadedFile.webViewLink
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Checks if a folder is accessible (exists and not trashed).
     * @param folderId The ID of the folder to check
     * @return true if accessible, false otherwise
     */
    suspend fun isFolderAccessible(folderId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val driveService = getDriveService() ?: return@withContext false

                val file = driveService.files().get(folderId)
                    .setFields("id, trashed")
                    .execute()

                file.trashed != true
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Gets folder details by ID.
     * @param folderId The ID of the folder
     * @return Result containing folder details or error
     */
    suspend fun getFolderDetails(folderId: String): Result<DriveFolder> {
        return withContext(Dispatchers.IO) {
            try {
                val driveService = getDriveService()
                    ?: return@withContext Result.failure(Exception("Non autenticato con Google"))

                val file = driveService.files().get(folderId)
                    .setFields("id, name")
                    .execute()

                Result.success(DriveFolder(file.id, file.name))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Lists CSV files within a folder.
     * @param folderId The ID of the folder to list CSV files from
     * @return Result containing list of CSV files or error
     */
    suspend fun listCsvFiles(folderId: String): Result<List<DriveCsvFile>> {
        return withContext(Dispatchers.IO) {
            try {
                val driveService = getDriveService()
                    ?: return@withContext Result.failure(Exception("Non autenticato con Google"))

                val query = "'$folderId' in parents and mimeType = '$MIME_TYPE_CSV' and trashed = false"

                val result = driveService.files().list()
                    .setQ(query)
                    .setSpaces("drive")
                    .setFields("files(id, name)")
                    .setOrderBy("name")
                    .execute()

                val csvFiles = result.files?.map { file ->
                    DriveCsvFile(id = file.id, name = file.name)
                } ?: emptyList()

                Result.success(csvFiles)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Downloads the text content of a file from Google Drive.
     * @param fileId The ID of the file to download
     * @return Result containing the file content as a String or error
     */
    suspend fun downloadFileContent(fileId: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val driveService = getDriveService()
                    ?: return@withContext Result.failure(Exception("Non autenticato con Google"))

                val inputStream = driveService.files().get(fileId)
                    .executeMediaAsInputStream()

                val content = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                Result.success(content)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // ───────────────────────── Sostituzione file (modifica giornaliere) ─────────────────────────

    /** Cartella {Commessa}/Giornaliere, creata se manca. */
    suspend fun getGiornaliereFolderId(commessaNome: String, rootFolderId: String): Result<String> {
        val commessaFolderId = getOrCreateCommessaFolder(commessaNome, rootFolderId)
            .getOrElse { return Result.failure(it) }
        return getOrCreateSubfolder("Giornaliere", commessaFolderId)
    }

    /** Cartella {Commessa}/Riepiloghi, creata se manca. */
    suspend fun getRiepiloghiFolderId(commessaNome: String, rootFolderId: String): Result<String> {
        val commessaFolderId = getOrCreateCommessaFolder(commessaNome, rootFolderId)
            .getOrElse { return Result.failure(it) }
        return getOrCreateSubfolder("Riepiloghi", commessaFolderId)
    }

    /** Cartella Modifiche nella cartella principale: il registro delle modifiche per l'ufficio. */
    suspend fun getModificheFolderId(rootFolderId: String): Result<String> =
        getOrCreateSubfolder("Modifiche", rootFolderId)

    /** File con quel nome nella cartella (non nel cestino), il più recente per primo. */
    suspend fun findFilesByName(name: String, parentId: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService()
                ?: return@withContext Result.failure(Exception("Non autenticato con Google"))
            val query = "name = '${escapeQuery(name)}' and '${escapeQuery(parentId)}' in parents and trashed = false"
            val result = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name, modifiedTime)")
                .setOrderBy("modifiedTime desc")
                .execute()
            Result.success(result.files?.map { it.id } ?: emptyList())
        } catch (e: GoogleJsonResponseException) {
            Result.failure(DriveHttpException(e.statusCode, messaggioErrore(e.statusCode), e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sostituisce il contenuto di un file esistente e, se serve, lo rinomina.
     * L'ID e il link restano uguali; la versione precedente resta nella cronologia
     * delle versioni di Drive (la segniamo "da conservare per sempre").
     * In caso di errore HTTP restituisce [DriveHttpException] con il codice (403, 404...).
     */
    suspend fun updateFile(fileId: String, newName: String, file: File, mimeType: String): Result<UploadResult> =
        withContext(Dispatchers.IO) {
            try {
                val driveService = getDriveService()
                    ?: return@withContext Result.failure(Exception("Non autenticato con Google"))

                // Best effort: conserva per sempre la versione che stiamo per sostituire
                try {
                    val head = driveService.files().get(fileId).setFields("headRevisionId").execute().headRevisionId
                    if (head != null) {
                        driveService.revisions().update(
                            fileId,
                            head,
                            com.google.api.services.drive.model.Revision().setKeepForever(true)
                        ).execute()
                    }
                } catch (e: GoogleJsonResponseException) {
                    // 404/403 qui li gestisce l'update vero e proprio qui sotto
                    if (e.statusCode == 404 || e.statusCode == 403) {
                        return@withContext Result.failure(DriveHttpException(e.statusCode, messaggioErrore(e.statusCode), e))
                    }
                } catch (_: Exception) {
                }

                val metadata = DriveFile().apply { name = newName }
                val updated = driveService.files().update(fileId, metadata, FileContent(mimeType, file))
                    .setKeepRevisionForever(true)
                    .setFields("id, name, webViewLink")
                    .execute()
                Result.success(UploadResult(updated.id, updated.name, "", updated.webViewLink))
            } catch (e: GoogleJsonResponseException) {
                Result.failure(DriveHttpException(e.statusCode, messaggioErrore(e.statusCode), e))
            } catch (e: java.io.IOException) {
                Result.failure(Exception("Errore di rete. Riprova.", e))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /** Crea un file nuovo nella cartella indicata. */
    suspend fun createFile(name: String, parentId: String, file: File, mimeType: String): Result<UploadResult> =
        withContext(Dispatchers.IO) {
            try {
                val driveService = getDriveService()
                    ?: return@withContext Result.failure(Exception("Non autenticato con Google"))
                val metadata = DriveFile().apply {
                    this.name = name
                    parents = listOf(parentId)
                }
                val created = driveService.files().create(metadata, FileContent(mimeType, file))
                    .setKeepRevisionForever(true)
                    .setFields("id, name, webViewLink")
                    .execute()
                Result.success(UploadResult(created.id, created.name, "", created.webViewLink))
            } catch (e: GoogleJsonResponseException) {
                Result.failure(DriveHttpException(e.statusCode, messaggioErrore(e.statusCode), e))
            } catch (e: java.io.IOException) {
                Result.failure(Exception("Errore di rete. Riprova.", e))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun escapeQuery(value: String): String = value.replace("\\", "\\\\").replace("'", "\\'")

    private fun messaggioErrore(statusCode: Int): String = when (statusCode) {
        404 -> "File o cartella non trovati su Google Drive."
        403 -> "Permessi insufficienti su Google Drive."
        507 -> "Spazio Google Drive esaurito."
        else -> "Errore Google Drive ($statusCode). Riprova."
    }

    /**
     * Sanitizes a folder name by removing special characters.
     * Keeps alphanumeric characters, spaces, and common punctuation.
     */
    private fun sanitizeFolderName(name: String): String {
        return name
            .replace(Regex("[\\\\/:*?\"<>|]"), "") // Remove illegal file system characters
            .replace(Regex("\\s+"), " ") // Normalize whitespace
            .trim()
            .take(100) // Limit length
            .ifEmpty { "Commessa" }
    }

    /**
     * Generates a file name using the naming convention: {CommessaNome}_{YYYY-MM-DD}.pdf
     */
    private fun generateFileName(commessaNome: String, data: Long): String {
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.ITALY).format(Date(data))
        return "${commessaNome}_${dateStr}.pdf"
    }
}
