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

    private val _mezzoToEdit = MutableStateFlow<Mezzo?>(null)
    val mezzoToEdit: StateFlow<Mezzo?> = _mezzoToEdit.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun showCreateDialog() {
        _showCreateDialog.value = true
    }

    fun hideCreateDialog() {
        _showCreateDialog.value = false
    }

    fun showEditDialog(mezzo: Mezzo) {
        _mezzoToEdit.value = mezzo
    }

    fun hideEditDialog() {
        _mezzoToEdit.value = null
    }

    fun createMezzo(nome: String, targa: String, codiceArticoloKm: String, codiceArticoloGiorni: String) {
        viewModelScope.launch {
            _errorMessage.value = null
            val trimmedTarga = targa.trim().uppercase()

            // Check for duplicate targa
            val existingMezzo = mezzoRepository.getByTarga(trimmedTarga)
            if (existingMezzo != null) {
                _errorMessage.value = "Esiste già un mezzo con questa targa"
                return@launch
            }

            val mezzo = Mezzo(
                nome = nome.trim(),
                targa = trimmedTarga,
                codiceArticoloKm = codiceArticoloKm.uppercase().trim(),
                codiceArticoloGiorni = codiceArticoloGiorni.uppercase().trim()
            )
            mezzoRepository.insert(mezzo)
            _showCreateDialog.value = false
        }
    }

    fun updateMezzo(nome: String, targa: String, codiceArticoloKm: String, codiceArticoloGiorni: String) {
        viewModelScope.launch {
            _errorMessage.value = null
            val current = _mezzoToEdit.value ?: return@launch
            val trimmedTarga = targa.trim().uppercase()

            // Check for duplicate targa (excluding current mezzo)
            val existingMezzo = mezzoRepository.getByTarga(trimmedTarga)
            if (existingMezzo != null && existingMezzo.id != current.id) {
                _errorMessage.value = "Esiste già un mezzo con questa targa"
                return@launch
            }

            mezzoRepository.update(
                current.copy(
                    nome = nome.trim(),
                    targa = trimmedTarga,
                    codiceArticoloKm = codiceArticoloKm.uppercase().trim(),
                    codiceArticoloGiorni = codiceArticoloGiorni.uppercase().trim()
                )
            )
            _mezzoToEdit.value = null
        }
    }

    fun deleteMezzo(mezzo: Mezzo) {
        viewModelScope.launch {
            mezzoRepository.delete(mezzo)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
