package com.idroambiente.giornalieradicantiere.ui.impostazioni

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idroambiente.giornalieradicantiere.database.entities.Mezzo
import com.idroambiente.giornalieradicantiere.database.repository.MezzoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MezzoViewModel @Inject constructor(
    private val mezzoRepository: MezzoRepository
) : ViewModel() {

    val mezzoList: StateFlow<List<Mezzo>> = mezzoRepository.getAll()
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

    fun createMezzo(nome: String, targa: String) {
        viewModelScope.launch {
            val mezzo = Mezzo(nome = nome.trim(), targa = targa.trim().uppercase())
            mezzoRepository.insert(mezzo)
            _showCreateDialog.value = false
        }
    }

    fun deleteMezzo(mezzo: Mezzo) {
        viewModelScope.launch {
            mezzoRepository.delete(mezzo)
        }
    }
}
