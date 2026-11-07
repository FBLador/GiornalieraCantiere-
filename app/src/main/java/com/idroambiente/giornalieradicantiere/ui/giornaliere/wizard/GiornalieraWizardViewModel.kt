package com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard

import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idroambiente.giornalieradicantiere.data.preferences.PinPolicy
import com.idroambiente.giornalieradicantiere.data.preferences.PinRepository
import com.idroambiente.giornalieradicantiere.data.preferences.PreferencesRepository
import com.idroambiente.giornalieradicantiere.data.preferences.StorageMode
import com.idroambiente.giornalieradicantiere.data.preferences.ExportBehavior
import com.idroambiente.giornalieradicantiere.database.GiornalieraStore
import com.idroambiente.giornalieradicantiere.database.dao.GiornalieraModificaDao
import com.idroambiente.giornalieradicantiere.database.entities.ArchivioFile
import com.idroambiente.giornalieradicantiere.database.entities.Attrezzatura
import com.idroambiente.giornalieradicantiere.database.entities.AttrezzaturaGiornaliera
import com.idroambiente.giornalieradicantiere.database.entities.Cantiere
import com.idroambiente.giornalieradicantiere.database.entities.Commessa
import com.idroambiente.giornalieradicantiere.database.entities.Giornaliera
import com.idroambiente.giornalieradicantiere.database.entities.GiornalieraModifica
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
import com.idroambiente.giornalieradicantiere.services.EsitoSostituzione
import com.idroambiente.giornalieradicantiere.services.FaseInvio
import com.idroambiente.giornalieradicantiere.services.FileStorageService
import com.idroambiente.giornalieradicantiere.services.GiornalieraExporter
import com.idroambiente.giornalieradicantiere.services.GiornalieraPdfData
import com.idroambiente.giornalieradicantiere.services.InvioEsito
import com.idroambiente.giornalieradicantiere.services.MezzoWithData
import com.idroambiente.giornalieradicantiere.services.NetworkUtils
import com.idroambiente.giornalieradicantiere.services.PdfRiepilogoData
import com.idroambiente.giornalieradicantiere.services.PersonaleWithData
import com.idroambiente.giornalieradicantiere.services.RegistroModifiche
import com.idroambiente.giornalieradicantiere.services.ReinvioEsito
import com.idroambiente.giornalieradicantiere.services.RiepilogoExporter
import com.idroambiente.giornalieradicantiere.services.SaveResult
import com.idroambiente.giornalieradicantiere.services.VoceRegistro
import com.idroambiente.giornalieradicantiere.ui.common.Formati
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
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

data class RiepilogoData(
    val totalePersonePresenti: Int = 0,
    val totaleOreLavoro: Float = 0f,
    val totaleOreViaggio: Float = 0f,
    val totalePernotti: Int = 0,
    val totaleMezziUtilizzati: Int = 0,
    val totaleKm: Float = 0f,
    val totaleAttrezzatureUtilizzate: Int = 0
)

/** Esito del reinvio, mostrato nel foglio "Giornaliera aggiornata". */
data class EsitoAggiornamento(
    val revisione: Int,
    val fileName: String,
    val archivio: ArchivioFile,
    val esitoPdf: EsitoSostituzione?,
    val nomePrecedente: String?,
    val webViewLink: String?,
    val csvErrore: String?,
    val reportAggiornati: List<String>,
    val reportFalliti: List<String>,
    val registro: String?,
    val registroErrore: String?
)

sealed interface WizardDialog {
    data object Esci : WizardDialog
    data object EsciModifica : WizardDialog
    data class Inviata(
        val fileName: String,
        val folderName: String?,
        val webViewLink: String?,
        val memoriaLocale: Boolean,
        val csvErrore: String?,
        val condivisa: Boolean
    ) : WizardDialog
    data class Aggiornata(val esito: EsitoAggiornamento) : WizardDialog
    data class Errore(val titolo: String, val messaggio: String) : WizardDialog
    data class Configurazione(val messaggio: String) : WizardDialog
    data object Offline : WizardDialog
}

/** Stato del tastierino PIN. */
data class PinUiState(val cifre: String = "", val errore: String? = null, val bloccato: Boolean = false)

data class WizardUiState(
    val modalita: ModalitaWizard = ModalitaWizard.NUOVA,
    val isLoading: Boolean = true,
    val giornaliera: Giornaliera? = null,
    val commessa: Commessa? = null,
    val cantiere: Cantiere? = null,
    val currentStep: Int = 0,
    val maxStep: Int = 0,
    val persPhase: Fase = Fase.SELEZIONE,
    val mezPhase: Fase = Fase.SELEZIONE,
    /** Solo le persone presenti */
    val personaleSelections: Map<Long, PersonaleFormData> = emptyMap(),
    /** Solo i mezzi usati */
    val mezzoSelections: Map<Long, MezzoFormData> = emptyMap(),
    /** Solo le attrezzature usate */
    val attrezzaturaSelections: Map<Long, AttrezzaturaFormData> = emptyMap(),
    val form: TurnoForm = TurnoForm(),
    /** Data della giornaliera da cui vengono i valori proposti */
    val formDa: Long? = null,
    val applica: Set<Long> = emptySet(),
    val applicaMezzi: Set<Long> = emptySet(),
    val kmAndata: String = "",
    val kmRitorno: String = "",
    val note: String = "",
    val selectedDate: Long = System.currentTimeMillis(),
    /** Data prima della modifica (solo MODIFICA_INVIATA) */
    val dataOriginale: Long? = null,
    val motivo: String = "",
    val squadraPrecedente: List<Long> = emptyList(),
    val modifiche: List<GiornalieraModifica> = emptyList(),
    val riepilogo: RiepilogoData = RiepilogoData(),
    val fasiInvio: List<FaseInvio> = emptyList(),
    val faseCorrente: FaseInvio? = null,
    val dialog: WizardDialog? = null,
    val pin: PinUiState? = null,
    val toast: String? = null,
    /** Intent di condivisione da aprire (modalità Condividi) */
    val condividi: Intent? = null,
    /** true = tornare alla schermata precedente */
    val chiudi: Boolean = false,
    val archivioDrive: Boolean = true
) {
    val inviando: Boolean get() = faseCorrente != null
    val dataSpostata: Boolean get() = dataOriginale != null && !Formati.stessoGiorno(dataOriginale, selectedDate)
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
    private val fileStorageService: FileStorageService,
    private val preferencesRepository: PreferencesRepository,
    private val networkUtils: NetworkUtils,
    private val giornalieraStore: GiornalieraStore,
    private val giornalieraExporter: GiornalieraExporter,
    private val riepilogoExporter: RiepilogoExporter,
    private val registroModifiche: RegistroModifiche,
    private val pinRepository: PinRepository,
    private val modificaDao: GiornalieraModificaDao
) : ViewModel() {

    val cantiereId: Long = savedStateHandle.get<Long>("cantiereId") ?: 0L
    val giornalieraId: Long? = savedStateHandle.get<Long>("giornalieraId")?.takeIf { it != 0L }

    private val _uiState = MutableStateFlow(WizardUiState())
    val uiState: StateFlow<WizardUiState> = _uiState.asStateFlow()

    // Liste: quelli attivi + quelli già presenti in questa giornaliera (anche se eliminati dopo)
    private val tuttePersone = personaleRepository.getAllIncludingInactive()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val tuttiMezzi = mezzoRepository.getAllIncludingInactive()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val tutteAttrezzature = attrezzaturaRepository.getAllIncludingInactive()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val personaleList: StateFlow<List<Personale>> = combine(tuttePersone, _uiState) { lista, s ->
        lista.filter { it.attivo || it.id in s.personaleSelections }.sortedWith(compareBy({ it.nome }, { it.cognome }))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mezzoList: StateFlow<List<Mezzo>> = combine(tuttiMezzi, _uiState) { lista, s ->
        lista.filter { it.attivo || it.id in s.mezzoSelections }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val attrezzaturaList: StateFlow<List<Attrezzatura>> = combine(tutteAttrezzature, _uiState) { lista, s ->
        lista.filter { it.attivo || it.id in s.attrezzaturaSelections }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch { carica() }
        viewModelScope.launch {
            preferencesRepository.storageMode.collect { m -> _uiState.update { it.copy(archivioDrive = m == StorageMode.GOOGLE_DRIVE) } }
        }
    }

    fun nomePersona(id: Long): String = tuttePersone.value.firstOrNull { it.id == id }?.let { "${it.nome} ${it.cognome}" } ?: "?"
    fun nomeMezzo(id: Long): String = tuttiMezzi.value.firstOrNull { it.id == id }?.nome ?: "?"

    // ───────────────────────── Caricamento ─────────────────────────

    private suspend fun carica() {
        val cantiere = cantiereRepository.getById(cantiereId)
        val commessa = cantiere?.let { commessaRepository.getById(it.commessaId) }
        val g = giornalieraId?.let { giornalieraRepository.getById(it) }

        if (g == null) {
            val adesso = System.currentTimeMillis()
            val prec = giornalieraRepository.getLatestCompiledBefore(cantiereId, adesso, 0)
            val (squadra, form) = daPrecedente(prec)
            _uiState.update {
                it.copy(
                    modalita = ModalitaWizard.NUOVA, isLoading = false, commessa = commessa, cantiere = cantiere,
                    selectedDate = adesso, squadraPrecedente = squadra,
                    form = form ?: TurnoForm(), formDa = if (form != null) prec?.data else null
                )
            }
            return
        }
        caricaGiornaliera(g, commessa, cantiere)
    }

    private suspend fun daPrecedente(prec: Giornaliera?): Pair<List<Long>, TurnoForm?> {
        if (prec == null) return emptyList<Long>() to null
        val righe = personaleGiornalieraRepository.getByGiornalieraId(prec.id).first().filter { it.presente }
        val form = righe.firstOrNull { it.oreLavoro > 0f }?.let { TurnoForm.da(it.toForm()) }
        return righe.map { it.personaleId } to form
    }

    private suspend fun caricaGiornaliera(g: Giornaliera, commessa: Commessa?, cantiere: Cantiere?) {
        val persone = personaleGiornalieraRepository.getByGiornalieraId(g.id).first()
            .filter { it.presente }.associate { it.personaleId to it.toForm() }
        val mezzi = mezzoGiornalieraRepository.getByGiornalieraId(g.id).first()
            .associate { it.mezzoId to MezzoFormData(true, it.kmAndata, it.kmRitorno) }
        val attrezzature = attrezzaturaGiornalieraRepository.getByGiornalieraId(g.id).first()
            .associate { it.attrezzaturaId to AttrezzaturaFormData(true) }
        val modifiche = modificaDao.getByGiornalieraIdSync(g.id)
        val prec = giornalieraRepository.getLatestCompiledBefore(cantiereId, g.data, g.id)
        val (squadra, formPrec) = daPrecedente(prec)
        val bozza = g.stato == StatoGiornaliera.BOZZA
        val formIniziale = persone.values.firstOrNull { it.compilato() }?.let { TurnoForm.da(it) } ?: formPrec ?: TurnoForm()

        _uiState.update {
            it.copy(
                modalita = if (bozza) ModalitaWizard.BOZZA else ModalitaWizard.DETTAGLIO,
                isLoading = false,
                giornaliera = g,
                commessa = commessa,
                cantiere = cantiere,
                currentStep = if (bozza) 0 else 3,
                maxStep = 3,
                persPhase = if (persone.isNotEmpty()) Fase.DATI else Fase.SELEZIONE,
                mezPhase = if (mezzi.isNotEmpty()) Fase.DATI else Fase.SELEZIONE,
                personaleSelections = persone,
                mezzoSelections = mezzi,
                attrezzaturaSelections = attrezzature,
                applica = persone.filterValues { p -> !p.compilato() }.keys,
                applicaMezzi = mezzi.filterValues { m -> !m.compilato() }.keys,
                form = formIniziale,
                formDa = if (persone.values.none { p -> p.compilato() }) prec?.data else null,
                note = g.note ?: "",
                selectedDate = g.data,
                dataOriginale = null,
                motivo = "",
                modifiche = modifiche,
                squadraPrecedente = squadra,
                riepilogo = calcolaRiepilogo(persone, mezzi, attrezzature)
            )
        }
    }

    private fun PersonaleGiornaliera.toForm(): PersonaleFormData {
        val durata = if (inizioTurno > 0 && fineTurno > inizioTurno) (fineTurno - inizioTurno) / 3_600_000f else 0f
        return PersonaleFormData(
            presente = presente,
            oreViaggioAndata = oreViaggioAndata,
            oreViaggioRitorno = oreViaggioRitorno,
            oreLavoro = oreLavoro,
            orePausa = maxOf(0f, durata - oreLavoro),
            inizioTurno = inizioTurno,
            fineTurno = fineTurno,
            pernotto = pernotto
        )
    }

    private fun aggiorna(f: (WizardUiState) -> WizardUiState) {
        _uiState.update { s ->
            val n = f(s)
            n.copy(riepilogo = calcolaRiepilogo(n.personaleSelections, n.mezzoSelections, n.attrezzaturaSelections))
        }
    }

    fun toast(msg: String) = _uiState.update { it.copy(toast = msg) }
    fun toastMostrato() = _uiState.update { it.copy(toast = null) }

    // ───────────────────────── Navigazione tra i passi ─────────────────────────

    fun vaiAStep(n: Int) {
        val s = _uiState.value
        if (s.modalita == ModalitaWizard.DETTAGLIO) return
        if (n > s.maxStep + (if (s.modalita == ModalitaWizard.NUOVA) 0 else 3)) return
        aggiorna { it.copy(currentStep = n, maxStep = maxOf(it.maxStep, n)) }
    }

    // ───────────────────────── Persone ─────────────────────────

    fun togglePresente(id: Long) = aggiorna { s ->
        val p = s.personaleSelections.toMutableMap()
        if (p.containsKey(id)) p.remove(id) else p[id] = PersonaleFormData(presente = true)
        s.copy(personaleSelections = p, applica = s.applica.filter { it in p }.toSet())
    }

    fun impostaPresenti(ids: Collection<Long>) = aggiorna { s ->
        s.copy(personaleSelections = ids.associateWith { s.personaleSelections[it] ?: PersonaleFormData(presente = true) })
    }

    fun squadraPrecedente() {
        val ids = _uiState.value.squadraPrecedente
        if (ids.isEmpty()) return
        impostaPresenti(ids)
        toast("Squadra di ieri: " + ids.joinToString(", ") { nomePersona(it).substringAfter(' ') })
    }

    fun vaiADatiPersonale() {
        val s = _uiState.value
        if (s.personaleSelections.isEmpty()) return toast("Tocca almeno una persona")
        aggiorna { it.copy(persPhase = Fase.DATI, applica = it.personaleSelections.filterValues { p -> !p.compilato() }.keys) }
    }

    fun tornaASelezionePersonale() = aggiorna { it.copy(persPhase = Fase.SELEZIONE) }

    fun impostaValoreForm(campo: String, minuti: Int) = aggiorna { s ->
        s.copy(
            form = when (campo) {
                "inizio" -> s.form.copy(inizio = minuti)
                "fine" -> s.form.copy(fine = minuti)
                "pausa" -> s.form.copy(pausa = minuti)
                "andata" -> s.form.copy(andata = minuti)
                "ritorno" -> s.form.copy(ritorno = minuti)
                else -> s.form
            }
        )
    }

    fun togglePernotto() = aggiorna { it.copy(form = it.form.copy(pernotto = !it.form.pernotto)) }

    fun toggleApplica(id: Long) = aggiorna { s -> s.copy(applica = if (id in s.applica) s.applica - id else s.applica + id) }
    fun applicaATutti() = aggiorna { it.copy(applica = it.personaleSelections.keys) }
    fun applicaANessuno() = aggiorna { it.copy(applica = emptySet()) }

    fun applicaOrari() {
        val s = _uiState.value
        if (s.applica.isEmpty()) return toast("Seleziona a chi applicare i dati")
        s.form.errore()?.let { return toast(it) }
        val n = s.applica.size
        aggiorna { st ->
            val p = st.personaleSelections.toMutableMap()
            st.applica.forEach { p[it] = st.form.toFormData() }
            st.copy(personaleSelections = p, applica = emptySet())
        }
        toast("Dati applicati a $n ${if (n == 1) "persona" else "persone"}")
    }

    fun avantiDaPersonale() {
        val s = _uiState.value
        if (s.personaleSelections.isEmpty()) return toast("Tocca almeno una persona")
        val mancano = s.personaleSelections.filterValues { !it.compilato() }.keys
        if (mancano.isNotEmpty()) return toast("Mancano i dati di " + mancano.joinToString(", ") { nomePersona(it) })
        vaiAStep(1)
    }

    // ───────────────────────── Mezzi ─────────────────────────

    fun toggleMezzo(id: Long) = aggiorna { s ->
        val m = s.mezzoSelections.toMutableMap()
        if (m.containsKey(id)) m.remove(id) else m[id] = MezzoFormData(utilizzato = true)
        s.copy(mezzoSelections = m, applicaMezzi = s.applicaMezzi.filter { it in m }.toSet())
    }

    fun vaiADatiMezzi() {
        val s = _uiState.value
        if (s.mezzoSelections.isEmpty()) return toast("Seleziona almeno un mezzo")
        aggiorna { it.copy(mezPhase = Fase.DATI, applicaMezzi = it.mezzoSelections.filterValues { m -> !m.compilato() }.keys) }
    }

    fun tornaASelezioneMezzi() = aggiorna { it.copy(mezPhase = Fase.SELEZIONE) }
    fun setKmAndata(v: String) = aggiorna { it.copy(kmAndata = v.filter(Char::isDigit).take(4)) }
    fun setKmRitorno(v: String) = aggiorna { it.copy(kmRitorno = v.filter(Char::isDigit).take(4)) }
    fun toggleApplicaMezzo(id: Long) = aggiorna { s -> s.copy(applicaMezzi = if (id in s.applicaMezzi) s.applicaMezzi - id else s.applicaMezzi + id) }
    fun applicaMezziATutti() = aggiorna { it.copy(applicaMezzi = it.mezzoSelections.keys) }
    fun applicaMezziANessuno() = aggiorna { it.copy(applicaMezzi = emptySet()) }

    fun applicaKm() {
        val s = _uiState.value
        if (s.applicaMezzi.isEmpty()) return toast("Seleziona a quali mezzi applicare i km")
        if (s.kmAndata.isEmpty() || s.kmRitorno.isEmpty()) return toast("Inserisci km di andata e ritorno")
        val n = s.applicaMezzi.size
        aggiorna { st ->
            val m = st.mezzoSelections.toMutableMap()
            st.applicaMezzi.forEach { m[it] = MezzoFormData(true, st.kmAndata.toFloat(), st.kmRitorno.toFloat()) }
            st.copy(mezzoSelections = m, applicaMezzi = emptySet())
        }
        toast("Km applicati a $n ${if (n == 1) "mezzo" else "mezzi"}")
    }

    /** Copia i km dall'ultima giornaliera precedente (stessi mezzi). */
    fun usaDatiPrecedenti() {
        viewModelScope.launch {
            val s = _uiState.value
            val prec = giornalieraRepository.getLatestCompiledBefore(cantiereId, s.selectedDate, s.giornaliera?.id ?: 0)
            val righe = prec?.let { mezzoGiornalieraRepository.getByGiornalieraId(it.id).first() }.orEmpty()
            if (prec == null || righe.isEmpty()) return@launch toast("Nessuna giornaliera precedente con mezzi")
            val perMezzo = righe.associateBy { it.mezzoId }
            aggiorna { st ->
                val m = st.mezzoSelections.toMutableMap()
                val ids = if (m.isEmpty()) perMezzo.keys else m.keys
                ids.forEach { id ->
                    val r = perMezzo[id]
                    if (r != null) m[id] = MezzoFormData(true, r.kmAndata, r.kmRitorno) else if (id !in m) m[id] = MezzoFormData(true)
                }
                st.copy(mezzoSelections = m, mezPhase = Fase.DATI, applicaMezzi = m.filterValues { !it.compilato() }.keys)
            }
            toast("Km caricati dalla giornaliera del ${Formati.dataBreve(prec.data)}")
        }
    }

    fun avantiDaMezzi() {
        val s = _uiState.value
        val mancano = s.mezzoSelections.filterValues { !it.compilato() }.keys
        if (s.mezzoSelections.isEmpty()) return toast("Seleziona almeno un mezzo")
        if (mancano.isNotEmpty()) return toast("Mancano i km di " + mancano.joinToString(", ") { nomeMezzo(it) })
        vaiAStep(2)
    }

    // ───────────────────────── Attrezzature e riepilogo ─────────────────────────

    fun toggleAttrezzatura(id: Long) = aggiorna { s ->
        val a = s.attrezzaturaSelections.toMutableMap()
        if (a.containsKey(id)) a.remove(id) else a[id] = AttrezzaturaFormData(true)
        s.copy(attrezzaturaSelections = a)
    }

    fun setNote(t: String) = _uiState.update { it.copy(note = t.take(500)) }
    fun setMotivo(t: String) = _uiState.update { it.copy(motivo = t.take(200)) }

    /** Cambia la data: niente date future né giorni già occupati da un'altra giornaliera della commessa. */
    fun cambiaData(millis: Long) {
        viewModelScope.launch {
            val s = _uiState.value
            val fineOggi = Formati.inizioGiorno(System.currentTimeMillis()) + 86_399_999L
            if (millis > fineOggi) return@launch toast("Non puoi scegliere una data futura")
            if (giornalieraRepository.existsOtherOnDay(cantiereId, millis, s.giornaliera?.id ?: 0)) {
                return@launch toast("Esiste già una giornaliera del ${Formati.dataBreve(millis)} per questa commessa")
            }
            _uiState.update { it.copy(selectedDate = millis) }
        }
    }

    private fun calcolaRiepilogo(
        persone: Map<Long, PersonaleFormData>,
        mezzi: Map<Long, MezzoFormData>,
        attrezzature: Map<Long, AttrezzaturaFormData>
    ): RiepilogoData {
        val p = persone.values.filter { it.presente }
        val m = mezzi.values.filter { it.utilizzato }
        return RiepilogoData(
            totalePersonePresenti = p.size,
            totaleOreLavoro = p.sumOf { it.oreLavoro.toDouble() }.toFloat(),
            totaleOreViaggio = p.sumOf { (it.oreViaggioAndata + it.oreViaggioRitorno).toDouble() }.toFloat(),
            totalePernotti = p.count { it.pernotto },
            totaleMezziUtilizzati = m.size,
            totaleKm = m.sumOf { ((it.kmAndata ?: 0f) + (it.kmRitorno ?: 0f)).toDouble() }.toFloat(),
            totaleAttrezzatureUtilizzate = attrezzature.values.count { it.utilizzata }
        )
    }

    // ───────────────────────── Uscita ─────────────────────────

    fun esci() {
        val s = _uiState.value
        _uiState.update {
            when (s.modalita) {
                ModalitaWizard.DETTAGLIO -> it.copy(chiudi = true)
                ModalitaWizard.MODIFICA_INVIATA -> it.copy(dialog = WizardDialog.EsciModifica)
                else -> it.copy(dialog = WizardDialog.Esci)
            }
        }
    }

    fun chiudiDialog() = _uiState.update { it.copy(dialog = null) }

    fun fatto() = _uiState.update { it.copy(dialog = null, chiudi = true) }

    fun salvaBozza() {
        viewModelScope.launch {
            val s = _uiState.value
            val base = s.giornaliera ?: Giornaliera(cantiereId = cantiereId, data = s.selectedDate, stato = StatoGiornaliera.BOZZA)
            if (s.giornaliera == null && giornalieraRepository.existsOtherOnDay(cantiereId, s.selectedDate, 0)) {
                _uiState.update { it.copy(dialog = WizardDialog.Errore("Bozza non salvata", "Esiste già una giornaliera per questa commessa in questa data")) }
                return@launch
            }
            giornalieraStore.salva(
                base.copy(data = s.selectedDate, note = s.note.ifBlank { null }, updatedAt = System.currentTimeMillis()),
                righe(s)
            )
            _uiState.update { it.copy(dialog = null, chiudi = true, toast = "Bozza salvata") }
        }
    }

    fun scarta() = _uiState.update { it.copy(dialog = null, chiudi = true) }

    /** Esce dalla modifica: la giornaliera inviata resta com'era. */
    fun annullaModifiche() {
        viewModelScope.launch {
            val g = _uiState.value.giornaliera?.id?.let { giornalieraRepository.getById(it) } ?: return@launch
            val s = _uiState.value
            caricaGiornaliera(g, s.commessa, s.cantiere)
            _uiState.update { it.copy(dialog = null) }
        }
    }

    // ───────────────────────── Modifica e PIN ─────────────────────────

    fun richiediModifica() {
        val g = _uiState.value.giornaliera ?: return
        if (g.stato != StatoGiornaliera.INVIATA) {
            _uiState.update { it.copy(modalita = ModalitaWizard.MODIFICA, currentStep = 3, maxStep = 3) }
            return
        }
        viewModelScope.launch {
            val blocco = pinRepository.bloccoRimanente()
            _uiState.update { it.copy(pin = PinUiState(errore = if (blocco > 0) messaggioBlocco(blocco) else null, bloccato = blocco > 0)) }
        }
    }

    fun pinCifra(c: Char) {
        val p = _uiState.value.pin ?: return
        if (p.bloccato || p.cifre.length >= 6) return
        val cifre = p.cifre + c
        _uiState.update { it.copy(pin = p.copy(cifre = cifre, errore = null)) }
        if (cifre.length == 6) {
            viewModelScope.launch {
                when (val esito = pinRepository.verifica(cifre)) {
                    PinPolicy.Esito.Corretto -> sbloccaModifica()
                    is PinPolicy.Esito.Errato -> _uiState.update {
                        it.copy(pin = PinUiState(errore = "PIN errato · ${esito.tentativiRimasti} ${if (esito.tentativiRimasti == 1) "tentativo rimasto" else "tentativi rimasti"}"))
                    }
                    is PinPolicy.Esito.Bloccato -> _uiState.update {
                        it.copy(pin = PinUiState(errore = messaggioBlocco(esito.millisRimanenti), bloccato = true))
                    }
                }
            }
        }
    }

    fun pinCancella() = _uiState.update { s -> s.copy(pin = s.pin?.copy(cifre = s.pin.cifre.dropLast(1))) }
    fun pinAnnulla() = _uiState.update { it.copy(pin = null) }

    private fun messaggioBlocco(ms: Long): String {
        val min = ((ms + 59_999) / 60_000).coerceAtLeast(1)
        return "Troppi tentativi. Riprova tra $min ${if (min == 1L) "minuto" else "minuti"}."
    }

    private fun sbloccaModifica() {
        _uiState.update {
            it.copy(
                pin = null,
                modalita = ModalitaWizard.MODIFICA_INVIATA,
                currentStep = 3,
                maxStep = 3,
                dataOriginale = it.giornaliera?.data,
                motivo = ""
            )
        }
    }

    // ───────────────────────── Invio ─────────────────────────

    fun invia() {
        val s = _uiState.value
        if (s.inviando) return
        problemaInvio(s.personaleSelections, s.mezzoSelections, ::nomePersona, ::nomeMezzo, s.modalita == ModalitaWizard.MODIFICA_INVIATA, s.motivo)?.let {
            aggiorna { st -> st.copy(currentStep = it.step) }
            return toast(it.messaggio)
        }
        viewModelScope.launch {
            erroreConfigurazione()?.let { msg ->
                _uiState.update { it.copy(dialog = WizardDialog.Configurazione(msg)) }
                return@launch
            }
            if (preferencesRepository.storageMode.first() == StorageMode.GOOGLE_DRIVE && !networkUtils.isOnline()) {
                _uiState.update { it.copy(dialog = WizardDialog.Offline) }
                return@launch
            }
            try {
                if (s.modalita == ModalitaWizard.MODIFICA_INVIATA) reinvia() else primoInvio()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(faseCorrente = null, dialog = WizardDialog.Errore("Invio non riuscito", e.message ?: "Errore sconosciuto"))
                }
            }
        }
    }

    private fun fase(f: FaseInvio) = _uiState.update { it.copy(faseCorrente = f) }

    private suspend fun primoInvio() {
        val s = _uiState.value
        if (s.giornaliera == null && giornalieraRepository.existsOtherOnDay(cantiereId, s.selectedDate, 0)) {
            _uiState.update { it.copy(dialog = WizardDialog.Errore("Giornaliera già presente", "Esiste già una giornaliera per questa commessa in questa data")) }
            return
        }
        _uiState.update {
            it.copy(
                fasiInvio = listOf(
                    FaseInvio.PREPARAZIONE, FaseInvio.GENERAZIONE_PDF, FaseInvio.GENERAZIONE_CSV,
                    FaseInvio.CARICAMENTO_PDF, FaseInvio.CARICAMENTO_CSV, FaseInvio.FINALIZZAZIONE
                ),
                faseCorrente = FaseInvio.PREPARAZIONE
            )
        }
        val base = s.giornaliera ?: Giornaliera(cantiereId = cantiereId, data = s.selectedDate, stato = StatoGiornaliera.BOZZA)
        val id = giornalieraStore.salva(
            base.copy(
                data = s.selectedDate,
                note = s.note.ifBlank { null },
                presaVisionePOS = true,
                updatedAt = System.currentTimeMillis()
            ),
            righe(s)
        )
        val salvata = giornalieraRepository.getById(id) ?: error("Giornaliera appena salvata non trovata")
        _uiState.update { it.copy(giornaliera = salvata) }

        when (val esito = giornalieraExporter.invia(datiPdf(salvata, s)) { fase(it) }) {
            is InvioEsito.Salvato -> {
                fase(FaseInvio.FINALIZZAZIONE)
                segnaInviata(salvata, esito.pdf, esito.csv)
                _uiState.update {
                    it.copy(
                        faseCorrente = null,
                        dialog = WizardDialog.Inviata(
                            esito.pdf.fileName, esito.pdf.folderName, esito.pdf.webViewLink,
                            esito.memoriaLocale, esito.csvErrore, condivisa = false
                        )
                    )
                }
            }
            is InvioEsito.DaCondividere -> {
                fase(FaseInvio.FINALIZZAZIONE)
                segnaInviata(salvata, esito.salvato, esito.csv, condivisa = esito.salvato == null)
                _uiState.update {
                    it.copy(
                        faseCorrente = null,
                        condividi = esito.shareIntent,
                        dialog = WizardDialog.Inviata(
                            esito.salvato?.fileName ?: esito.pdfFile.name, esito.salvato?.folderName,
                            esito.salvato?.webViewLink, false, null, condivisa = true
                        )
                    )
                }
            }
            is InvioEsito.Errore -> _uiState.update {
                it.copy(faseCorrente = null, dialog = WizardDialog.Errore("Invio non riuscito", esito.messaggio + "\nLa giornaliera è salvata come bozza."))
            }
        }
    }

    private suspend fun segnaInviata(g: Giornaliera, pdf: SaveResult.Success?, csv: SaveResult.Success?, condivisa: Boolean = false) {
        val aggiornata = g.copy(
            stato = StatoGiornaliera.INVIATA,
            updatedAt = System.currentTimeMillis(),
            fileArchivio = (pdf?.archivio ?: if (condivisa) ArchivioFile.CONDIVISO else null)?.name,
            pdfFileRef = pdf?.ref,
            pdfFileName = pdf?.fileName,
            csvFileRef = csv?.ref,
            csvFileName = csv?.fileName
        )
        giornalieraRepository.update(aggiornata)
        _uiState.update { it.copy(giornaliera = aggiornata) }
    }

    /**
     * Reinvio dopo una modifica. Ordine sicuro:
     * 1. genera e SOSTITUISCE PDF e CSV (se fallisce: il database non cambia)
     * 2. salva dati, revisione e storico in una transazione
     * 3. aggiorna i report di questo telefono e scrive il registro modifiche
     */
    private suspend fun reinvia() {
        val s = _uiState.value
        val originale = s.giornaliera ?: return
        val adesso = System.currentTimeMillis()
        val revisione = originale.revisione + 1
        val nuova = originale.copy(
            data = s.selectedDate,
            note = s.note.ifBlank { null },
            revisione = revisione,
            modificataAt = adesso,
            updatedAt = adesso
        )
        _uiState.update {
            it.copy(
                fasiInvio = listOf(
                    FaseInvio.GENERAZIONE_PDF, FaseInvio.GENERAZIONE_CSV, FaseInvio.SOSTITUZIONE_PDF,
                    FaseInvio.SOSTITUZIONE_CSV, FaseInvio.AGGIORNAMENTO_REPORT, FaseInvio.REGISTRO
                ),
                faseCorrente = FaseInvio.GENERAZIONE_PDF
            )
        }

        val esito = giornalieraExporter.reinvia(datiPdf(nuova, s).copy(motivoModifica = s.motivo.trim()), originale) { fase(it) }
        if (esito is ReinvioEsito.Errore) {
            _uiState.update {
                it.copy(
                    faseCorrente = null,
                    dialog = WizardDialog.Errore(
                        "Reinvio non riuscito",
                        "${esito.messaggio}\nLa giornaliera inviata resta com'era, anche su Drive. Riprova."
                    )
                )
            }
            return
        }
        esito as ReinvioEsito.Ok

        val aggiornata = nuova.copy(
            fileArchivio = esito.archivio.name,
            pdfFileRef = esito.pdf.ref,
            pdfFileName = esito.pdf.nome,
            csvFileRef = esito.csv?.ref ?: originale.csvFileRef,
            csvFileName = esito.csv?.nome ?: originale.csvFileName
        )
        val spostata = !Formati.stessoGiorno(originale.data, s.selectedDate)
        giornalieraStore.applicaRevisione(
            aggiornata,
            righe(s),
            GiornalieraModifica(
                giornalieraId = originale.id,
                revisione = revisione,
                modificataAt = adesso,
                motivo = s.motivo.trim(),
                dataPrecedente = if (spostata) originale.data else null,
                dataNuova = s.selectedDate
            )
        )

        fase(FaseInvio.AGGIORNAMENTO_REPORT)
        val commessaId = s.commessa?.id
        val report = if (commessaId != null) {
            runCatching { riepilogoExporter.aggiornaReportCheContengono(commessaId, setOf(originale.data, s.selectedDate)) }.getOrNull()
        } else null

        fase(FaseInvio.REGISTRO)
        val registro = s.commessa?.let { c ->
            registroModifiche.scrivi(
                VoceRegistro(
                    modificataAt = adesso,
                    commessaNome = c.nome,
                    cliente = c.cliente,
                    dataOriginale = originale.data,
                    dataNuova = s.selectedDate,
                    revisione = revisione,
                    motivo = s.motivo.trim(),
                    filePdf = esito.pdf.nome,
                    fileCsv = esito.csv?.nome,
                    reportAggiornati = report?.aggiornati.orEmpty(),
                    reportNonAggiornati = report?.falliti.orEmpty()
                )
            )
        }

        val ricaricata = giornalieraRepository.getById(originale.id) ?: aggiornata
        caricaGiornaliera(ricaricata, s.commessa, s.cantiere)
        _uiState.update {
            it.copy(
                faseCorrente = null,
                condividi = esito.shareIntent,
                dialog = WizardDialog.Aggiornata(
                    EsitoAggiornamento(
                        revisione = revisione,
                        fileName = esito.pdf.nome,
                        archivio = esito.archivio,
                        esitoPdf = esito.pdf.esito,
                        nomePrecedente = esito.pdf.nomePrecedente,
                        webViewLink = esito.pdf.webViewLink,
                        csvErrore = esito.csvErrore,
                        reportAggiornati = report?.aggiornati.orEmpty(),
                        reportFalliti = report?.falliti.orEmpty() + if (report == null && commessaId != null) listOf("report") else emptyList(),
                        registro = registro?.getOrNull(),
                        registroErrore = registro?.exceptionOrNull()?.message
                    )
                )
            )
        }
    }

    fun condivisioneAvviata() = _uiState.update { it.copy(condividi = null) }

    private suspend fun erroreConfigurazione(): String? {
        return when (preferencesRepository.storageMode.first()) {
            StorageMode.GOOGLE_DRIVE -> {
                val root = preferencesRepository.googleDriveRootFolderId.first()
                val email = preferencesRepository.googleUserEmail.first()
                when {
                    email.isNullOrEmpty() -> "Account Google non connesso. Vai alle impostazioni per collegare il tuo account."
                    root.isNullOrEmpty() -> "Cartella Google Drive non selezionata. Vai alle impostazioni per sceglierne una."
                    else -> null
                }
            }
            StorageMode.LOCAL -> {
                if (preferencesRepository.exportBehavior.first() == ExportBehavior.SHARE) return null
                val folderUri = preferencesRepository.exportFolderUri.first()
                when {
                    folderUri.isNullOrEmpty() -> "Cartella di salvataggio non configurata. Vai alle impostazioni per selezionarne una."
                    !fileStorageService.isConfiguredFolderAccessible() -> "La cartella di salvataggio non è più accessibile. Vai alle impostazioni per selezionarne una nuova."
                    else -> null
                }
            }
        }
    }

    // ───────────────────────── Dati per salvataggio e PDF ─────────────────────────

    private fun righe(s: WizardUiState) = GiornalieraStore.Righe(
        personale = s.personaleSelections.filterValues { it.presente }.map { (id, d) ->
            PersonaleGiornaliera(
                giornalieraId = 0,
                personaleId = id,
                presente = true,
                oreViaggioAndata = maxOf(0f, d.oreViaggioAndata),
                oreViaggioRitorno = maxOf(0f, d.oreViaggioRitorno),
                oreLavoro = maxOf(0f, d.oreLavoro),
                inizioTurno = d.inizioTurno,
                fineTurno = d.fineTurno,
                pernotto = d.pernotto
            )
        },
        mezzi = s.mezzoSelections.filterValues { it.utilizzato }.map { (id, d) ->
            MezzoGiornaliera(giornalieraId = 0, mezzoId = id, kmAndata = maxOf(0f, d.kmAndata ?: 0f), kmRitorno = maxOf(0f, d.kmRitorno ?: 0f))
        },
        attrezzature = s.attrezzaturaSelections.filterValues { it.utilizzata }.keys.map { AttrezzaturaGiornaliera(0, it) }
    )

    private suspend fun datiPdf(g: Giornaliera, s: WizardUiState): GiornalieraPdfData {
        val cantiere = s.cantiere ?: cantiereRepository.getById(g.cantiereId) ?: error("Cantiere non trovato")
        val commessa = s.commessa ?: commessaRepository.getById(cantiere.commessaId) ?: error("Commessa non trovata")
        val persone = personaleRepository.getByIds(s.personaleSelections.keys.toList()).associateBy { it.id }
        val mezzi = mezzoRepository.getByIds(s.mezzoSelections.keys.toList()).associateBy { it.id }
        val attrezzature = attrezzaturaRepository.getByIds(s.attrezzaturaSelections.keys.toList())
        val r = s.riepilogo
        return GiornalieraPdfData(
            giornaliera = g,
            cantiere = cantiere,
            commessa = commessa,
            personaleList = s.personaleSelections.filterValues { it.presente }.mapNotNull { (id, d) ->
                persone[id]?.let {
                    PersonaleWithData(
                        it,
                        PersonaleGiornaliera(
                            giornalieraId = g.id, personaleId = id, presente = true,
                            oreViaggioAndata = d.oreViaggioAndata, oreViaggioRitorno = d.oreViaggioRitorno,
                            oreLavoro = d.oreLavoro, inizioTurno = d.inizioTurno, fineTurno = d.fineTurno, pernotto = d.pernotto
                        )
                    )
                }
            }.sortedWith(compareBy({ it.personale.cognome }, { it.personale.nome })),
            mezziList = s.mezzoSelections.filterValues { it.utilizzato }.mapNotNull { (id, d) ->
                mezzi[id]?.let { MezzoWithData(it, MezzoGiornaliera(giornalieraId = g.id, mezzoId = id, kmAndata = d.kmAndata ?: 0f, kmRitorno = d.kmRitorno ?: 0f)) }
            },
            attrezzatureList = attrezzature,
            riepilogo = PdfRiepilogoData(
                totalePersonePresenti = r.totalePersonePresenti,
                totaleOreLavoro = r.totaleOreLavoro,
                totaleOreViaggio = r.totaleOreViaggio,
                totalePernotti = r.totalePernotti,
                totaleMezziUtilizzati = r.totaleMezziUtilizzati,
                totaleKm = r.totaleKm,
                totaleAttrezzatureUtilizzate = r.totaleAttrezzatureUtilizzate
            ),
            userName = preferencesRepository.googleUserDisplayName.first()
        )
    }
}

/** Converte la data scelta nel DatePicker (mezzanotte UTC) in mezzogiorno locale dello stesso giorno. */
fun dataDaPicker(utcMillis: Long): Long {
    val utc = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
    return Calendar.getInstance().apply {
        clear()
        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), 12, 0)
    }.timeInMillis
}

/** Il contrario: data locale → mezzanotte UTC per inizializzare il DatePicker. */
fun dataPerPicker(localMillis: Long): Long {
    val loc = Calendar.getInstance().apply { timeInMillis = localMillis }
    return Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(loc.get(Calendar.YEAR), loc.get(Calendar.MONTH), loc.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}
