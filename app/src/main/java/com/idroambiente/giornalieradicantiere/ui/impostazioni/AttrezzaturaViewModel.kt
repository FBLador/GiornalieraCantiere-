package com.idroambiente.giornalieradicantiere.ui.impostazioni

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idroambiente.giornalieradicantiere.database.entities.Attrezzatura
import com.idroambiente.giornalieradicantiere.database.repository.AttrezzaturaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AttrezzaturaViewModel @Inject constructor(
    private val attrezzaturaRepository: AttrezzaturaRepository
) : ViewModel() {

    val attrezzaturaList: StateFlow<List<Attrezzatura>> = attrezzaturaRepository.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()

    private val _attrezzaturaToEdit = MutableStateFlow<Attrezzatura?>(null)
    val attrezzaturaToEdit: StateFlow<Attrezzatura?> = _attrezzaturaToEdit.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun showCreateDialog() {
        _showCreateDialog.value = true
    }

    fun hideCreateDialog() {
        _showCreateDialog.value = false
    }

    fun showEditDialog(attrezzatura: Attrezzatura) {
        _attrezzaturaToEdit.value = attrezzatura
    }

    fun hideEditDialog() {
        _attrezzaturaToEdit.value = null
    }

    fun createAttrezzatura(nome: String, codiceArticolo: String) {
        viewModelScope.launch {
            _errorMessage.value = null
            val trimmedNome = nome.trim()

            // Check for duplicate nome (case-insensitive)
            val existingAttrezzatura = attrezzaturaRepository.getByNome(trimmedNome)
            if (existingAttrezzatura != null) {
                _errorMessage.value = "Esiste già un'attrezzatura con questo nome"
                return@launch
            }

            val attrezzatura = Attrezzatura(
                nome = trimmedNome,
                codiceArticolo = codiceArticolo.uppercase().trim()
            )
            attrezzaturaRepository.insert(attrezzatura)
            _showCreateDialog.value = false
        }
    }

    fun updateAttrezzatura(nome: String, codiceArticolo: String) {
        viewModelScope.launch {
            _errorMessage.value = null
            val current = _attrezzaturaToEdit.value ?: return@launch
            val trimmedNome = nome.trim()

            // Check for duplicate nome (case-insensitive, excluding current)
            val existingAttrezzatura = attrezzaturaRepository.getByNome(trimmedNome)
            if (existingAttrezzatura != null && existingAttrezzatura.id != current.id) {
                _errorMessage.value = "Esiste già un'attrezzatura con questo nome"
                return@launch
            }

            attrezzaturaRepository.update(
                current.copy(
                    nome = trimmedNome,
                    codiceArticolo = codiceArticolo.uppercase().trim()
                )
            )
            _attrezzaturaToEdit.value = null
        }
    }

    fun deleteAttrezzatura(attrezzatura: Attrezzatura) {
        viewModelScope.launch {
            attrezzaturaRepository.delete(attrezzatura)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
