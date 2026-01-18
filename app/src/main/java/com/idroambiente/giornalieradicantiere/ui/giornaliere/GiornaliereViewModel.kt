package com.idroambiente.giornalieradicantiere.ui.giornaliere

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idroambiente.giornalieradicantiere.database.entities.Cantiere
import com.idroambiente.giornalieradicantiere.database.entities.Commessa
import com.idroambiente.giornalieradicantiere.database.entities.Giornaliera
import com.idroambiente.giornalieradicantiere.database.entities.StatoGiornaliera
import com.idroambiente.giornalieradicantiere.database.repository.CantiereRepository
import com.idroambiente.giornalieradicantiere.database.repository.CommessaRepository
import com.idroambiente.giornalieradicantiere.database.repository.GiornalieraRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GiornaliereViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val commessaRepository: CommessaRepository,
    private val cantiereRepository: CantiereRepository,
    private val giornalieraRepository: GiornalieraRepository
) : ViewModel() {

    companion object {
        private const val TAG = "GiornaliereViewModel"
    }

    val commessaId: Long = savedStateHandle.get<Long>("commessaId") ?: 0L

    private val _commessa = MutableStateFlow<Commessa?>(null)
    val commessa: StateFlow<Commessa?> = _commessa.asStateFlow()

    private val _cantiereId = MutableStateFlow<Long?>(null)

    val giornaliere: StateFlow<List<Giornaliera>> = _cantiereId
        .filterNotNull()
        .flatMapLatest { cantiereId ->
            giornalieraRepository.getByCantiereId(cantiereId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val commessa = commessaRepository.getById(commessaId)
                _commessa.value = commessa
                Log.d(TAG, "Commessa caricata: ${commessa?.nome}")

                val cantieri = cantiereRepository.getByCommessaId(commessaId).first()
                if (cantieri.isNotEmpty()) {
                    _cantiereId.value = cantieri.first().id
                    Log.d(TAG, "Cantiere trovato: ${_cantiereId.value}")
                } else if (commessa != null) {
                    val cantiere = Cantiere(
                        commessaId = commessaId,
                        nome = commessa.nome,
                        indirizzo = commessa.indirizzo
                    )
                    val newCantiereId = cantiereRepository.insert(cantiere)
                    _cantiereId.value = newCantiereId
                    Log.d(TAG, "Cantiere creato automaticamente: $newCantiereId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Errore caricamento dati", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createGiornaliera() {
        viewModelScope.launch {
            val cantiereId = _cantiereId.value
            if (cantiereId == null) {
                Log.e(TAG, "Impossibile creare giornaliera: cantiereId non disponibile")
                return@launch
            }

            Log.d(TAG, "Creazione nuova giornaliera per cantiere: $cantiereId")
            val giornaliera = Giornaliera(
                cantiereId = cantiereId,
                data = System.currentTimeMillis(),
                stato = StatoGiornaliera.BOZZA
            )
            val id = giornalieraRepository.insert(giornaliera)
            Log.d(TAG, "Giornaliera creata con id: $id")
        }
    }

    fun openGiornaliera(giornalieraId: Long) {
        Log.d(TAG, "Apertura giornaliera: $giornalieraId")
    }

    fun generateRiepilogoCommessa() {
        viewModelScope.launch {
            Log.d(TAG, "Generazione riepilogo per commessa: $commessaId")
            _commessa.value?.let { commessa ->
                Log.d(TAG, "Riepilogo commessa: ${commessa.nome}, Cliente: ${commessa.cliente}")
                val giornaliereList = giornaliere.value
                Log.d(TAG, "Numero giornaliere: ${giornaliereList.size}")
                giornaliereList.forEach { g ->
                    Log.d(TAG, "  - Giornaliera ${g.id}: data=${g.data}, stato=${g.stato}")
                }
            }
        }
    }
}
