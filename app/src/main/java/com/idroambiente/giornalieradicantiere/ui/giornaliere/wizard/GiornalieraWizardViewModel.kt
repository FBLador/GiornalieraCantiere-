package com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idroambiente.giornalieradicantiere.database.entities.Attrezzatura
import com.idroambiente.giornalieradicantiere.database.entities.AttrezzaturaGiornaliera
import com.idroambiente.giornalieradicantiere.database.entities.Giornaliera
import com.idroambiente.giornalieradicantiere.database.entities.Mezzo
import com.idroambiente.giornalieradicantiere.database.entities.MezzoGiornaliera
import com.idroambiente.giornalieradicantiere.database.entities.Personale
import com.idroambiente.giornalieradicantiere.database.entities.PersonaleGiornaliera
import com.idroambiente.giornalieradicantiere.database.entities.StatoGiornaliera
import com.idroambiente.giornalieradicantiere.database.repository.AttrezzaturaGiornalieraRepository
import com.idroambiente.giornalieradicantiere.database.repository.AttrezzaturaRepository
import com.idroambiente.giornalieradicantiere.database.repository.GiornalieraRepository
import com.idroambiente.giornalieradicantiere.database.repository.MezzoGiornalieraRepository
import com.idroambiente.giornalieradicantiere.database.repository.MezzoRepository
import com.idroambiente.giornalieradicantiere.database.repository.PersonaleGiornalieraRepository
import com.idroambiente.giornalieradicantiere.database.repository.PersonaleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PersonaleFormData(
    val presente: Boolean = false,
    val oreViaggioAndata: Float = 0f,
    val oreViaggioRitorno: Float = 0f,
    val oreLavoro: Float = 0f,
    val inizioTurno: Long = 0L,
    val fineTurno: Long = 0L,
    val pernotto: Boolean = false
)

data class MezzoFormData(
    val utilizzato: Boolean = false,
    val kmAndata: Float = 0f,
    val kmRitorno: Float = 0f
)

data class AttrezzaturaFormData(
    val utilizzata: Boolean = false
)

data class WizardUiState(
    val currentStep: Int = 0,
    val personaleSelections: Map<Long, PersonaleFormData> = emptyMap(),
    val mezzoSelections: Map<Long, MezzoFormData> = emptyMap(),
    val attrezzaturaSelections: Map<Long, AttrezzaturaFormData> = emptyMap(),
    val isSaving: Boolean = false,
    val saveCompleted: Boolean = false
)

@HiltViewModel
class GiornalieraWizardViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val personaleRepository: PersonaleRepository,
    private val giornalieraRepository: GiornalieraRepository,
    private val personaleGiornalieraRepository: PersonaleGiornalieraRepository,
    private val mezzoRepository: MezzoRepository,
    private val mezzoGiornalieraRepository: MezzoGiornalieraRepository,
    private val attrezzaturaRepository: AttrezzaturaRepository,
    private val attrezzaturaGiornalieraRepository: AttrezzaturaGiornalieraRepository
) : ViewModel() {

    val cantiereId: Long = savedStateHandle.get<Long>("cantiereId") ?: 0L

    val personaleList: StateFlow<List<Personale>> = personaleRepository.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val mezzoList: StateFlow<List<Mezzo>> = mezzoRepository.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val attrezzaturaList: StateFlow<List<Attrezzatura>> = attrezzaturaRepository.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(WizardUiState())
    val uiState: StateFlow<WizardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            personaleList.collect { list ->
                if (list.isNotEmpty() && _uiState.value.personaleSelections.isEmpty()) {
                    val initialSelections = list.associate { personale ->
                        personale.id to PersonaleFormData()
                    }
                    _uiState.update { it.copy(personaleSelections = initialSelections) }
                }
            }
        }
        viewModelScope.launch {
            mezzoList.collect { list ->
                if (list.isNotEmpty() && _uiState.value.mezzoSelections.isEmpty()) {
                    val initialSelections = list.associate { mezzo ->
                        mezzo.id to MezzoFormData()
                    }
                    _uiState.update { it.copy(mezzoSelections = initialSelections) }
                }
            }
        }
        viewModelScope.launch {
            attrezzaturaList.collect { list ->
                if (list.isNotEmpty() && _uiState.value.attrezzaturaSelections.isEmpty()) {
                    val initialSelections = list.associate { attrezzatura ->
                        attrezzatura.id to AttrezzaturaFormData()
                    }
                    _uiState.update { it.copy(attrezzaturaSelections = initialSelections) }
                }
            }
        }
    }

    fun updatePersonaleData(personaleId: Long, data: PersonaleFormData) {
        _uiState.update { state ->
            state.copy(
                personaleSelections = state.personaleSelections + (personaleId to data)
            )
        }
    }

    fun updateMezzoData(mezzoId: Long, data: MezzoFormData) {
        _uiState.update { state ->
            state.copy(
                mezzoSelections = state.mezzoSelections + (mezzoId to data)
            )
        }
    }

    fun updateAttrezzaturaData(attrezzaturaId: Long, data: AttrezzaturaFormData) {
        _uiState.update { state ->
            state.copy(
                attrezzaturaSelections = state.attrezzaturaSelections + (attrezzaturaId to data)
            )
        }
    }

    fun nextStep() {
        _uiState.update { state ->
            if (state.currentStep < 3) {
                state.copy(currentStep = state.currentStep + 1)
            } else {
                state
            }
        }
    }

    fun previousStep() {
        _uiState.update { state ->
            if (state.currentStep > 0) {
                state.copy(currentStep = state.currentStep - 1)
            } else {
                state
            }
        }
    }

    fun saveGiornaliera() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            try {
                val giornaliera = Giornaliera(
                    cantiereId = cantiereId,
                    data = System.currentTimeMillis(),
                    stato = StatoGiornaliera.BOZZA
                )
                val giornalieraId = giornalieraRepository.insert(giornaliera)

                // Salva solo il personale presente
                val personaleGiornaliere = _uiState.value.personaleSelections
                    .filter { it.value.presente }
                    .map { (personaleId, data) ->
                        PersonaleGiornaliera(
                            giornalieraId = giornalieraId,
                            personaleId = personaleId,
                            presente = data.presente,
                            oreViaggioAndata = data.oreViaggioAndata,
                            oreViaggioRitorno = data.oreViaggioRitorno,
                            oreLavoro = data.oreLavoro,
                            inizioTurno = data.inizioTurno,
                            fineTurno = data.fineTurno,
                            pernotto = data.pernotto
                        )
                    }
                personaleGiornalieraRepository.insertAll(personaleGiornaliere)

                // Salva solo i mezzi utilizzati
                val mezzoGiornaliere = _uiState.value.mezzoSelections
                    .filter { it.value.utilizzato }
                    .map { (mezzoId, data) ->
                        MezzoGiornaliera(
                            giornalieraId = giornalieraId,
                            mezzoId = mezzoId,
                            kmAndata = data.kmAndata,
                            kmRitorno = data.kmRitorno
                        )
                    }
                mezzoGiornalieraRepository.insertAll(mezzoGiornaliere)

                // Salva solo le attrezzature utilizzate
                val attrezzaturaGiornaliere = _uiState.value.attrezzaturaSelections
                    .filter { it.value.utilizzata }
                    .map { (attrezzaturaId, _) ->
                        AttrezzaturaGiornaliera(
                            giornalieraId = giornalieraId,
                            attrezzaturaId = attrezzaturaId
                        )
                    }
                attrezzaturaGiornalieraRepository.insertAll(attrezzaturaGiornaliere)

                _uiState.update { it.copy(isSaving = false, saveCompleted = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}
