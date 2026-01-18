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

    fun showCreateDialog() {
        _showCreateDialog.value = true
    }

    fun hideCreateDialog() {
        _showCreateDialog.value = false
    }

    fun createAttrezzatura(nome: String) {
        viewModelScope.launch {
            val attrezzatura = Attrezzatura(nome = nome.trim())
            attrezzaturaRepository.insert(attrezzatura)
            _showCreateDialog.value = false
        }
    }

    fun deleteAttrezzatura(attrezzatura: Attrezzatura) {
        viewModelScope.launch {
            attrezzaturaRepository.delete(attrezzatura)
        }
    }
}
