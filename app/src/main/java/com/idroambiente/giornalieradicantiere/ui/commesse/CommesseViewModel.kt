package com.idroambiente.giornalieradicantiere.ui.commesse

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idroambiente.giornalieradicantiere.database.entities.Cantiere
import com.idroambiente.giornalieradicantiere.database.entities.Commessa
import com.idroambiente.giornalieradicantiere.database.entities.StatoCommessa
import com.idroambiente.giornalieradicantiere.database.repository.CantiereRepository
import com.idroambiente.giornalieradicantiere.database.repository.CommessaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CommesseViewModel @Inject constructor(
    private val commessaRepository: CommessaRepository,
    private val cantiereRepository: CantiereRepository
) : ViewModel() {

    companion object {
        private const val TAG = "CommesseViewModel"
    }

    val commesse: StateFlow<List<Commessa>> = commessaRepository.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()

    fun showCreateDialog() {
        _showCreateDialog.value = true
    }

    fun hideCreateDialog() {
        _showCreateDialog.value = false
    }

    fun createCommessa(nome: String, cliente: String, indirizzo: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val commessa = Commessa(
                    nome = nome,
                    cliente = cliente,
                    indirizzo = indirizzo,
                    stato = StatoCommessa.APERTA
                )
                val commessaId = commessaRepository.insert(commessa)
                Log.d(TAG, "Commessa creata: $nome (id: $commessaId)")

                val cantiere = Cantiere(
                    commessaId = commessaId,
                    nome = nome,
                    indirizzo = indirizzo
                )
                val cantiereId = cantiereRepository.insert(cantiere)
                Log.d(TAG, "Cantiere creato automaticamente per commessa $commessaId (cantiereId: $cantiereId)")

                hideCreateDialog()
            } catch (e: Exception) {
                Log.e(TAG, "Errore creazione commessa", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun openCommessa(commessaId: Long) {
        // TODO: Navigare a dettaglio commessa / lista cantieri
        Log.d(TAG, "Apri commessa: $commessaId")
    }

    fun generateRiepilogo(commessaId: Long) {
        viewModelScope.launch {
            Log.d(TAG, "Generazione riepilogo per commessa: $commessaId")
            val commessa = commessaRepository.getById(commessaId)
            commessa?.let {
                Log.d(TAG, "Riepilogo commessa: ${it.nome}, Cliente: ${it.cliente}, Stato: ${it.stato}")
                // TODO: Implementare generazione PDF o export dati
            }
        }
    }
}
