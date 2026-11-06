package com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idroambiente.giornalieradicantiere.data.preferences.ExportBehavior
import com.idroambiente.giornalieradicantiere.data.preferences.PreferencesRepository
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
import com.idroambiente.giornalieradicantiere.database.repository.CantiereRepository
import com.idroambiente.giornalieradicantiere.database.repository.CommessaRepository
import com.idroambiente.giornalieradicantiere.database.repository.GiornalieraRepository
import com.idroambiente.giornalieradicantiere.database.repository.MezzoGiornalieraRepository
import com.idroambiente.giornalieradicantiere.database.repository.MezzoRepository
import com.idroambiente.giornalieradicantiere.database.repository.PersonaleGiornalieraRepository
import com.idroambiente.giornalieradicantiere.database.repository.PersonaleRepository
import com.idroambiente.giornalieradicantiere.data.preferences.StorageMode
import com.idroambiente.giornalieradicantiere.database.GiornalieraStore
import com.idroambiente.giornalieradicantiere.services.CsvGeneratorService
import com.idroambiente.giornalieradicantiere.services.FileStorageService
import com.idroambiente.giornalieradicantiere.services.GiornalieraPdfData
import com.idroambiente.giornalieradicantiere.services.MezzoWithData
import com.idroambiente.giornalieradicantiere.services.NetworkUtils
import com.idroambiente.giornalieradicantiere.services.PdfGeneratorService
import com.idroambiente.giornalieradicantiere.services.PdfRiepilogoData
import com.idroambiente.giornalieradicantiere.services.PersonaleWithData
import com.idroambiente.giornalieradicantiere.services.SaveResult
import com.idroambiente.giornalieradicantiere.util.formatCommessaNome
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class PersonaleFormData(
    val presente: Boolean = false,
    val oreViaggioAndata: Float = 0f,
    val oreViaggioRitorno: Float = 0f,
    val oreLavoro: Float = 0f,
    val orePausa: Float = 0f,
    val inizioTurno: Long = 0L,
    val fineTurno: Long = 0L,
    val pernotto: Boolean = false
)

data class MezzoFormData(
    val utilizzato: Boolean = false,
    val kmAndata: Float? = null,   // null = non compilato
    val kmRitorno: Float? = null   // null = non compilato
)

data class AttrezzaturaFormData(
    val utilizzata: Boolean = false
)

/**
 * Stato della generazione/salvataggio del PDF
 */
sealed class PdfStatus {
    data object Idle : PdfStatus()
    data object Generating : PdfStatus()
    data object Saving : PdfStatus()
    data class Success(val uri: Uri?, val fileName: String, val folderName: String?, val webViewLink: String? = null) : PdfStatus()
    data class ReadyToShare(val shareIntent: Intent, val pdfFile: File) : PdfStatus()
    data class SavedAndReadyToShare(val uri: Uri?, val fileName: String, val folderName: String?, val shareIntent: Intent) : PdfStatus()
    data class Error(val message: String?) : PdfStatus()
}

sealed class SendPhase {
    data object Idle : SendPhase()
    data object PreparingData : SendPhase()
    data object GeneratingPdf : SendPhase()
    data object GeneratingCsv : SendPhase()
    data object UploadingPdf : SendPhase()
    data object UploadingCsv : SendPhase()
    data object Finalizing : SendPhase()
}

data class PreviousMezzoDataResult(
    val message: String,
    val updatedCount: Int
)

data class WizardUiState(
    val currentStep: Int = 0,
    val personaleSelections: Map<Long, PersonaleFormData> = emptyMap(),
    val mezzoSelections: Map<Long, MezzoFormData> = emptyMap(),
    val attrezzaturaSelections: Map<Long, AttrezzaturaFormData> = emptyMap(),
    val isSaving: Boolean = false,
    val saveCompleted: Boolean = false,
    val isEditMode: Boolean = false,
    val giornaliera: Giornaliera? = null,
    val isLoading: Boolean = false,
    val riepilogo: RiepilogoData = RiepilogoData(),
    val note: String = "",
    val pdfStatus: PdfStatus = PdfStatus.Idle,
    val sendPhase: SendPhase = SendPhase.Idle,
    val showConfigErrorDialog: Boolean = false,
    val configErrorMessage: String? = null,
    val showOfflineDialog: Boolean = false,
    val duplicateError: String? = null,
    val selectedDate: Long? = null,
    val previousMezzoDataResult: PreviousMezzoDataResult? = null,
    val stepValidationError: String? = null
) {
    val isOperationInProgress: Boolean
        get() = isSaving || sendPhase != SendPhase.Idle
}

@HiltViewModel
class GiornalieraWizardViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val personaleRepository: PersonaleRepository,
    private val giornalieraRepository: GiornalieraRepository,
    private val personaleGiornalieraRepository: PersonaleGiornalieraRepository,
    private val mezzoRepository: MezzoRepository,
    private val mezzoGiornalieraRepository: MezzoGiornalieraRepository,
    private val attrezzaturaRepository: AttrezzaturaRepository,
    private val attrezzaturaGiornalieraRepository: AttrezzaturaGiornalieraRepository,
    private val cantiereRepository: CantiereRepository,
    private val commessaRepository: CommessaRepository,
    private val pdfGeneratorService: PdfGeneratorService,
    private val csvGeneratorService: CsvGeneratorService,
    private val fileStorageService: FileStorageService,
    private val preferencesRepository: PreferencesRepository,
    private val networkUtils: NetworkUtils,
    private val giornalieraStore: GiornalieraStore
) : ViewModel() {

    val cantiereId: Long = savedStateHandle.get<Long>("cantiereId") ?: 0L
    val giornalieraId: Long? = savedStateHandle.get<Long>("giornalieraId")?.takeIf { it != 0L }

    val isDetailMode: Boolean = giornalieraId != null

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
        if (isDetailMode) {
            loadExistingGiornaliera()
        } else {
            initializeEmptySelections()
        }
    }

    private fun initializeEmptySelections() {
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

    private fun loadExistingGiornaliera() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val giornaliera = giornalieraRepository.getById(giornalieraId!!)
                if (giornaliera == null) {
                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                }

                // Load personale giornaliera data
                val personaleGiornaliere = personaleGiornalieraRepository
                    .getByGiornalieraId(giornalieraId)
                    .first()

                val personaleSelections = mutableMapOf<Long, PersonaleFormData>()

                // First, initialize all personale with empty data
                val allPersonale = personaleList.first()
                allPersonale.forEach { personale ->
                    personaleSelections[personale.id] = PersonaleFormData()
                }

                // Then fill in the data for personale that were in the giornaliera
                personaleGiornaliere.forEach { pg ->
                    // Calcola orePausa da dati esistenti: orePausa = durataOre - oreLavoro
                    val orePausa = if (pg.inizioTurno > 0 && pg.fineTurno > 0) {
                        val durataMs = pg.fineTurno - pg.inizioTurno
                        val durataOre = durataMs / 1000f / 60f / 60f
                        maxOf(0f, durataOre - pg.oreLavoro)
                    } else {
                        0f
                    }
                    personaleSelections[pg.personaleId] = PersonaleFormData(
                        presente = pg.presente,
                        oreViaggioAndata = pg.oreViaggioAndata,
                        oreViaggioRitorno = pg.oreViaggioRitorno,
                        oreLavoro = pg.oreLavoro,
                        orePausa = orePausa,
                        inizioTurno = pg.inizioTurno,
                        fineTurno = pg.fineTurno,
                        pernotto = pg.pernotto
                    )
                }

                // Load mezzo giornaliera data
                val mezzoGiornaliere = mezzoGiornalieraRepository
                    .getByGiornalieraId(giornalieraId)
                    .first()

                val mezzoSelections = mutableMapOf<Long, MezzoFormData>()

                // First, initialize all mezzi with empty data
                val allMezzi = mezzoList.first()
                allMezzi.forEach { mezzo ->
                    mezzoSelections[mezzo.id] = MezzoFormData()
                }

                // Then fill in the data for mezzi that were in the giornaliera
                mezzoGiornaliere.forEach { mg ->
                    mezzoSelections[mg.mezzoId] = MezzoFormData(
                        utilizzato = true,
                        kmAndata = mg.kmAndata,
                        kmRitorno = mg.kmRitorno
                    )
                }

                // Load attrezzatura giornaliera data
                val attrezzaturaGiornaliere = attrezzaturaGiornalieraRepository
                    .getByGiornalieraId(giornalieraId)
                    .first()

                val attrezzaturaSelections = mutableMapOf<Long, AttrezzaturaFormData>()

                // First, initialize all attrezzature with empty data
                val allAttrezzature = attrezzaturaList.first()
                allAttrezzature.forEach { attrezzatura ->
                    attrezzaturaSelections[attrezzatura.id] = AttrezzaturaFormData()
                }

                // Then fill in the data for attrezzature that were in the giornaliera
                attrezzaturaGiornaliere.forEach { ag ->
                    attrezzaturaSelections[ag.attrezzaturaId] = AttrezzaturaFormData(utilizzata = true)
                }

                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        giornaliera = giornaliera,
                        personaleSelections = personaleSelections,
                        mezzoSelections = mezzoSelections,
                        attrezzaturaSelections = attrezzaturaSelections,
                        riepilogo = calculateRiepilogo(personaleSelections, mezzoSelections, attrezzaturaSelections),
                        note = giornaliera.note ?: "",
                        selectedDate = giornaliera.data
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun updatePersonaleData(personaleId: Long, data: PersonaleFormData) {
        _uiState.update { state ->
            val newSelections = state.personaleSelections + (personaleId to data)
            state.copy(
                personaleSelections = newSelections,
                riepilogo = calculateRiepilogo(newSelections, state.mezzoSelections, state.attrezzaturaSelections)
            )
        }
    }

    fun updateMezzoData(mezzoId: Long, data: MezzoFormData) {
        _uiState.update { state ->
            val newSelections = state.mezzoSelections + (mezzoId to data)
            state.copy(
                mezzoSelections = newSelections,
                riepilogo = calculateRiepilogo(state.personaleSelections, newSelections, state.attrezzaturaSelections)
            )
        }
    }

    fun updateAttrezzaturaData(attrezzaturaId: Long, data: AttrezzaturaFormData) {
        _uiState.update { state ->
            val newSelections = state.attrezzaturaSelections + (attrezzaturaId to data)
            state.copy(
                attrezzaturaSelections = newSelections,
                riepilogo = calculateRiepilogo(state.personaleSelections, state.mezzoSelections, newSelections)
            )
        }
    }

    fun updateSelectedDate(date: Long?) {
        _uiState.update { it.copy(selectedDate = date) }
    }

    fun updateNote(note: String) {
        _uiState.update { state ->
            state.copy(note = note)
        }
    }

    fun loadPreviousMezzoData() {
        viewModelScope.launch {
            try {
                val latestGiornaliera = giornalieraRepository.getLatestByCantiereId(cantiereId)

                // If editing, skip the current giornaliera
                val previousGiornaliera = if (latestGiornaliera != null && latestGiornaliera.id == giornalieraId) {
                    // Need to find the one before this - query all and pick second
                    null
                } else {
                    latestGiornaliera
                }

                if (previousGiornaliera == null) {
                    _uiState.update {
                        it.copy(previousMezzoDataResult = PreviousMezzoDataResult(
                            message = "Nessuna giornaliera precedente trovata per questo cantiere",
                            updatedCount = 0
                        ))
                    }
                    return@launch
                }

                // Load mezzo data from previous giornaliera
                val previousMezzoData = mezzoGiornalieraRepository
                    .getByGiornalieraId(previousGiornaliera.id)
                    .first()

                val previousDataMap = previousMezzoData.associateBy { it.mezzoId }

                val currentSelections = _uiState.value.mezzoSelections.toMutableMap()
                var updatedCount = 0

                // For each currently selected mezzo, copy KM values from previous
                currentSelections.forEach { (mezzoId, formData) ->
                    if (formData.utilizzato) {
                        val prev = previousDataMap[mezzoId]
                        if (prev != null) {
                            currentSelections[mezzoId] = formData.copy(
                                kmAndata = prev.kmAndata,
                                kmRitorno = prev.kmRitorno
                            )
                            updatedCount++
                        }
                    }
                }

                val dateStr = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.ITALIAN)
                    .format(java.util.Date(previousGiornaliera.data))

                val message = if (updatedCount > 0) {
                    "KM applicati dalla giornaliera del $dateStr ($updatedCount mezz${if (updatedCount == 1) "o" else "i"} aggiornati)"
                } else {
                    "Nessun mezzo corrispondente trovato nella giornaliera del $dateStr"
                }

                _uiState.update { state ->
                    state.copy(
                        mezzoSelections = currentSelections,
                        riepilogo = calculateRiepilogo(state.personaleSelections, currentSelections, state.attrezzaturaSelections),
                        previousMezzoDataResult = PreviousMezzoDataResult(
                            message = message,
                            updatedCount = updatedCount
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(previousMezzoDataResult = PreviousMezzoDataResult(
                        message = "Errore nel caricamento dati precedenti",
                        updatedCount = 0
                    ))
                }
            }
        }
    }

    fun clearPreviousMezzoDataResult() {
        _uiState.update { it.copy(previousMezzoDataResult = null) }
    }

    private fun calculateRiepilogo(
        personaleSelections: Map<Long, PersonaleFormData>,
        mezzoSelections: Map<Long, MezzoFormData>,
        attrezzaturaSelections: Map<Long, AttrezzaturaFormData>
    ): RiepilogoData {
        val personalePresenti = personaleSelections.filter { it.value.presente }
        val mezziUtilizzati = mezzoSelections.filter { it.value.utilizzato }
        val attrezzatureUtilizzate = attrezzaturaSelections.filter { it.value.utilizzata }

        return RiepilogoData(
            totalePersonePresenti = personalePresenti.size,
            totaleOreLavoro = personalePresenti.values.sumOf { it.oreLavoro.toDouble() }.toFloat(),
            totaleOreViaggio = personalePresenti.values.sumOf { (it.oreViaggioAndata + it.oreViaggioRitorno).toDouble() }.toFloat(),
            totalePernotti = personalePresenti.values.count { it.pernotto },
            totaleMezziUtilizzati = mezziUtilizzati.size,
            totaleKm = mezziUtilizzati.values.sumOf { ((it.kmAndata ?: 0f) + (it.kmRitorno ?: 0f)).toDouble() }.toFloat(),
            totaleAttrezzatureUtilizzate = attrezzatureUtilizzate.size
        )
    }

    fun nextStep() {
        _uiState.update { state ->
            if (state.currentStep < 3) {
                val validationError = validateCurrentStep(state)
                if (validationError != null) {
                    state.copy(stepValidationError = validationError)
                } else {
                    state.copy(currentStep = state.currentStep + 1, stepValidationError = null)
                }
            } else {
                state
            }
        }
    }

    private fun validateCurrentStep(state: WizardUiState): String? {
        return when (state.currentStep) {
            0 -> {
                val presenti = state.personaleSelections.values.filter { it.presente }
                if (presenti.isEmpty()) {
                    "Seleziona almeno una persona"
                } else if (presenti.any { it.oreLavoro <= 0f }) {
                    "Compila il turno (inizio/fine) per tutte le persone selezionate"
                } else null
            }
            1 -> {
                val utilizzati = state.mezzoSelections.values.filter { it.utilizzato }
                if (utilizzati.isEmpty()) {
                    "Seleziona almeno un mezzo"
                } else if (utilizzati.any { it.kmAndata == null || it.kmRitorno == null }) {
                    "Compila i km per tutti i mezzi selezionati"
                } else null
            }
            else -> null
        }
    }

    fun clearStepValidationError() {
        _uiState.update { it.copy(stepValidationError = null) }
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

    fun toggleEditMode() {
        _uiState.update { state ->
            state.copy(isEditMode = !state.isEditMode)
        }
    }

    fun saveGiornaliera() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            try {
                if (isDetailMode && giornalieraId != null) {
                    // Update existing giornaliera
                    val existingGiornaliera = _uiState.value.giornaliera
                    if (existingGiornaliera != null) {
                        giornalieraStore.salva(
                            existingGiornaliera.copy(
                                note = _uiState.value.note.ifBlank { null },
                                updatedAt = System.currentTimeMillis()
                            ),
                            buildRighe()
                        )
                    }
                } else {
                    // Create new giornaliera
                    val currentTime = _uiState.value.selectedDate ?: System.currentTimeMillis()

                    // Check for duplicate giornaliera for same cantiere on same day
                    if (giornalieraRepository.existsForCantiereAndDay(cantiereId, currentTime)) {
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                duplicateError = "Esiste già una giornaliera per questo cantiere in questa data"
                            )
                        }
                        return@launch
                    }

                    val giornaliera = Giornaliera(
                        cantiereId = cantiereId,
                        data = currentTime,
                        stato = StatoGiornaliera.BOZZA,
                        note = _uiState.value.note.ifBlank { null }
                    )
                    giornalieraStore.salva(giornaliera, buildRighe())
                }

                _uiState.update { it.copy(isSaving = false, saveCompleted = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    /** Righe da salvare, costruite dallo stato corrente del wizard. */
    private fun buildRighe(): GiornalieraStore.Righe {
        val state = _uiState.value
        val personale = state.personaleSelections
            .filter { it.value.presente }
            .map { (personaleId, data) ->
                PersonaleGiornaliera(
                    giornalieraId = 0,
                    personaleId = personaleId,
                    presente = data.presente,
                    oreViaggioAndata = maxOf(0f, data.oreViaggioAndata),
                    oreViaggioRitorno = maxOf(0f, data.oreViaggioRitorno),
                    oreLavoro = maxOf(0f, data.oreLavoro),
                    inizioTurno = data.inizioTurno,
                    fineTurno = data.fineTurno,
                    pernotto = data.pernotto
                )
            }
        val mezzi = state.mezzoSelections
            .filter { it.value.utilizzato }
            .map { (mezzoId, data) ->
                MezzoGiornaliera(
                    giornalieraId = 0,
                    mezzoId = mezzoId,
                    kmAndata = maxOf(0f, data.kmAndata ?: 0f),
                    kmRitorno = maxOf(0f, data.kmRitorno ?: 0f)
                )
            }
        val attrezzature = state.attrezzaturaSelections
            .filter { it.value.utilizzata }
            .map { (attrezzaturaId, _) -> AttrezzaturaGiornaliera(giornalieraId = 0, attrezzaturaId = attrezzaturaId) }
        return GiornalieraStore.Righe(personale, mezzi, attrezzature)
    }

    fun sendGiornaliera(presaVisionePOS: Boolean = true) {
        viewModelScope.launch {
            var giornalieraToSend = _uiState.value.giornaliera
                ?: return@launch

            // Se siamo in edit mode, salva prima le modifiche nel DB
            if (_uiState.value.isEditMode) {
                _uiState.update { it.copy(isSaving = true) }
                try {
                    giornalieraToSend = giornalieraToSend.copy(
                        note = _uiState.value.note.ifBlank { null },
                        presaVisionePOS = presaVisionePOS,
                        updatedAt = System.currentTimeMillis()
                    )
                    giornalieraStore.salva(giornalieraToSend, buildRighe())
                    _uiState.update { it.copy(isSaving = false, giornaliera = giornalieraToSend) }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            sendPhase = SendPhase.Idle,
                            pdfStatus = PdfStatus.Error(e.message ?: "Errore durante il salvataggio")
                        )
                    }
                    return@launch
                }
            } else {
                // Giornaliera esistente (es. BOZZA): aggiorna presaVisionePOS
                giornalieraToSend = giornalieraToSend.copy(
                    presaVisionePOS = presaVisionePOS,
                    updatedAt = System.currentTimeMillis()
                )
                giornalieraRepository.update(giornalieraToSend)
                _uiState.update { it.copy(giornaliera = giornalieraToSend) }
            }

            performSend(giornalieraToSend)
        }
    }

    fun saveAndSendGiornaliera(presaVisionePOS: Boolean = true) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                // 1. Salva la giornaliera nel DB come BOZZA
                val currentTime = _uiState.value.selectedDate ?: System.currentTimeMillis()
                
                // Check for duplicate giornaliera for same cantiere on same day
                if (giornalieraRepository.existsForCantiereAndDay(cantiereId, currentTime)) {
                    _uiState.update { 
                        it.copy(
                            isSaving = false,
                            sendPhase = SendPhase.Idle,
                            duplicateError = "Esiste già una giornaliera per questo cantiere in questa data",
                            pdfStatus = PdfStatus.Error("Esiste già una giornaliera per questo cantiere in questa data")
                        )
                    }
                    return@launch
                }
                
                val giornaliera = Giornaliera(
                    cantiereId = cantiereId,
                    data = currentTime,
                    stato = StatoGiornaliera.BOZZA,
                    note = _uiState.value.note.ifBlank { null },
                    presaVisionePOS = presaVisionePOS
                )
                // 2. Salva giornaliera e righe in un'unica transazione
                val newId = giornalieraStore.salva(giornaliera, buildRighe())

                // 3. Ricarica l'entità salvata dal DB
                val savedGiornaliera = giornalieraRepository.getById(newId)
                    ?: throw Exception("Giornaliera appena salvata non trovata")

                _uiState.update { it.copy(giornaliera = savedGiornaliera, isSaving = false) }

                // 4. Esegui l'invio (PDF + export)
                performSend(savedGiornaliera)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        sendPhase = SendPhase.Idle,
                        pdfStatus = PdfStatus.Error(e.message ?: "Errore durante il salvataggio")
                    )
                }
            }
        }
    }

    private suspend fun performSend(giornaliera: Giornaliera) {
        try {
            // 0. Pre-validation: check configuration is valid before anything
            val storageMode = preferencesRepository.storageMode.first()
            val exportBehavior = if (storageMode == StorageMode.GOOGLE_DRIVE) {
                ExportBehavior.SAVE
            } else {
                preferencesRepository.exportBehavior.first()
            }

            // Validate configuration
            val validationError = validateExportConfiguration(storageMode, exportBehavior)
            if (validationError != null) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        sendPhase = SendPhase.Idle,
                        showConfigErrorDialog = true,
                        configErrorMessage = validationError
                    )
                }
                return
            }

            // Network check for Google Drive
            if (storageMode == StorageMode.GOOGLE_DRIVE && !networkUtils.isOnline()) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        sendPhase = SendPhase.Idle,
                        showOfflineDialog = true
                    )
                }
                return
            }

            _uiState.update {
                it.copy(
                    isSaving = true,
                    sendPhase = SendPhase.PreparingData,
                    pdfStatus = PdfStatus.Generating
                )
            }

            // 1. Raccogli tutti i dati per il PDF (prima di cambiare stato)
            val pdfData = collectPdfData(giornaliera)
            val commessaNome = formatCommessaNome(pdfData.commessa.nome)
            val giornalieraData = giornaliera.data

            // 2. Genera PDF
            _uiState.update { it.copy(sendPhase = SendPhase.GeneratingPdf, pdfStatus = PdfStatus.Generating) }
            val pdfFile = pdfGeneratorService.generateGiornalieraPdf(pdfData)

            // 2b. Genera CSV (best-effort, non bloccante)
            _uiState.update { it.copy(sendPhase = SendPhase.GeneratingCsv) }
            val csvFile = try {
                csvGeneratorService.generateGiornalieraCsv(pdfData)
            } catch (_: Exception) {
                null
            }

            // 3. Salva/Condividi in base alle preferenze
            when (exportBehavior) {
                ExportBehavior.SAVE -> {
                    _uiState.update {
                        it.copy(
                            sendPhase = SendPhase.UploadingPdf,
                            pdfStatus = PdfStatus.Saving
                        )
                    }
                    val saveResult = fileStorageService.savePdf(pdfFile, commessaNome, giornalieraData)
                    when (saveResult) {
                        is SaveResult.Success -> {
                            // Salva anche il CSV (best-effort)
                            if (csvFile != null) {
                                _uiState.update { it.copy(sendPhase = SendPhase.UploadingCsv) }
                                try { fileStorageService.saveCsv(csvFile, commessaNome, giornalieraData) } catch (_: Exception) {}
                            }
                            // Aggiorna stato a INVIATA solo dopo salvataggio riuscito
                            _uiState.update { it.copy(sendPhase = SendPhase.Finalizing) }
                            markAsInviata(giornaliera)
                            _uiState.update {
                                it.copy(
                                    isSaving = false,
                                    sendPhase = SendPhase.Idle,
                                    saveCompleted = true,
                                    pdfStatus = PdfStatus.Success(
                                        uri = saveResult.uri,
                                        fileName = saveResult.fileName,
                                        folderName = saveResult.folderName,
                                        webViewLink = saveResult.webViewLink
                                    )
                                )
                            }
                        }
                        is SaveResult.Error -> {
                            // Fallback: salva localmente
                            _uiState.update { it.copy(sendPhase = SendPhase.UploadingPdf, pdfStatus = PdfStatus.Saving) }
                            val localResult = fileStorageService.savePdfToAppStorage(pdfFile)
                            when (localResult) {
                                is SaveResult.Success -> {
                                    // Salva anche CSV nel fallback (best-effort)
                                    if (csvFile != null) {
                                        _uiState.update { it.copy(sendPhase = SendPhase.UploadingCsv) }
                                        try { fileStorageService.saveCsvToAppStorage(csvFile) } catch (_: Exception) {}
                                    }
                                    _uiState.update { it.copy(sendPhase = SendPhase.Finalizing) }
                                    markAsInviata(giornaliera)
                                    _uiState.update {
                                        it.copy(
                                            isSaving = false,
                                            sendPhase = SendPhase.Idle,
                                            saveCompleted = true,
                                            pdfStatus = PdfStatus.Success(
                                                uri = localResult.uri,
                                                fileName = localResult.fileName,
                                                folderName = "memoria locale"
                                            )
                                        )
                                    }
                                }
                                is SaveResult.Error -> {
                                    _uiState.update {
                                        it.copy(
                                            isSaving = false,
                                            sendPhase = SendPhase.Idle,
                                            pdfStatus = PdfStatus.Error(localResult.message)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                ExportBehavior.SHARE -> {
                    // Share rimane solo PDF, ma salva CSV se possibile
                    if (csvFile != null) {
                        _uiState.update { it.copy(sendPhase = SendPhase.UploadingCsv) }
                        try { fileStorageService.saveCsvToAppStorage(csvFile) } catch (_: Exception) {}
                    }
                    val shareIntent = fileStorageService.createShareChooserIntent(pdfFile)
                    _uiState.update {
                        it.copy(
                            sendPhase = SendPhase.Finalizing
                        )
                    }
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            sendPhase = SendPhase.Idle,
                            saveCompleted = true,
                            pdfStatus = PdfStatus.ReadyToShare(shareIntent, pdfFile)
                        )
                    }
                }
                ExportBehavior.BOTH -> {
                    _uiState.update {
                        it.copy(
                            sendPhase = SendPhase.UploadingPdf,
                            pdfStatus = PdfStatus.Saving
                        )
                    }

                    // Copia il file cache prima del salvataggio per evitare doppia generazione
                    val pdfCopy = File(pdfFile.parent, "share_${pdfFile.name}")
                    pdfFile.copyTo(pdfCopy, overwrite = true)

                    val saveResult = fileStorageService.savePdf(pdfFile, commessaNome, giornalieraData)
                    val shareIntent = fileStorageService.createShareChooserIntent(pdfCopy)

                    when (saveResult) {
                        is SaveResult.Success -> {
                            // Salva anche il CSV (best-effort)
                            if (csvFile != null) {
                                _uiState.update { it.copy(sendPhase = SendPhase.UploadingCsv) }
                                try { fileStorageService.saveCsv(csvFile, commessaNome, giornalieraData) } catch (_: Exception) {}
                            }
                            _uiState.update { it.copy(sendPhase = SendPhase.Finalizing) }
                            markAsInviata(giornaliera)
                            _uiState.update {
                                it.copy(
                                    isSaving = false,
                                    sendPhase = SendPhase.Idle,
                                    saveCompleted = true,
                                    pdfStatus = PdfStatus.SavedAndReadyToShare(
                                        uri = saveResult.uri,
                                        fileName = saveResult.fileName,
                                        folderName = saveResult.folderName,
                                        shareIntent = shareIntent
                                    )
                                )
                            }
                        }
                        is SaveResult.Error -> {
                            // Salvataggio fallito ma condividi comunque
                            _uiState.update { it.copy(sendPhase = SendPhase.Finalizing) }
                            _uiState.update {
                                it.copy(
                                    isSaving = false,
                                    sendPhase = SendPhase.Idle,
                                    saveCompleted = true,
                                    pdfStatus = PdfStatus.ReadyToShare(shareIntent, pdfCopy)
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isSaving = false,
                    sendPhase = SendPhase.Idle,
                    pdfStatus = PdfStatus.Error(e.message ?: "Errore durante l'invio")
                )
            }
        }
    }

    private suspend fun markAsInviata(giornaliera: Giornaliera) {
        val updated = giornaliera.copy(
            stato = StatoGiornaliera.INVIATA,
            updatedAt = System.currentTimeMillis()
        )
        giornalieraRepository.update(updated)
        _uiState.update { it.copy(giornaliera = updated) }
    }

    private suspend fun validateExportConfiguration(
        storageMode: StorageMode,
        exportBehavior: ExportBehavior
    ): String? {
        return when (storageMode) {
            StorageMode.GOOGLE_DRIVE -> {
                val rootFolderId = preferencesRepository.googleDriveRootFolderId.first()
                val email = preferencesRepository.googleUserEmail.first()
                when {
                    email.isNullOrEmpty() -> "Account Google non connesso. Vai alle impostazioni per collegare il tuo account."
                    rootFolderId.isNullOrEmpty() -> "Cartella Google Drive non selezionata. Vai alle impostazioni per sceglierne una."
                    else -> null
                }
            }
            StorageMode.LOCAL -> {
                when (exportBehavior) {
                    ExportBehavior.SHARE -> null // No folder needed for share only
                    else -> {
                        val folderUri = preferencesRepository.exportFolderUri.first()
                        when {
                            folderUri.isNullOrEmpty() -> "Cartella di salvataggio non configurata. Vai alle impostazioni per selezionarne una."
                            !fileStorageService.isConfiguredFolderAccessible() -> "La cartella di salvataggio non è più accessibile. Vai alle impostazioni per selezionarne una nuova."
                            else -> null
                        }
                    }
                }
            }
        }
    }

    fun dismissConfigErrorDialog() {
        _uiState.update { it.copy(showConfigErrorDialog = false, configErrorMessage = null) }
    }

    fun dismissOfflineDialog() {
        _uiState.update { it.copy(showOfflineDialog = false) }
    }

    fun clearDuplicateError() {
        _uiState.update { it.copy(duplicateError = null) }
    }

    /**
     * Raccoglie tutti i dati necessari per generare il PDF della giornaliera
     */
    private suspend fun collectPdfData(giornaliera: Giornaliera): GiornalieraPdfData {
        // Ottieni il cantiere
        val cantiere = cantiereRepository.getById(giornaliera.cantiereId)
            ?: throw Exception("Cantiere non trovato")

        // Ottieni la commessa
        val commessa = commessaRepository.getById(cantiere.commessaId)
            ?: throw Exception("Commessa non trovata")

        // Ottieni la lista personale con i dati della giornaliera
        val allPersonale = personaleList.first()
        val personalePresenti = _uiState.value.personaleSelections
            .filter { it.value.presente }
            .mapNotNull { (personaleId, formData) ->
                val personale = allPersonale.find { it.id == personaleId }
                if (personale != null) {
                    PersonaleWithData(
                        personale = personale,
                        giornalieraData = PersonaleGiornaliera(
                            giornalieraId = giornaliera.id,
                            personaleId = personaleId,
                            presente = formData.presente,
                            oreViaggioAndata = formData.oreViaggioAndata,
                            oreViaggioRitorno = formData.oreViaggioRitorno,
                            oreLavoro = formData.oreLavoro,
                            inizioTurno = formData.inizioTurno,
                            fineTurno = formData.fineTurno,
                            pernotto = formData.pernotto
                        )
                    )
                } else null
            }

        // Ottieni la lista mezzi con i dati della giornaliera
        val allMezzi = mezzoList.first()
        val mezziUtilizzati = _uiState.value.mezzoSelections
            .filter { it.value.utilizzato }
            .mapNotNull { (mezzoId, formData) ->
                val mezzo = allMezzi.find { it.id == mezzoId }
                if (mezzo != null) {
                    MezzoWithData(
                        mezzo = mezzo,
                        giornalieraData = MezzoGiornaliera(
                            giornalieraId = giornaliera.id,
                            mezzoId = mezzoId,
                            kmAndata = formData.kmAndata ?: 0f,
                            kmRitorno = formData.kmRitorno ?: 0f
                        )
                    )
                } else null
            }

        // Ottieni le attrezzature utilizzate
        val allAttrezzature = attrezzaturaList.first()
        val attrezzatureUtilizzate = _uiState.value.attrezzaturaSelections
            .filter { it.value.utilizzata }
            .mapNotNull { (attrezzaturaId, _) ->
                allAttrezzature.find { it.id == attrezzaturaId }
            }

        // Ottieni il nome utente Google
        val userName = preferencesRepository.googleUserDisplayName.first()

        // Calcola il riepilogo per il PDF
        val riepilogo = _uiState.value.riepilogo
        val pdfRiepilogo = PdfRiepilogoData(
            totalePersonePresenti = riepilogo.totalePersonePresenti,
            totaleOreLavoro = riepilogo.totaleOreLavoro,
            totaleOreViaggio = riepilogo.totaleOreViaggio,
            totalePernotti = riepilogo.totalePernotti,
            totaleMezziUtilizzati = riepilogo.totaleMezziUtilizzati,
            totaleKm = riepilogo.totaleKm,
            totaleAttrezzatureUtilizzate = riepilogo.totaleAttrezzatureUtilizzate
        )

        return GiornalieraPdfData(
            giornaliera = giornaliera,
            cantiere = cantiere,
            commessa = commessa,
            personaleList = personalePresenti,
            mezziList = mezziUtilizzati,
            attrezzatureList = attrezzatureUtilizzate,
            riepilogo = pdfRiepilogo,
            userName = userName
        )
    }

    /**
     * Resetta lo stato del PDF (da chiamare dopo aver gestito il risultato)
     */
    fun resetPdfStatus() {
        _uiState.update { it.copy(pdfStatus = PdfStatus.Idle, sendPhase = SendPhase.Idle, isSaving = false) }
    }

    /**
     * Conferma il completamento dopo aver gestito il PDF
     */
    fun confirmCompletion() {
        viewModelScope.launch {
            val currentStatus = _uiState.value.pdfStatus
            val giornaliera = _uiState.value.giornaliera
            
            // Marca come INVIATA se è stato condiviso (ReadyToShare o SavedAndReadyToShare)
            if (giornaliera != null && 
                (currentStatus is PdfStatus.ReadyToShare || currentStatus is PdfStatus.SavedAndReadyToShare)) {
                markAsInviata(giornaliera)
            }
            
            _uiState.update { it.copy(saveCompleted = true) }
        }
    }
}
