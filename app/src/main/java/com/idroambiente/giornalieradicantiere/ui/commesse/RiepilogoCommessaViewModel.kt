package com.idroambiente.giornalieradicantiere.ui.commesse

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idroambiente.giornalieradicantiere.data.preferences.PreferencesRepository
import com.idroambiente.giornalieradicantiere.data.preferences.StorageMode
import com.idroambiente.giornalieradicantiere.database.entities.Commessa
import com.idroambiente.giornalieradicantiere.database.entities.Giornaliera
import com.idroambiente.giornalieradicantiere.database.repository.CantiereRepository
import com.idroambiente.giornalieradicantiere.database.repository.CommessaRepository
import com.idroambiente.giornalieradicantiere.database.repository.AttrezzaturaGiornalieraRepository
import com.idroambiente.giornalieradicantiere.database.repository.AttrezzaturaRepository
import com.idroambiente.giornalieradicantiere.database.repository.GiornalieraRepository
import com.idroambiente.giornalieradicantiere.database.repository.MezzoGiornalieraRepository
import com.idroambiente.giornalieradicantiere.database.repository.MezzoRepository
import com.idroambiente.giornalieradicantiere.database.repository.PersonaleGiornalieraRepository
import com.idroambiente.giornalieradicantiere.services.AttrezzaturaCsvData
import com.idroambiente.giornalieradicantiere.services.AttrezzaturaRiepilogoItem
import com.idroambiente.giornalieradicantiere.services.CsvGeneratorService
import com.idroambiente.giornalieradicantiere.services.FileStorageService
import com.idroambiente.giornalieradicantiere.services.MezzoCsvData
import com.idroambiente.giornalieradicantiere.services.MezzoRiepilogoItem
import com.idroambiente.giornalieradicantiere.services.EsitoSostituzione
import com.idroambiente.giornalieradicantiere.services.NetworkUtils
import com.idroambiente.giornalieradicantiere.services.RiepilogoExporter
import com.idroambiente.giornalieradicantiere.services.PdfGeneratorService
import com.idroambiente.giornalieradicantiere.services.RiepilogoCommessaCsvData
import com.idroambiente.giornalieradicantiere.services.RiepilogoCommessaPdfData
import com.idroambiente.giornalieradicantiere.services.RiepilogoGiornalieraItem
import com.idroambiente.giornalieradicantiere.services.SaveResult
import com.idroambiente.giornalieradicantiere.util.formatCommessaNome
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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

data class PdfExportResult(
    val fileName: String,
    val folderName: String?,
    val webViewLink: String?,
    /** true = esisteva già un report con questo nome ed è stato sostituito. */
    val sovrascritto: Boolean = false,
    val csvErrore: String? = null
)

@HiltViewModel
class RiepilogoCommessaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val commessaRepository: CommessaRepository,
    private val cantiereRepository: CantiereRepository,
    private val giornalieraRepository: GiornalieraRepository,
    private val personaleGiornalieraRepository: PersonaleGiornalieraRepository,
    private val mezzoGiornalieraRepository: MezzoGiornalieraRepository,
    private val mezzoRepository: MezzoRepository,
    private val attrezzaturaRepository: AttrezzaturaRepository,
    private val attrezzaturaGiornalieraRepository: AttrezzaturaGiornalieraRepository,
    private val pdfGeneratorService: PdfGeneratorService,
    private val csvGeneratorService: CsvGeneratorService,
    private val fileStorageService: FileStorageService,
    private val preferencesRepository: PreferencesRepository,
    private val networkUtils: NetworkUtils,
    private val riepilogoExporter: RiepilogoExporter
) : ViewModel() {

    val commessaId: Long = savedStateHandle.get<Long>("commessaId") ?: 0L

    private val _commessa = MutableStateFlow<Commessa?>(null)
    val commessa: StateFlow<Commessa?> = _commessa.asStateFlow()

    private val _cantiereId = MutableStateFlow<Long?>(null)
    val cantiereId: StateFlow<Long?> = _cantiereId.asStateFlow()

    private val _startDate = MutableStateFlow<Long?>(null)
    val startDate: StateFlow<Long?> = _startDate.asStateFlow()

    private val _endDate = MutableStateFlow<Long?>(null)
    val endDate: StateFlow<Long?> = _endDate.asStateFlow()

    private val _allGiornaliere: StateFlow<List<Giornaliera>> = _cantiereId
        .filterNotNull()
        .flatMapLatest { cantiereId ->
            giornalieraRepository.getByCantiereId(cantiereId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val giornaliere: StateFlow<List<Giornaliera>> = combine(
        _allGiornaliere, _startDate, _endDate
    ) { list, start, end ->
        list.filter { g ->
            (start == null || g.data >= start) &&
            (end == null || g.data <= end + 86_399_999L)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _riepilogo = MutableStateFlow(RiepilogoCommessaData())
    val riepilogo: StateFlow<RiepilogoCommessaData> = _riepilogo.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // PDF export states
    private val _pdfExporting = MutableStateFlow(false)
    val pdfExporting: StateFlow<Boolean> = _pdfExporting.asStateFlow()

    private val _pdfExportSuccess = MutableStateFlow<PdfExportResult?>(null)
    val pdfExportSuccess: StateFlow<PdfExportResult?> = _pdfExportSuccess.asStateFlow()

    private val _pdfExportError = MutableStateFlow<String?>(null)
    val pdfExportError: StateFlow<String?> = _pdfExportError.asStateFlow()

    private val _showConfigError = MutableStateFlow<String?>(null)
    val showConfigError: StateFlow<String?> = _showConfigError.asStateFlow()

    private val _showOfflineDialog = MutableStateFlow(false)
    val showOfflineDialog: StateFlow<Boolean> = _showOfflineDialog.asStateFlow()

    init {
        loadData()
        viewModelScope.launch {
            giornaliere.collect { list ->
                updateRiepilogo(list)
            }
        }
    }

    fun setStartDate(date: Long?) {
        _startDate.value = date
    }

    fun setEndDate(date: Long?) {
        _endDate.value = date
    }

    fun clearDateFilter() {
        _startDate.value = null
        _endDate.value = null
    }

    fun dismissExportSuccess() {
        _pdfExportSuccess.value = null
    }

    fun dismissExportError() {
        _pdfExportError.value = null
    }

    fun dismissConfigError() {
        _showConfigError.value = null
    }

    fun dismissOfflineDialog() {
        _showOfflineDialog.value = false
    }

    fun exportPdf() {
        viewModelScope.launch {
            if (_commessa.value == null) return@launch

            // Validate configuration
            val validationError = validateExportConfiguration()
            if (validationError != null) {
                _showConfigError.value = validationError
                return@launch
            }

            // Network check for Google Drive
            val storageMode = preferencesRepository.storageMode.first()
            if (storageMode == StorageMode.GOOGLE_DRIVE && !networkUtils.isOnline()) {
                _showOfflineDialog.value = true
                return@launch
            }

            _pdfExporting.value = true
            // Il report viene sovrascritto se esiste già (niente doppioni) e annotato,
            // così si aggiorna da solo quando si modifica una giornaliera.
            riepilogoExporter.esporta(commessaId, _startDate.value, _endDate.value)
                .onSuccess { esito ->
                    _pdfExportSuccess.value = PdfExportResult(
                        fileName = esito.fileName,
                        folderName = esito.folderName,
                        webViewLink = esito.webViewLink,
                        sovrascritto = esito.esito == EsitoSostituzione.SOSTITUITO,
                        csvErrore = esito.csvErrore
                    )
                }
                .onFailure { e ->
                    _pdfExportError.value = e.message ?: "Errore durante la generazione del PDF"
                }
            _pdfExporting.value = false
        }
    }

    private suspend fun validateExportConfiguration(): String? {
        val storageMode = preferencesRepository.storageMode.first()
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
                val folderUri = preferencesRepository.exportFolderUri.first()
                when {
                    folderUri.isNullOrEmpty() -> "Cartella di salvataggio non configurata. Vai alle impostazioni per selezionarne una."
                    !fileStorageService.isConfiguredFolderAccessible() -> "La cartella di salvataggio non è più accessibile. Vai alle impostazioni per selezionarne una nuova."
                    else -> null
                }
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
