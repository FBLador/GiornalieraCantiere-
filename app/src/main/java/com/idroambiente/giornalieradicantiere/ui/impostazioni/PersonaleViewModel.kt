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

    fun showCreateDialog() {
        _showCreateDialog.value = true
    }

    fun hideCreateDialog() {
        _showCreateDialog.value = false
    }

    fun createPersonale(nome: String, cognome: String) {
        viewModelScope.launch {
            val personale = Personale(nome = nome.trim(), cognome = cognome.trim())
            personaleRepository.insert(personale)
            _showCreateDialog.value = false
        }
    }

    fun deletePersonale(personale: Personale) {
        viewModelScope.launch {
            personaleRepository.delete(personale)
        }
    }
}
