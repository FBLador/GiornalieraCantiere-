package com.idroambiente.giornalieradicantiere.ui.impostazioni

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idroambiente.giornalieradicantiere.database.entities.Personale
import com.idroambiente.giornalieradicantiere.database.repository.PersonaleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DuplicateDialogState(
    val nome: String,
    val cognome: String,
    val existingPersonale: Personale
)

@HiltViewModel
class PersonaleViewModel @Inject constructor(
    private val personaleRepository: PersonaleRepository
) : ViewModel() {

    val personaleList: StateFlow<List<Personale>> = personaleRepository.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()

    private val _personaleToEdit = MutableStateFlow<Personale?>(null)
    val personaleToEdit: StateFlow<Personale?> = _personaleToEdit.asStateFlow()

    private val _duplicateDialogState = MutableStateFlow<DuplicateDialogState?>(null)
    val duplicateDialogState: StateFlow<DuplicateDialogState?> = _duplicateDialogState.asStateFlow()

    fun showCreateDialog() {
        _showCreateDialog.value = true
    }

    fun hideCreateDialog() {
        _showCreateDialog.value = false
    }

    fun showEditDialog(personale: Personale) {
        _personaleToEdit.value = personale
    }

    fun hideEditDialog() {
        _personaleToEdit.value = null
    }

    fun createPersonale(nome: String, cognome: String) {
        viewModelScope.launch {
            val trimmedNome = nome.trim()
            val trimmedCognome = cognome.trim()
            
            // Check for duplicate nome+cognome (case-insensitive)
            val existingPersonale = personaleRepository.getByNomeECognome(trimmedNome, trimmedCognome)
            if (existingPersonale != null) {
                // Set duplicate dialog state - don't close create dialog
                _duplicateDialogState.value = DuplicateDialogState(
                    nome = trimmedNome,
                    cognome = trimmedCognome,
                    existingPersonale = existingPersonale
                )
                return@launch
            }

            val personale = Personale(nome = trimmedNome, cognome = trimmedCognome)
            personaleRepository.insert(personale)
            _showCreateDialog.value = false
        }
    }

    fun updatePersonale(nome: String, cognome: String) {
        viewModelScope.launch {
            val current = _personaleToEdit.value ?: return@launch
            val trimmedNome = nome.trim()
            val trimmedCognome = cognome.trim()
            
            // Check for duplicate nome+cognome (case-insensitive, excluding current)
            val existingPersonale = personaleRepository.getByNomeECognome(trimmedNome, trimmedCognome)
            if (existingPersonale != null && existingPersonale.id != current.id) {
                // Set duplicate dialog state - don't close edit dialog
                _duplicateDialogState.value = DuplicateDialogState(
                    nome = trimmedNome,
                    cognome = trimmedCognome,
                    existingPersonale = existingPersonale
                )
                return@launch
            }

            personaleRepository.update(
                current.copy(nome = trimmedNome, cognome = trimmedCognome)
            )
            _personaleToEdit.value = null
        }
    }

    fun deletePersonale(personale: Personale) {
        viewModelScope.launch {
            personaleRepository.delete(personale)
        }
    }

    fun dismissDuplicateDialog() {
        _duplicateDialogState.value = null
        // Close create/edit dialogs when user cancels
        _showCreateDialog.value = false
        _personaleToEdit.value = null
    }

    fun confirmDuplicateAndModify() {
        // Just close the duplicate dialog, keep create/edit dialog open
        _duplicateDialogState.value = null
    }
}
