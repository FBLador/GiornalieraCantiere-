package com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idroambiente.giornalieradicantiere.database.entities.Giornaliera
import com.idroambiente.giornalieradicantiere.database.entities.Personale
import com.idroambiente.giornalieradicantiere.database.entities.PersonaleGiornaliera
import com.idroambiente.giornalieradicantiere.database.entities.StatoGiornaliera
import com.idroambiente.giornalieradicantiere.database.repository.GiornalieraRepository
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
    val presente: Boolean = true,
    val oreViaggioAndata: Float = 0f,
    val oreViaggioRitorno: Float = 0f,
    val oreLavoro: Float = 0f,
    val inizioTurno: Long = 0L,
    val fineTurno: Long = 0L,
    val pernotto: Boolean = false
)

data class WizardUiState(
    val currentStep: Int = 0,
    val personaleSelections: Map<Long, PersonaleFormData> = emptyMap(),
    val isSaving: Boolean = false,
    val saveCompleted: Boolean = false
)

@HiltViewModel
class GiornalieraWizardViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val personaleRepository: PersonaleRepository,
    private val giornalieraRepository: GiornalieraRepository,
    private val personaleGiornalieraRepository: PersonaleGiornalieraRepository
) : ViewModel() {

    val cantiereId: Long = savedStateHandle.get<Long>("cantiereId") ?: 0L

    val personaleList: StateFlow<List<Personale>> = personaleRepository.getAll()
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
    }

    fun updatePersonaleData(personaleId: Long, data: PersonaleFormData) {
        _uiState.update { state ->
            state.copy(
                personaleSelections = state.personaleSelections + (personaleId to data)
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

                val personaleGiornaliere = _uiState.value.personaleSelections.map { (personaleId, data) ->
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

                _uiState.update { it.copy(isSaving = false, saveCompleted = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}
