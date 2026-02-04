package com.idroambiente.giornalieradicantiere.ui.impostazioni

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.idroambiente.giornalieradicantiere.data.preferences.PreferencesRepository
import com.idroambiente.giornalieradicantiere.services.DriveFolder
import com.idroambiente.giornalieradicantiere.services.GoogleAuthService
import com.idroambiente.giornalieradicantiere.services.GoogleDriveService
import com.idroambiente.giornalieradicantiere.services.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GoogleDriveSettingsState(
    val isSignedIn: Boolean = false,
    val userEmail: String? = null,
    val rootFolderId: String? = null,
    val rootFolderName: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isOffline: Boolean = false,
    val showFolderPicker: Boolean = false,
    val folderPickerState: FolderPickerState = FolderPickerState()
)

data class FolderPickerState(
    val currentFolderId: String = "root",
    val currentFolderName: String = "Il mio Drive",
    val folders: List<DriveFolder> = emptyList(),
    val isLoading: Boolean = false,
    val breadcrumbs: List<Pair<String, String>> = listOf("root" to "Il mio Drive"),
    val showCreateFolderDialog: Boolean = false
)

@HiltViewModel
class GoogleDriveSettingsViewModel @Inject constructor(
    private val googleAuthService: GoogleAuthService,
    private val googleDriveService: GoogleDriveService,
    private val preferencesRepository: PreferencesRepository,
    private val networkUtils: NetworkUtils
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoogleDriveSettingsState())
    val uiState: StateFlow<GoogleDriveSettingsState> = _uiState.asStateFlow()

    init {
        checkNetwork()
        // Combine auth state with preferences
        viewModelScope.launch {
            combine(
                googleAuthService.isSignedIn,
                googleAuthService.currentAccount,
                preferencesRepository.googleDriveRootFolderId,
                preferencesRepository.googleDriveRootFolderName,
                preferencesRepository.googleUserEmail
            ) { isSignedIn, account, folderId, folderName, email ->
                _uiState.value.copy(
                    isSignedIn = isSignedIn,
                    userEmail = email ?: account?.email,
                    rootFolderId = folderId,
                    rootFolderName = folderName
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    /**
     * Gets the Intent to launch Google Sign-In
     */
    fun getSignInIntent(): Intent {
        return googleAuthService.getSignInIntent()
    }

    /**
     * Handles the result from Google Sign-In activity
     */
    fun handleSignInResult(data: Intent?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val result = googleAuthService.handleSignInResult(data)
            
            result.fold(
                onSuccess = { account ->
                    // Save email to preferences
                    account.email?.let { email ->
                        preferencesRepository.setGoogleUserEmail(email)
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSignedIn = true,
                        userEmail = account.email
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Errore durante l'accesso"
                    )
                }
            )
        }
    }

    /**
     * Signs out the current user
     */
    fun signOut() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            googleAuthService.signOut()
            preferencesRepository.clearGoogleDrive()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isSignedIn = false,
                userEmail = null,
                rootFolderId = null,
                rootFolderName = null
            )
        }
    }

    /**
     * Opens the folder picker dialog
     */
    fun openFolderPicker() {
        _uiState.value = _uiState.value.copy(showFolderPicker = true)
        loadFolders("root")
    }

    /**
     * Closes the folder picker dialog
     */
    fun closeFolderPicker() {
        _uiState.value = _uiState.value.copy(
            showFolderPicker = false,
            folderPickerState = FolderPickerState()
        )
    }

    /**
     * Loads folders for the current directory
     */
    fun loadFolders(folderId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                folderPickerState = _uiState.value.folderPickerState.copy(isLoading = true)
            )

            val result = googleDriveService.listFolders(folderId)
            
            result.fold(
                onSuccess = { folders ->
                    _uiState.value = _uiState.value.copy(
                        folderPickerState = _uiState.value.folderPickerState.copy(
                            folders = folders,
                            isLoading = false,
                            currentFolderId = folderId
                        )
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message,
                        folderPickerState = _uiState.value.folderPickerState.copy(isLoading = false)
                    )
                }
            )
        }
    }

    /**
     * Navigates into a folder
     */
    fun navigateToFolder(folder: DriveFolder) {
        val currentBreadcrumbs = _uiState.value.folderPickerState.breadcrumbs.toMutableList()
        currentBreadcrumbs.add(folder.id to folder.name)
        
        _uiState.value = _uiState.value.copy(
            folderPickerState = _uiState.value.folderPickerState.copy(
                currentFolderId = folder.id,
                currentFolderName = folder.name,
                breadcrumbs = currentBreadcrumbs
            )
        )
        
        loadFolders(folder.id)
    }

    /**
     * Navigates back to a breadcrumb folder
     */
    fun navigateToBreadcrumb(index: Int) {
        val breadcrumbs = _uiState.value.folderPickerState.breadcrumbs.take(index + 1)
        val (folderId, folderName) = breadcrumbs.last()
        
        _uiState.value = _uiState.value.copy(
            folderPickerState = _uiState.value.folderPickerState.copy(
                currentFolderId = folderId,
                currentFolderName = folderName,
                breadcrumbs = breadcrumbs
            )
        )
        
        loadFolders(folderId)
    }

    /**
     * Shows the create folder dialog
     */
    fun showCreateFolderDialog() {
        _uiState.value = _uiState.value.copy(
            folderPickerState = _uiState.value.folderPickerState.copy(showCreateFolderDialog = true)
        )
    }

    /**
     * Hides the create folder dialog
     */
    fun hideCreateFolderDialog() {
        _uiState.value = _uiState.value.copy(
            folderPickerState = _uiState.value.folderPickerState.copy(showCreateFolderDialog = false)
        )
    }

    /**
     * Creates a new folder in the current directory
     */
    fun createFolder(name: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                folderPickerState = _uiState.value.folderPickerState.copy(
                    isLoading = true,
                    showCreateFolderDialog = false
                )
            )

            val currentFolderId = _uiState.value.folderPickerState.currentFolderId
            val result = googleDriveService.createFolder(name, currentFolderId)
            
            result.fold(
                onSuccess = {
                    // Reload folders
                    loadFolders(currentFolderId)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message,
                        folderPickerState = _uiState.value.folderPickerState.copy(isLoading = false)
                    )
                }
            )
        }
    }

    /**
     * Selects the current folder as the root folder for saving PDFs
     */
    fun selectCurrentFolder() {
        viewModelScope.launch {
            val folderId = _uiState.value.folderPickerState.currentFolderId
            val folderName = _uiState.value.folderPickerState.currentFolderName
            
            preferencesRepository.setGoogleDriveRootFolder(folderId, folderName)
            
            _uiState.value = _uiState.value.copy(
                rootFolderId = folderId,
                rootFolderName = folderName,
                showFolderPicker = false,
                folderPickerState = FolderPickerState()
            )
        }
    }

    /**
     * Checks network connectivity and updates state
     */
    fun checkNetwork() {
        _uiState.value = _uiState.value.copy(isOffline = !networkUtils.isOnline())
    }

    /**
     * Clears any error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
