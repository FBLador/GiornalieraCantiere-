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
import com.idroambiente.giornalieradicantiere.database.repository.GiornalieraRepository
import com.idroambiente.giornalieradicantiere.database.repository.MezzoGiornalieraRepository
import com.idroambiente.giornalieradicantiere.database.repository.PersonaleGiornalieraRepository
import com.idroambiente.giornalieradicantiere.services.CsvGeneratorService
import com.idroambiente.giornalieradicantiere.services.FileStorageService
import com.idroambiente.giornalieradicantiere.services.NetworkUtils
import com.idroambiente.giornalieradicantiere.services.PdfGeneratorService
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
    val webViewLink: String?
)

@HiltViewModel
class RiepilogoCommessaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val commessaRepository: CommessaRepository,
    private val cantiereRepository: CantiereRepository,
    private val giornalieraRepository: GiornalieraRepository,
    private val personaleGiornalieraRepository: PersonaleGiornalieraRepository,
    private val mezzoGiornalieraRepository: MezzoGiornalieraRepository,
    private val pdfGeneratorService: PdfGeneratorService,
    private val csvGeneratorService: CsvGeneratorService,
    private val fileStorageService: FileStorageService,
    private val preferencesRepository: PreferencesRepository,
    private val networkUtils: NetworkUtils
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
            val commessa = _commessa.value ?: return@launch

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

            try {
                val currentGiornaliere = giornaliere.value
                val currentRiepilogo = _riepilogo.value
                val currentStartDate = _startDate.value
                val currentEndDate = _endDate.value

                // Build PDF data
                val pdfData = RiepilogoCommessaPdfData(
                    commessaNome = commessa.nome.toString().padStart(7, '0'),
                    cliente = commessa.cliente,
                    indirizzo = commessa.indirizzo,
                    stato = commessa.stato.name,
                    totaleOreLavoro = currentRiepilogo.totaleOreLavoro,
                    totaleOreViaggio = currentRiepilogo.totaleOreViaggio,
                    totaleKm = currentRiepilogo.totaleKm,
                    giornaliere = currentGiornaliere.map { g ->
                        RiepilogoGiornalieraItem(
                            data = g.data,
                            stato = g.stato.name
                        )
                    },
                    startDate = currentStartDate,
                    endDate = currentEndDate
                )

                // Generate PDF
                val pdfFile = pdfGeneratorService.generateRiepilogoCommessaPdf(pdfData)

                val commessaNome = formatCommessaNome(commessa.nome)
                val fileName = pdfGeneratorService.generateRiepilogoFileName(
                    commessaNome.replace(Regex("[^a-zA-Z0-9\\-]"), "_"),
                    currentStartDate,
                    currentEndDate
                )

                // Generate CSV (best-effort, non bloccante)
                val csvFile = try {
                    csvGeneratorService.generateRiepilogoCommessaCsv(pdfData)
                } catch (_: Exception) {
                    null
                }
                val csvFileName = try {
                    csvGeneratorService.generateRiepilogoFileName(
                        commessaNome.replace(Regex("[^a-zA-Z0-9\\-]"), "_"),
                        currentStartDate,
                        currentEndDate
                    )
                } catch (_: Exception) {
                    null
                }

                // Save
                val saveResult = fileStorageService.saveRiepilogoPdf(pdfFile, commessaNome, fileName)

                when (saveResult) {
                    is SaveResult.Success -> {
                        // Salva anche il CSV (best-effort)
                        if (csvFile != null && csvFileName != null) {
                            try { fileStorageService.saveRiepilogoCsv(csvFile, commessaNome, csvFileName) } catch (_: Exception) {}
                        }
                        _pdfExporting.value = false
                        _pdfExportSuccess.value = PdfExportResult(
                            fileName = saveResult.fileName,
                            folderName = saveResult.folderName,
                            webViewLink = saveResult.webViewLink
                        )
                    }
                    is SaveResult.Error -> {
                        // Fallback: save to app storage
                        val pdfFileFallback = pdfGeneratorService.generateRiepilogoCommessaPdf(pdfData)
                        val localResult = fileStorageService.savePdfToAppStorage(pdfFileFallback)
                        // Salva anche CSV nel fallback (best-effort)
                        if (csvFile != null) {
                            try { fileStorageService.saveCsvToAppStorage(csvFile) } catch (_: Exception) {}
                        }
                        _pdfExporting.value = false
                        when (localResult) {
                            is SaveResult.Success -> {
                                _pdfExportSuccess.value = PdfExportResult(
                                    fileName = localResult.fileName,
                                    folderName = "memoria locale",
                                    webViewLink = null
                                )
                            }
                            is SaveResult.Error -> {
                                _pdfExportError.value = localResult.message
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _pdfExporting.value = false
                _pdfExportError.value = e.message ?: "Errore durante la generazione del PDF"
            }
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
