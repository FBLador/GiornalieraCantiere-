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
import com.idroambiente.giornalieradicantiere.database.repository.CantiereRepository
import com.idroambiente.giornalieradicantiere.database.repository.CommessaRepository
import com.idroambiente.giornalieradicantiere.database.repository.GiornalieraRepository
import com.idroambiente.giornalieradicantiere.database.repository.MezzoGiornalieraRepository
import com.idroambiente.giornalieradicantiere.database.repository.MezzoRepository
import com.idroambiente.giornalieradicantiere.database.repository.PersonaleGiornalieraRepository
import com.idroambiente.giornalieradicantiere.database.repository.PersonaleRepository
import com.idroambiente.giornalieradicantiere.pdf.AttrezzaturaPdfEntry
import com.idroambiente.giornalieradicantiere.pdf.GiornalieraPdfGenerator
import com.idroambiente.giornalieradicantiere.pdf.MezzoPdfEntry
import com.idroambiente.giornalieradicantiere.pdf.PdfFolderRepository
import com.idroambiente.giornalieradicantiere.pdf.PersonalePdfEntry
import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    val saveCompleted: Boolean = false,
    val isEditMode: Boolean = false,
    val giornaliera: Giornaliera? = null,
    val isLoading: Boolean = false,
    val riepilogo: RiepilogoData = RiepilogoData(),
    val note: String = "",
    val pdfFolderLabel: String? = null,
    val pdfFolderUri: String? = null
)

@HiltViewModel
class GiornalieraWizardViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val personaleRepository: PersonaleRepository,
    private val giornalieraRepository: GiornalieraRepository,
    private val cantiereRepository: CantiereRepository,
    private val commessaRepository: CommessaRepository,
    private val personaleGiornalieraRepository: PersonaleGiornalieraRepository,
    private val mezzoRepository: MezzoRepository,
    private val mezzoGiornalieraRepository: MezzoGiornalieraRepository,
    private val attrezzaturaRepository: AttrezzaturaRepository,
    private val attrezzaturaGiornalieraRepository: AttrezzaturaGiornalieraRepository,
    private val giornalieraPdfGenerator: GiornalieraPdfGenerator,
    private val pdfFolderRepository: PdfFolderRepository
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
        loadPdfFolder()
        if (isDetailMode) {
            loadExistingGiornaliera()
        } else {
            initializeEmptySelections()
        }
    }

    private fun loadPdfFolder() {
        val uri = pdfFolderRepository.getFolderUri()
        _uiState.update { state ->
            state.copy(
                pdfFolderUri = uri?.toString(),
                pdfFolderLabel = pdfFolderRepository.getFolderLabel(uri)
            )
        }
    }

    fun onPdfFolderSelected(uri: Uri) {
        pdfFolderRepository.persistAccess(uri)
        pdfFolderRepository.saveFolderUri(uri)
        _uiState.update { state ->
            state.copy(
                pdfFolderUri = uri.toString(),
                pdfFolderLabel = pdfFolderRepository.getFolderLabel(uri)
            )
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
                    personaleSelections[pg.personaleId] = PersonaleFormData(
                        presente = pg.presente,
                        oreViaggioAndata = pg.oreViaggioAndata,
                        oreViaggioRitorno = pg.oreViaggioRitorno,
                        oreLavoro = pg.oreLavoro,
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
                        note = giornaliera.note ?: ""
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

    fun updateNote(note: String) {
        _uiState.update { state ->
            state.copy(note = note)
        }
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
            totaleKm = mezziUtilizzati.values.sumOf { (it.kmAndata + it.kmRitorno).toDouble() }.toFloat(),
            totaleAttrezzatureUtilizzate = attrezzatureUtilizzate.size
        )
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
                        giornalieraRepository.update(
                            existingGiornaliera.copy(
                                note = _uiState.value.note.ifBlank { null },
                                updatedAt = System.currentTimeMillis()
                            )
                        )

                        // Delete existing relations and re-insert
                        personaleGiornalieraRepository.deleteByGiornalieraId(giornalieraId)
                        mezzoGiornalieraRepository.deleteByGiornalieraId(giornalieraId)
                        attrezzaturaGiornalieraRepository.deleteByGiornalieraId(giornalieraId)

                        saveRelations(giornalieraId)
                    }
                } else {
                    // Create new giornaliera
                    val giornaliera = Giornaliera(
                        cantiereId = cantiereId,
                        data = System.currentTimeMillis(),
                        stato = StatoGiornaliera.BOZZA,
                        note = _uiState.value.note.ifBlank { null }
                    )
                    val newGiornalieraId = giornalieraRepository.insert(giornaliera)
                    saveRelations(newGiornalieraId)
                }

                _uiState.update { it.copy(isSaving = false, saveCompleted = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    private suspend fun saveRelations(giornalieraId: Long) {
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
    }

    fun sendGiornaliera() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            try {
                if (giornalieraId != null) {
                    val existingGiornaliera = _uiState.value.giornaliera
                    if (existingGiornaliera != null) {
                        val updatedGiornaliera = existingGiornaliera.copy(
                            stato = StatoGiornaliera.INVIATA,
                            updatedAt = System.currentTimeMillis()
                        )
                        giornalieraRepository.update(updatedGiornaliera)
                        generatePdf(updatedGiornaliera)
                    }
                }
                _uiState.update { it.copy(isSaving = false, saveCompleted = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    private suspend fun generatePdf(giornaliera: Giornaliera) {
        val personaleById = personaleList.value.associateBy { it.id }
        val mezziById = mezzoList.value.associateBy { it.id }
        val attrezzatureById = attrezzaturaList.value.associateBy { it.id }

        val personaleEntries = _uiState.value.personaleSelections
            .filter { it.value.presente }
            .mapNotNull { (personaleId, data) ->
                val persona = personaleById[personaleId] ?: return@mapNotNull null
                PersonalePdfEntry(
                    nomeCompleto = "${persona.nome} ${persona.cognome}",
                    oreLavoro = data.oreLavoro,
                    oreViaggio = data.oreViaggioAndata + data.oreViaggioRitorno,
                    pernotto = data.pernotto,
                    inizioTurno = data.inizioTurno,
                    fineTurno = data.fineTurno
                )
            }

        val mezziEntries = _uiState.value.mezzoSelections
            .filter { it.value.utilizzato }
            .mapNotNull { (mezzoId, data) ->
                val mezzo = mezziById[mezzoId] ?: return@mapNotNull null
                MezzoPdfEntry(
                    nome = mezzo.nome,
                    targa = mezzo.targa,
                    kmAndata = data.kmAndata,
                    kmRitorno = data.kmRitorno
                )
            }

        val attrezzatureEntries = _uiState.value.attrezzaturaSelections
            .filter { it.value.utilizzata }
            .mapNotNull { (attrezzaturaId, _) ->
                val attrezzatura = attrezzatureById[attrezzaturaId] ?: return@mapNotNull null
                AttrezzaturaPdfEntry(nome = attrezzatura.nome)
            }

        val cantiere = cantiereRepository.getById(giornaliera.cantiereId)
        val commessa = cantiere?.let { commessaRepository.getById(it.commessaId) }

        giornalieraPdfGenerator.generatePdf(
            giornaliera = giornaliera,
            cantiere = cantiere,
            commessa = commessa,
            personale = personaleEntries,
            mezzi = mezziEntries,
            attrezzature = attrezzatureEntries,
            riepilogo = _uiState.value.riepilogo,
            note = _uiState.value.note,
            folderUri = pdfFolderRepository.getFolderUri()
        )
    }
}
