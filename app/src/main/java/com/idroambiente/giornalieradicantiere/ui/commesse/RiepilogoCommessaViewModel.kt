package com.idroambiente.giornalieradicantiere.ui.commesse

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idroambiente.giornalieradicantiere.database.entities.Commessa
import com.idroambiente.giornalieradicantiere.database.entities.Giornaliera
import com.idroambiente.giornalieradicantiere.database.repository.CantiereRepository
import com.idroambiente.giornalieradicantiere.database.repository.CommessaRepository
import com.idroambiente.giornalieradicantiere.database.repository.GiornalieraRepository
import com.idroambiente.giornalieradicantiere.database.repository.MezzoGiornalieraRepository
import com.idroambiente.giornalieradicantiere.database.repository.PersonaleGiornalieraRepository
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

data class RiepilogoCommessaData(
    val totaleOreLavoro: Float = 0f,
    val totaleOreViaggio: Float = 0f,
    val totaleKm: Float = 0f
)

@HiltViewModel
class RiepilogoCommessaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val commessaRepository: CommessaRepository,
    private val cantiereRepository: CantiereRepository,
    private val giornalieraRepository: GiornalieraRepository,
    private val personaleGiornalieraRepository: PersonaleGiornalieraRepository,
    private val mezzoGiornalieraRepository: MezzoGiornalieraRepository
) : ViewModel() {

    val commessaId: Long = savedStateHandle.get<Long>("commessaId") ?: 0L

    private val _commessa = MutableStateFlow<Commessa?>(null)
    val commessa: StateFlow<Commessa?> = _commessa.asStateFlow()

    private val _cantiereId = MutableStateFlow<Long?>(null)
    val cantiereId: StateFlow<Long?> = _cantiereId.asStateFlow()

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

    private val _riepilogo = MutableStateFlow(RiepilogoCommessaData())
    val riepilogo: StateFlow<RiepilogoCommessaData> = _riepilogo.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadData()
        viewModelScope.launch {
            giornaliere.collect { list ->
                updateRiepilogo(list)
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _commessa.value = commessaRepository.getById(commessaId)
                val cantieri = cantiereRepository.getByCommessaId(commessaId).first()
                _cantiereId.value = cantieri.firstOrNull()?.id
            } catch (e: Exception) {
                _commessa.value = null
                _cantiereId.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun updateRiepilogo(giornaliereList: List<Giornaliera>) {
        var oreLavoro = 0f
        var oreViaggio = 0f
        var km = 0f
        for (g in giornaliereList) {
            val personale = personaleGiornalieraRepository.getByGiornalieraId(g.id).first()
            oreLavoro += personale.sumOf { it.oreLavoro.toDouble() }.toFloat()
            oreViaggio += personale.sumOf { (it.oreViaggioAndata + it.oreViaggioRitorno).toDouble() }.toFloat()
            val mezzi = mezzoGiornalieraRepository.getByGiornalieraId(g.id).first()
            km += mezzi.sumOf { (it.kmAndata + it.kmRitorno).toDouble() }.toFloat()
        }
        _riepilogo.value = RiepilogoCommessaData(
            totaleOreLavoro = oreLavoro,
            totaleOreViaggio = oreViaggio,
            totaleKm = km
        )
    }
}
