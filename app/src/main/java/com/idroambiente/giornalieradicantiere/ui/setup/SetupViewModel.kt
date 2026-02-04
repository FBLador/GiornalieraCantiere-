package com.idroambiente.giornalieradicantiere.ui.setup

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idroambiente.giornalieradicantiere.data.preferences.PreferencesRepository
import com.idroambiente.giornalieradicantiere.data.preferences.StorageMode
import com.idroambiente.giornalieradicantiere.services.DriveFolder
import com.idroambiente.giornalieradicantiere.services.GoogleAuthService
import com.idroambiente.giornalieradicantiere.services.GoogleDriveService
import com.idroambiente.giornalieradicantiere.services.NetworkUtils
import com.idroambiente.giornalieradicantiere.ui.impostazioni.FolderPickerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SetupStep { WELCOME, FOLDER_SELECTION, COMPLETE }

data class SetupUiState(
    val step: SetupStep = SetupStep.WELCOME,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isOffline: Boolean = false,
    val userName: String? = null,
    val userEmail: String? = null,
    val selectedFolderName: String? = null,
    val folderPickerState: FolderPickerState = FolderPickerState()
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val googleAuthService: GoogleAuthService,
    private val googleDriveService: GoogleDriveService,
    private val preferencesRepository: PreferencesRepository,
    private val networkUtils: NetworkUtils
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    init {
        checkNetwork()
    }

    fun getSignInIntent(): Intent {
        return googleAuthService.getSignInIntent()
    }

    fun handleSignInResult(data: Intent?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = googleAuthService.handleSignInResult(data)

            result.fold(
                onSuccess = { account ->
                    account.email?.let { preferencesRepository.setGoogleUserEmail(it) }
                    account.displayName?.let { preferencesRepository.setGoogleUserDisplayName(it) }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        userName = account.displayName,
                        userEmail = account.email,
                        step = SetupStep.FOLDER_SELECTION
                    )
                    // Start loading root folders for folder picker
                    loadFolders("root")
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

    fun loadFolders(folderId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                folderPickerState = _uiState.value.folderPickerState.copy(isLoading = true)
            )

            if (folderId == "root") {
                coroutineScope {
                    val myDriveDeferred = async { googleDriveService.listFolders("root") }
                    val sharedDeferred = async { googleDriveService.listSharedFolders() }
                    val myDriveResult = myDriveDeferred.await()
                    val sharedResult = sharedDeferred.await()

                    myDriveResult.fold(
                        onSuccess = { folders ->
                            val sharedFolders = sharedResult.getOrElse { emptyList() }
                            _uiState.value = _uiState.value.copy(
                                folderPickerState = _uiState.value.folderPickerState.copy(
                                    folders = folders,
                                    sharedFolders = sharedFolders,
                                    isLoading = false,
                                    currentFolderId = folderId
                                )
                            )
                        },
                        onFailure = { error ->
                            _uiState.value = _uiState.value.copy(
                                error = error.message,
                                folderPickerState = _uiState.value.folderPickerState.copy(
                                    isLoading = false,
                                    sharedFolders = emptyList()
                                )
                            )
                        }
                    )
                    sharedResult.fold(
                        onFailure = { error ->
                            if (myDriveResult.isSuccess) {
                                _uiState.value = _uiState.value.copy(
                                    folderPickerState = _uiState.value.folderPickerState.copy(
                                        sharedFolders = emptyList()
                                    )
                                )
                            }
                        },
                        onSuccess = { }
                    )
                }
            } else {
                val result = googleDriveService.listFolders(folderId)
                result.fold(
                    onSuccess = { folders ->
                        _uiState.value = _uiState.value.copy(
                            folderPickerState = _uiState.value.folderPickerState.copy(
                                folders = folders,
                                sharedFolders = emptyList(),
                                isLoading = false,
                                currentFolderId = folderId
                            )
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            error = error.message,
                            folderPickerState = _uiState.value.folderPickerState.copy(
                                isLoading = false,
                                sharedFolders = emptyList()
                            )
                        )
                    }
                )
            }
        }
    }

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

    fun showCreateFolderDialog() {
        _uiState.value = _uiState.value.copy(
            folderPickerState = _uiState.value.folderPickerState.copy(showCreateFolderDialog = true)
        )
    }

    fun hideCreateFolderDialog() {
        _uiState.value = _uiState.value.copy(
            folderPickerState = _uiState.value.folderPickerState.copy(showCreateFolderDialog = false)
        )
    }

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
                onSuccess = { loadFolders(currentFolderId) },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message,
                        folderPickerState = _uiState.value.folderPickerState.copy(isLoading = false)
                    )
                }
            )
        }
    }

    fun selectCurrentFolder() {
        viewModelScope.launch {
            val folderId = _uiState.value.folderPickerState.currentFolderId
            val folderName = _uiState.value.folderPickerState.currentFolderName

            preferencesRepository.setGoogleDriveRootFolder(folderId, folderName)

            _uiState.value = _uiState.value.copy(
                selectedFolderName = folderName,
                step = SetupStep.COMPLETE
            )
        }
    }

    fun completeSetup() {
        viewModelScope.launch {
            preferencesRepository.setStorageMode(StorageMode.GOOGLE_DRIVE)
            preferencesRepository.setSetupCompleted(true)
        }
    }

    fun checkNetwork() {
        _uiState.value = _uiState.value.copy(isOffline = !networkUtils.isOnline())
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
