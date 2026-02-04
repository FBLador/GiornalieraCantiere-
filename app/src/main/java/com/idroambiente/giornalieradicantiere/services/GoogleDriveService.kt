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
import java.util.Collections
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
 * Result of a successful file upload
 */
data class UploadResult(
    val fileId: String,
    val fileName: String,
    val folderName: String,
    val webViewLink: String?
)

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
        private const val APP_NAME = "GiornalieraCantiere"
    }

    /**
     * Gets a Drive service instance for the current signed-in user.
     */
    private fun getDriveService(): Drive? {
        val account = googleAuthService.getCurrentAccount() ?: return null
        
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            Collections.singleton(DriveScopes.DRIVE_FILE)
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
     * Uploads a PDF file for a giornaliera, organizing it by commessa.
     * Creates the commessa folder if it doesn't exist.
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

                val sanitizedCommessaNome = sanitizeFolderName(commessaNome)
                val fileName = generateFileName(sanitizedCommessaNome, data)

                // Create file metadata
                val fileMetadata = DriveFile().apply {
                    name = fileName
                    parents = listOf(commessaFolderId)
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
