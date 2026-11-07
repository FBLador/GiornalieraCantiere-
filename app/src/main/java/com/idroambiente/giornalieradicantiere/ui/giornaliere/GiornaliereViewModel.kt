package com.idroambiente.giornalieradicantiere.ui.giornaliere

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idroambiente.giornalieradicantiere.database.dao.GiornalieraSommario
import com.idroambiente.giornalieradicantiere.database.dao.ReportEsportatoDao
import com.idroambiente.giornalieradicantiere.database.entities.Cantiere
import com.idroambiente.giornalieradicantiere.database.entities.Commessa
import com.idroambiente.giornalieradicantiere.database.entities.Giornaliera
import com.idroambiente.giornalieradicantiere.database.repository.CantiereRepository
import com.idroambiente.giornalieradicantiere.database.repository.CommessaRepository
import com.idroambiente.giornalieradicantiere.database.repository.GiornalieraRepository
import com.idroambiente.giornalieradicantiere.services.RiepilogoExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GiornaliereViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val commessaRepository: CommessaRepository,
    private val cantiereRepository: CantiereRepository,
    private val giornalieraRepository: GiornalieraRepository,
    private val reportDao: ReportEsportatoDao,
    private val riepilogoExporter: RiepilogoExporter
) : ViewModel() {

    companion object {
        private const val TAG = "GiornaliereViewModel"
    }

    val commessaId: Long = savedStateHandle.get<Long>("commessaId") ?: 0L

    private val _commessa = MutableStateFlow<Commessa?>(null)
    val commessa: StateFlow<Commessa?> = _commessa.asStateFlow()

    private val _cantiereId = MutableStateFlow<Long?>(null)
    val cantiereId: StateFlow<Long?> = _cantiereId.asStateFlow()

    val giornaliere: StateFlow<List<Giornaliera>> = _cantiereId
        .filterNotNull()
        .flatMapLatest { giornalieraRepository.getByCantiereId(it) }
        .map { list -> list.sortedByDescending { it.data } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Persone e ore di lavoro per ogni giornaliera. */
    val sommari: StateFlow<Map<Long, GiornalieraSommario>> = _cantiereId
        .filterNotNull()
        .flatMapLatest { giornalieraRepository.getSommariByCantiere(it) }
        .map { list -> list.associateBy { it.giornalieraId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /** Report finali rimasti indietro dopo una modifica (aggiornamento non riuscito). */
    val reportDaAggiornare: StateFlow<Int> = reportDao.countDaAggiornare(commessaId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _aggiornandoReport = MutableStateFlow(false)
    val aggiornandoReport: StateFlow<Boolean> = _aggiornandoReport.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                val commessa = commessaRepository.getById(commessaId)
                _commessa.value = commessa
                val cantieri = cantiereRepository.getByCommessaId(commessaId).first()
                if (cantieri.isNotEmpty()) {
                    _cantiereId.value = cantieri.first().id
                } else if (commessa != null) {
                    _cantiereId.value = cantiereRepository.insert(
                        Cantiere(commessaId = commessaId, nome = commessa.nome.toString().padStart(7, '0'), indirizzo = commessa.indirizzo)
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Errore caricamento dati", e)
            }
        }
    }

    fun riprovaReport() {
        if (_aggiornandoReport.value) return
        viewModelScope.launch {
            _aggiornandoReport.value = true
            val esito = riepilogoExporter.riprovaDaAggiornare(commessaId)
            _aggiornandoReport.value = false
            _toast.value = when {
                esito.falliti.isEmpty() -> "Report aggiornati: ${esito.aggiornati.size}"
                else -> "Non riuscito: controlla la connessione e riprova"
            }
        }
    }

    fun toastShown() {
        _toast.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
