package com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.East
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.RemoveDone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.West
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.idroambiente.giornalieradicantiere.database.entities.ArchivioFile
import com.idroambiente.giornalieradicantiere.database.entities.Attrezzatura
import com.idroambiente.giornalieradicantiere.database.entities.Mezzo
import com.idroambiente.giornalieradicantiere.database.entities.Personale
import com.idroambiente.giornalieradicantiere.database.entities.StatoGiornaliera
import com.idroambiente.giornalieradicantiere.services.EsitoSostituzione
import com.idroambiente.giornalieradicantiere.services.FaseInvio
import com.idroambiente.giornalieradicantiere.ui.common.Formati
import com.idroambiente.giornalieradicantiere.ui.components.AppPanel
import com.idroambiente.giornalieradicantiere.ui.components.AppSheet
import com.idroambiente.giornalieradicantiere.ui.components.ApplyRow
import com.idroambiente.giornalieradicantiere.ui.components.BigButton
import com.idroambiente.giornalieradicantiere.ui.components.BottomBar
import com.idroambiente.giornalieradicantiere.ui.components.FileBox
import com.idroambiente.giornalieradicantiere.ui.components.InfoBox
import com.idroambiente.giornalieradicantiere.ui.components.InfoKind
import com.idroambiente.giornalieradicantiere.ui.components.OutlineButton
import com.idroambiente.giornalieradicantiere.ui.components.PinSheetContent
import com.idroambiente.giornalieradicantiere.ui.components.ProgressOverlay
import com.idroambiente.giornalieradicantiere.ui.components.SectionHeaderRow
import com.idroambiente.giornalieradicantiere.ui.components.SectionLabel
import com.idroambiente.giornalieradicantiere.ui.components.SelectTile
import com.idroambiente.giornalieradicantiere.ui.components.SheetButton
import com.idroambiente.giornalieradicantiere.ui.components.SheetButtonKind
import com.idroambiente.giornalieradicantiere.ui.components.SheetIcon
import com.idroambiente.giornalieradicantiere.ui.components.SheetTitle
import com.idroambiente.giornalieradicantiere.ui.components.SwitchRow
import com.idroambiente.giornalieradicantiere.ui.components.TextArea
import com.idroambiente.giornalieradicantiere.ui.components.TextLink
import com.idroambiente.giornalieradicantiere.ui.components.ToastHost
import com.idroambiente.giornalieradicantiere.ui.components.ToastState
import com.idroambiente.giornalieradicantiere.ui.components.ValueTile
import com.idroambiente.giornalieradicantiere.ui.components.WheelPickerContent
import com.idroambiente.giornalieradicantiere.ui.components.cardShadow
import com.idroambiente.giornalieradicantiere.ui.components.formatDurata
import com.idroambiente.giornalieradicantiere.ui.components.rememberToastState
import com.idroambiente.giornalieradicantiere.ui.theme.AppText
import com.idroambiente.giornalieradicantiere.ui.theme.AppTheme
import com.idroambiente.giornalieradicantiere.util.formatCommessaNome

/** Tutte le azioni del wizard (il ViewModel nell'app, nulla nelle anteprime/screenshot). */
data class WizardAzioni(
    val esci: () -> Unit = {},
    val vaiAStep: (Int) -> Unit = {},
    val togglePresente: (Long) -> Unit = {},
    val tutti: (Boolean) -> Unit = {},
    val squadraPrecedente: () -> Unit = {},
    val vaiADatiPersonale: () -> Unit = {},
    val tornaASelezionePersonale: () -> Unit = {},
    val valoreForm: (String, Int) -> Unit = { _, _ -> },
    val togglePernotto: () -> Unit = {},
    val toggleApplica: (Long) -> Unit = {},
    val applicaTutti: (Boolean) -> Unit = {},
    val applicaOrari: () -> Unit = {},
    val avantiDaPersonale: () -> Unit = {},
    val toggleMezzo: (Long) -> Unit = {},
    val vaiADatiMezzi: () -> Unit = {},
    val tornaASelezioneMezzi: () -> Unit = {},
    val kmAndata: (String) -> Unit = {},
    val kmRitorno: (String) -> Unit = {},
    val toggleApplicaMezzo: (Long) -> Unit = {},
    val applicaMezziTutti: (Boolean) -> Unit = {},
    val applicaKm: () -> Unit = {},
    val usaDatiPrecedenti: () -> Unit = {},
    val avantiDaMezzi: () -> Unit = {},
    val toggleAttrezzatura: (Long) -> Unit = {},
    val avantiARiepilogo: () -> Unit = {},
    val note: (String) -> Unit = {},
    val motivo: (String) -> Unit = {},
    val cambiaData: (Long) -> Unit = {},
    val invia: () -> Unit = {},
    val richiediModifica: () -> Unit = {},
    val pinCifra: (Char) -> Unit = {},
    val pinCancella: () -> Unit = {},
    val pinAnnulla: () -> Unit = {},
    val chiudiDialog: () -> Unit = {},
    val fatto: () -> Unit = {},
    val salvaBozza: () -> Unit = {},
    val scarta: () -> Unit = {},
    val annullaModifiche: () -> Unit = {},
    val apriLink: (String) -> Unit = {},
    val toast: (String) -> Unit = {}
)

@Composable
fun GiornalieraWizardScreen(
    viewModel: GiornalieraWizardViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val persone by viewModel.personaleList.collectAsState()
    val mezzi by viewModel.mezzoList.collectAsState()
    val attrezzature by viewModel.attrezzaturaList.collectAsState()
    val context = LocalContext.current
    val toast = rememberToastState()

    LaunchedEffect(state.chiudi) { if (state.chiudi) onNavigateBack() }
    // Indietro di sistema: in compilazione/modifica chiede conferma (i fogli aperti hanno la precedenza)
    BackHandler(enabled = !state.inviando) { viewModel.esci() }
    LaunchedEffect(state.toast) { state.toast?.let { toast.show(it); viewModel.toastMostrato() } }
    LaunchedEffect(state.condividi) {
        state.condividi?.let { runCatching { context.startActivity(it) }; viewModel.condivisioneAvviata() }
    }

    val vm = viewModel
    WizardContent(
        state = state,
        persone = persone,
        mezzi = mezzi,
        attrezzature = attrezzature,
        toastState = toast,
        nomePersona = vm::nomePersona,
        nomeMezzo = vm::nomeMezzo,
        azioni = WizardAzioni(
            esci = vm::esci,
            vaiAStep = vm::vaiAStep,
            togglePresente = vm::togglePresente,
            tutti = { all -> vm.impostaPresenti(if (all) persone.filter { it.attivo }.map { it.id } else emptyList()) },
            squadraPrecedente = vm::squadraPrecedente,
            vaiADatiPersonale = vm::vaiADatiPersonale,
            tornaASelezionePersonale = vm::tornaASelezionePersonale,
            valoreForm = vm::impostaValoreForm,
            togglePernotto = vm::togglePernotto,
            toggleApplica = vm::toggleApplica,
            applicaTutti = { if (it) vm.applicaATutti() else vm.applicaANessuno() },
            applicaOrari = vm::applicaOrari,
            avantiDaPersonale = vm::avantiDaPersonale,
            toggleMezzo = vm::toggleMezzo,
            vaiADatiMezzi = vm::vaiADatiMezzi,
            tornaASelezioneMezzi = vm::tornaASelezioneMezzi,
            kmAndata = vm::setKmAndata,
            kmRitorno = vm::setKmRitorno,
            toggleApplicaMezzo = vm::toggleApplicaMezzo,
            applicaMezziTutti = { if (it) vm.applicaMezziATutti() else vm.applicaMezziANessuno() },
            applicaKm = vm::applicaKm,
            usaDatiPrecedenti = vm::usaDatiPrecedenti,
            avantiDaMezzi = vm::avantiDaMezzi,
            toggleAttrezzatura = vm::toggleAttrezzatura,
            avantiARiepilogo = vm::avantiARiepilogo,
            note = vm::setNote,
            motivo = vm::setMotivo,
            cambiaData = vm::cambiaData,
            invia = vm::invia,
            richiediModifica = vm::richiediModifica,
            pinCifra = vm::pinCifra,
            pinCancella = vm::pinCancella,
            pinAnnulla = vm::pinAnnulla,
            chiudiDialog = vm::chiudiDialog,
            fatto = vm::fatto,
            salvaBozza = vm::salvaBozza,
            scarta = vm::scarta,
            annullaModifiche = vm::annullaModifiche,
            apriLink = { link -> runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) } },
            toast = vm::toast
        )
    )
}

private data class Scelta(val campo: String, val label: String, val isTime: Boolean, val minuti: Int)

fun etichettaFase(f: FaseInvio, drive: Boolean): String = when (f) {
    FaseInvio.PREPARAZIONE -> "Preparazione invio…"
    FaseInvio.GENERAZIONE_PDF -> "Generazione PDF…"
    FaseInvio.GENERAZIONE_CSV -> "Preparazione CSV…"
    FaseInvio.CARICAMENTO_PDF -> "Caricamento PDF…"
    FaseInvio.CARICAMENTO_CSV -> "Caricamento CSV…"
    FaseInvio.SOSTITUZIONE_PDF -> if (drive) "Sostituzione PDF su Drive…" else "Sovrascrittura PDF…"
    FaseInvio.SOSTITUZIONE_CSV -> if (drive) "Sostituzione CSV su Drive…" else "Sovrascrittura CSV…"
    FaseInvio.AGGIORNAMENTO_REPORT -> "Aggiornamento report…"
    FaseInvio.REGISTRO -> "Registro modifiche…"
    FaseInvio.FINALIZZAZIONE -> "Finalizzazione…"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WizardContent(
    state: WizardUiState,
    persone: List<Personale>,
    mezzi: List<Mezzo>,
    attrezzature: List<Attrezzatura>,
    toastState: ToastState,
    nomePersona: (Long) -> String,
    nomeMezzo: (Long) -> String,
    azioni: WizardAzioni,
    sceltaIniziale: Triple<String, String, Boolean>? = null
) {
    val c = AppTheme.colors
    var scelta by remember {
        mutableStateOf(sceltaIniziale?.let { (campo, label, isTime) -> Scelta(campo, label, isTime, valoreCampo(state.form, campo)) })
    }
    var sceltaData by remember { mutableStateOf(false) }
    val barra = barraWizard(state, nomePersona, nomeMezzo)

    Box(Modifier.fillMaxSize().background(c.bg)) {
        Column(Modifier.fillMaxSize()) {
            WizardHeader(state, azioni)
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when {
                    state.isLoading -> Unit
                    state.currentStep == 0 && state.persPhase == Fase.SELEZIONE -> PersoneSelezione(state, persone, azioni)
                    state.currentStep == 0 -> PersoneDati(state, nomePersona, azioni) { campo, label, isTime ->
                        scelta = Scelta(campo, label, isTime, valoreCampo(state.form, campo))
                    }
                    state.currentStep == 1 && state.mezPhase == Fase.SELEZIONE -> MezziSelezione(state, mezzi, azioni)
                    state.currentStep == 1 -> MezziDati(state, mezzi, azioni)
                    state.currentStep == 2 -> AttrezzatureStep(state, attrezzature, azioni)
                    else -> RiepilogoStep(state, persone, mezzi, attrezzature, nomePersona, azioni) { sceltaData = true }
                }
            }
            if (barra.applica != null) {
                Box(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 10.dp)) {
                    BigButton(
                        text = barra.applica,
                        onClick = { if (state.currentStep == 0) azioni.applicaOrari() else azioni.applicaKm() },
                        icon = Icons.Default.DoneAll,
                        container = c.yel,
                        content = c.onYel,
                        height = 62.dp,
                        textStyle = AppText.buttonLarge,
                        modifier = Modifier.cardShadow(RoundedCornerShape(12.dp), false)
                    )
                }
            }
            WizardBottomBar(barra, azioni)
        }

        ToastHost(toastState)
        ProgressOverlay(
            visible = state.inviando,
            label = state.faseCorrente?.let { etichettaFase(it, state.archivioDrive) } ?: "",
            progress = state.faseCorrente?.let { (state.fasiInvio.indexOf(it) + 1f) / state.fasiInvio.size.coerceAtLeast(1) } ?: 0f
        )
        WizardSheets(state, azioni)
        AppSheet(visible = state.pin != null, onDismiss = azioni.pinAnnulla) {
            val pin = state.pin ?: PinUiState()
            PinSheetContent(
                title = "Modifica giornaliera inviata",
                subtitle = "Inserisci il PIN del responsabile",
                icon = Icons.Default.Lock,
                digits = pin.cifre.length,
                error = pin.errore,
                locked = pin.bloccato,
                onDigit = azioni.pinCifra,
                onDelete = azioni.pinCancella,
                onCancel = azioni.pinAnnulla
            )
        }
        AppSheet(visible = scelta != null, onDismiss = { scelta = null }) {
            val s = scelta ?: return@AppSheet
            WheelPickerContent(
                label = s.label,
                isTime = s.isTime,
                initialMinutes = s.minuti,
                onConfirm = { azioni.valoreForm(s.campo, it); scelta = null },
                onCancel = { scelta = null }
            )
        }
    }

    if (sceltaData) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = dataPerPicker(state.selectedDate))
        DatePickerDialog(
            onDismissRequest = { sceltaData = false },
            confirmButton = {
                TextButton(onClick = {
                    sceltaData = false
                    picker.selectedDateMillis?.let { azioni.cambiaData(dataDaPicker(it)) }
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { sceltaData = false }) { Text("Annulla") } }
        ) { DatePicker(state = picker) }
    }
}

private fun valoreCampo(f: TurnoForm, campo: String) = when (campo) {
    "inizio" -> f.inizio
    "fine" -> f.fine
    "pausa" -> f.pausa
    "andata" -> f.andata
    else -> f.ritorno
}

// ───────────────────────── Intestazione con schede ─────────────────────────

@Composable
private fun WizardHeader(state: WizardUiState, azioni: WizardAzioni) {
    val c = AppTheme.colors
    val codice = state.commessa?.let { formatCommessaNome(it.nome) } ?: ""
    val passo = "passo ${state.currentStep + 1} di 4"
    val (titolo, sotto) = when (state.modalita) {
        ModalitaWizard.DETTAGLIO -> "Giornaliera del ${Formati.dataBreve(state.selectedDate)}" to
            listOfNotNull(codice, state.giornaliera?.stato?.name, state.giornaliera?.revisione?.takeIf { it > 0 }?.let { "rev. $it" }).joinToString(" · ")
        ModalitaWizard.MODIFICA_INVIATA -> "Giornaliera del ${Formati.dataBreve(state.selectedDate)}" to "$codice · modifica · $passo"
        ModalitaWizard.NUOVA -> "$codice · ${state.commessa?.cliente ?: ""}" to "Giornaliera di oggi · $passo"
        ModalitaWizard.BOZZA -> "$codice · ${state.commessa?.cliente ?: ""}" to "Bozza · $passo"
        ModalitaWizard.MODIFICA -> "$codice · ${state.commessa?.cliente ?: ""}" to "Modifica · $passo"
    }
    val dettaglio = state.modalita == ModalitaWizard.DETTAGLIO
    Column(Modifier.fillMaxWidth().background(c.hdr).statusBarsPadding()) {
        Row(Modifier.padding(start = 4.dp, end = 12.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button, onClick = azioni.esci)
                    .semantics { contentDescription = if (dettaglio) "Indietro" else "Chiudi" },
                contentAlignment = Alignment.Center
            ) {
                Icon(if (dettaglio) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Close, null, tint = c.onHdr, modifier = Modifier.size(28.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(titolo, style = AppText.headerSmall, color = c.onHdr, maxLines = 1)
                Text(sotto, style = AppText.headerSub, color = c.hdrSub, maxLines = 1)
            }
        }
        if (!dettaglio) {
            val tabs = listOf("Persone" to Icons.Default.Groups, "Mezzi" to Icons.Default.LocalShipping, "Attrezz." to Icons.Default.Construction, "Riepilogo" to Icons.Default.FactCheck)
            val presenti = state.personaleSelections.values
            val usati = state.mezzoSelections.values
            val fatto = listOf(
                presenti.isNotEmpty() && presenti.all { it.compilato() },
                state.maxStep > 1 && usati.isNotEmpty() && usati.all { it.compilato() },
                state.maxStep > 2,
                false
            )
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                tabs.forEachIndexed { i, (label, icon) ->
                    val attiva = i == state.currentStep
                    val abilitata = state.modalita != ModalitaWizard.NUOVA || i <= state.maxStep
                    val fg = when {
                        attiva -> c.hdr
                        fatto[i] -> c.yel
                        else -> c.onHdr
                    }
                    Row(
                        (if (attiva) Modifier.weight(1f) else Modifier.width(46.dp))
                            .height(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (attiva) c.onHdr else c.hdrBtn)
                            .alpha(if (abilitata) 1f else 0.5f)
                            .clickable(enabled = abilitata, role = Role.Tab) { azioni.vaiAStep(i) }
                            .semantics { contentDescription = label },
                        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(if (!attiva && fatto[i]) Icons.Default.Check else icon, contentDescription = null, tint = fg)
                        if (attiva) Text(label, style = AppText.metaStrong, color = fg)
                    }
                }
            }
        }
        if (state.modalita == ModalitaWizard.MODIFICA_INVIATA) {
            Row(
                Modifier
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(c.yel)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.LockOpen, null, tint = c.onYel)
                Text("Stai modificando una giornaliera inviata", style = AppText.buttonSmall, color = c.onYel, modifier = Modifier.weight(1f))
                Text("Rev. ${(state.giornaliera?.revisione ?: 0) + 1}", style = AppText.buttonSmall, color = c.onYel)
            }
        }
        if (dettaglio) Spacer(Modifier.height(10.dp))
    }
}

// ───────────────────────── Barra in basso ─────────────────────────

@Composable
private fun WizardBottomBar(barra: BarraWizard, azioni: WizardAzioni) {
    val c = AppTheme.colors
    BottomBar {
        barra.sinistra?.let { s ->
            Box(
                Modifier
                    .size(62.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(c.card)
                    .border(BorderStroke(2.dp, c.pri), RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button) {
                        when (s) {
                            AzioneSinistra.ESCI -> azioni.esci()
                            AzioneSinistra.SELEZIONE_PERSONALE -> azioni.tornaASelezionePersonale()
                            AzioneSinistra.STEP_0 -> azioni.vaiAStep(0)
                            AzioneSinistra.SELEZIONE_MEZZI -> azioni.tornaASelezioneMezzi()
                            AzioneSinistra.STEP_1 -> azioni.vaiAStep(1)
                            AzioneSinistra.STEP_2 -> azioni.vaiAStep(2)
                        }
                    }
                    .semantics { contentDescription = if (s == AzioneSinistra.ESCI) "Esci" else "Indietro" },
                contentAlignment = Alignment.Center
            ) {
                Icon(if (s == AzioneSinistra.ESCI) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack, null, tint = c.pri, modifier = Modifier.size(28.dp))
            }
        }
        val (bg, fg) = when (barra.stile) {
            StileCta.PRIMARIO -> c.pri to c.onPri
            StileCta.FORTE -> c.strong to c.onStrong
            StileCta.DISATTIVO -> c.soft to c.sub
        }
        val icona = when (barra.icona) {
            IconaCta.LUCCHETTO -> Icons.Default.Lock
            IconaCta.MATITA -> Icons.Default.Edit
            IconaCta.AVANTI -> Icons.AutoMirrored.Filled.ArrowForward
            IconaCta.INVIA -> Icons.AutoMirrored.Filled.Send
            IconaCta.SINCRONIZZA -> Icons.Default.Sync
        }
        BigButton(
            text = barra.label,
            hint = barra.suggerimento,
            onClick = {
                if (barra.stile == StileCta.DISATTIVO) {
                    barra.suggerimento?.let(azioni.toast)
                } else when (barra.azione) {
                    AzioneCta.MODIFICA -> azioni.richiediModifica()
                    AzioneCta.VAI_DATI_PERSONALE -> azioni.vaiADatiPersonale()
                    AzioneCta.AVANTI_DA_PERSONALE -> azioni.avantiDaPersonale()
                    AzioneCta.VAI_DATI_MEZZI -> azioni.vaiADatiMezzi()
                    AzioneCta.AVANTI_DA_MEZZI -> azioni.avantiDaMezzi()
                    AzioneCta.AVANTI_A_RIEPILOGO -> azioni.avantiARiepilogo()
                    AzioneCta.INVIA -> azioni.invia()
                }
            },
            icon = icona,
            container = bg,
            content = fg,
            height = 62.dp,
            textStyle = AppText.buttonLarge,
            modifier = Modifier.weight(1f)
        )
    }
}

// ───────────────────────── Passi ─────────────────────────

@Composable
private fun TitoloPasso(titolo: String, destra: String? = null, sotto: String? = null) {
    val c = AppTheme.colors
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text(titolo, style = AppText.pageTitle, color = c.ink, modifier = Modifier.weight(1f))
            if (destra != null) Text(destra, style = AppText.buttonSmall.copy(fontSize = AppText.body.fontSize), color = c.pri)
        }
        if (sotto != null) Text(sotto, style = AppText.bodySmall, color = c.sub, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun <T> Griglia(items: List<T>, cella: @Composable (T, Modifier) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(2).forEach { riga ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                riga.forEach { cella(it, Modifier.weight(1f)) }
                if (riga.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PersoneSelezione(state: WizardUiState, persone: List<Personale>, azioni: WizardAzioni) {
    val n = state.personaleSelections.size
    val attivi = persone.filter { it.attivo }
    val tutti = attivi.isNotEmpty() && attivi.all { it.id in state.personaleSelections }
    TitoloPasso("Chi c'è oggi?", "$n ${if (n == 1) "presente" else "presenti"}")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.squadraPrecedente.isNotEmpty()) {
            OutlineButton("Squadra di ieri", azioni.squadraPrecedente, Modifier.weight(1f), Icons.Default.History)
        }
        OutlineButton(
            if (tutti) "Nessuno" else "Tutti",
            { azioni.tutti(!tutti) },
            Modifier.weight(1f),
            if (tutti) Icons.Default.RemoveDone else Icons.Default.DoneAll
        )
    }
    if (persone.isEmpty()) {
        InfoBox("Nessuna persona in elenco: aggiungile da Impostazioni › Gestione liste.", InfoKind.SOFT, icon = Icons.Default.Info)
    }
    Griglia(persone) { p, mod ->
        SelectTile(
            selected = p.id in state.personaleSelections,
            onClick = { azioni.togglePresente(p.id) },
            title = "${p.nome} ${p.cognome}",
            initials = "${p.nome.take(1)}${p.cognome.take(1)}".uppercase(),
            modifier = mod
        )
    }
}

@Composable
private fun PersoneDati(
    state: WizardUiState,
    nomePersona: (Long) -> String,
    azioni: WizardAzioni,
    scegli: (campo: String, label: String, isTime: Boolean) -> Unit
) {
    val c = AppTheme.colors
    val f = state.form
    TitoloPasso(
        "Compila dati personale",
        sotto = state.formDa?.let { "Valori proposti dalla giornaliera del ${Formati.dataBreve(it)}. Tocca un valore per cambiarlo." }
            ?: "Tocca un valore per cambiarlo."
    )
    AppPanel(padding = PaddingValues(10.dp)) {
        val tiles = listOf(
            Triple("Inizio turno", Formati.hhmm(f.inizio), Icons.AutoMirrored.Filled.Login) to { scegli("inizio", "Inizio turno", true) },
            Triple("Fine turno", Formati.hhmm(f.fine), Icons.AutoMirrored.Filled.Logout) to { scegli("fine", "Fine turno", true) },
            Triple("Pausa", formatDurata(f.pausa), Icons.Default.Coffee) to { scegli("pausa", "Pausa", false) },
            Triple("Ore di lavoro", formatDurata(f.lavoroMinuti), Icons.Default.Calculate) to { azioni.toast("Calcolate da turno e pausa") },
            Triple("Viaggio andata", formatDurata(f.andata), Icons.Default.East) to { scegli("andata", "Viaggio andata", false) },
            Triple("Viaggio ritorno", formatDurata(f.ritorno), Icons.Default.West) to { scegli("ritorno", "Viaggio ritorno", false) }
        )
        Griglia(tiles) { (t, onClick), mod ->
            ValueTile(t.first, t.second, t.third, mod, filled = t.first != "Ore di lavoro", onClick = onClick)
        }
        SwitchRow("Pernotto", f.pernotto, azioni.togglePernotto, icon = Icons.Default.Hotel, modifier = Modifier.padding(top = 4.dp))
    }
    f.errore()?.let { InfoBox(it, InfoKind.ERROR, icon = Icons.Default.Error) }
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        SectionLabel("Applica a")
        Row {
            TextLink("Tutti", { azioni.applicaTutti(true) })
            TextLink("Nessuno", { azioni.applicaTutti(false) })
        }
    }
    state.personaleSelections.keys.sortedBy { nomePersona(it) }.forEach { id ->
        val p = state.personaleSelections.getValue(id)
        val ok = p.compilato()
        ApplyRow(
            title = nomePersona(id),
            subtitle = if (ok) "Lavoro ${Formati.ore(p.oreLavoro)} h · viaggio ${Formati.ore(p.oreViaggioAndata + p.oreViaggioRitorno)} h" else null,
            selected = id in state.applica,
            statusText = if (ok) "${Formati.hhmm((p.inizioTurno / 60_000).toInt())}–${Formati.hhmm((p.fineTurno / 60_000).toInt())}" else "da compilare",
            statusIcon = if (ok) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            statusColor = if (ok) c.ok else c.sub,
            onClick = { azioni.toggleApplica(id) }
        )
    }
}

@Composable
private fun MezziSelezione(state: WizardUiState, mezzi: List<Mezzo>, azioni: WizardAzioni) {
    val n = state.mezzoSelections.size
    TitoloPasso("Quali mezzi?", "$n ${if (n == 1) "mezzo" else "mezzi"}")
    if (mezzi.isEmpty()) {
        InfoBox("Nessun mezzo in elenco: aggiungili da Impostazioni › Gestione liste.", InfoKind.SOFT, icon = Icons.Default.Info)
    }
    Griglia(mezzi) { m, mod ->
        SelectTile(
            selected = m.id in state.mezzoSelections,
            onClick = { azioni.toggleMezzo(m.id) },
            title = m.nome,
            subtitle = m.targa,
            icon = Icons.Default.LocalShipping,
            height = 116.dp,
            modifier = mod
        )
    }
}

@Composable
private fun MezziDati(state: WizardUiState, mezzi: List<Mezzo>, azioni: WizardAzioni) {
    val c = AppTheme.colors
    TitoloPasso("Compila dati mezzi", sotto = "Inserisci i km, scegli i mezzi e premi Applica.")
    OutlineButton("Usa dati precedenti", azioni.usaDatiPrecedenti, Modifier.fillMaxWidth(), Icons.Default.History, height = 56.dp)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CampoKm("KM andata", state.kmAndata, azioni.kmAndata, Modifier.weight(1f))
        CampoKm("KM ritorno", state.kmRitorno, azioni.kmRitorno, Modifier.weight(1f))
    }
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        SectionLabel("Applica a")
        Row {
            TextLink("Tutti", { azioni.applicaMezziTutti(true) })
            TextLink("Nessuno", { azioni.applicaMezziTutti(false) })
        }
    }
    mezzi.filter { it.id in state.mezzoSelections }.forEach { m ->
        val d = state.mezzoSelections.getValue(m.id)
        val ok = d.compilato()
        ApplyRow(
            title = m.nome,
            subtitle = m.targa,
            selected = m.id in state.applicaMezzi,
            statusText = if (ok) "A ${Formati.km(d.kmAndata ?: 0f)} · R ${Formati.km(d.kmRitorno ?: 0f)}" else "da compilare",
            statusIcon = if (ok) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            statusColor = if (ok) c.ok else c.sub,
            onClick = { azioni.toggleApplicaMezzo(m.id) }
        )
    }
}

@Composable
private fun CampoKm(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier) {
    val c = AppTheme.colors
    AppPanel(modifier, padding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 12.dp)) {
        Text(label, style = AppText.metaStrong, color = c.sub)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = AppText.input.copy(color = c.ink),
            cursorBrush = SolidColor(c.pri),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp).semantics { contentDescription = label },
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text("0", style = AppText.input, color = c.sub.copy(alpha = 0.5f))
                    inner()
                }
            }
        )
    }
}

@Composable
private fun AttrezzatureStep(state: WizardUiState, attrezzature: List<Attrezzatura>, azioni: WizardAzioni) {
    TitoloPasso("Attrezzature usate", "${state.attrezzaturaSelections.size} su ${attrezzature.count { it.attivo }}")
    Griglia(attrezzature) { a, mod ->
        SelectTile(
            selected = a.id in state.attrezzaturaSelections,
            onClick = { azioni.toggleAttrezzatura(a.id) },
            title = a.nome,
            icon = Icons.Default.Construction,
            modifier = mod
        )
    }
}

@Composable
private fun RiepilogoStep(
    state: WizardUiState,
    persone: List<Personale>,
    mezzi: List<Mezzo>,
    attrezzature: List<Attrezzatura>,
    nomePersona: (Long) -> String,
    azioni: WizardAzioni,
    cambiaData: () -> Unit
) {
    val c = AppTheme.colors
    val modificabile = state.modalita != ModalitaWizard.DETTAGLIO
    val reinvio = state.modalita == ModalitaWizard.MODIFICA_INVIATA
    val r = state.riepilogo

    if (modificabile) Text(if (reinvio) "Controlla e reinvia" else "Controlla e invia", style = AppText.pageTitle, color = c.ink)

    if (reinvio) {
        AppPanel(border = BorderStroke(2.dp, c.yel)) {
            SectionLabel("Motivo della modifica", icon = Icons.Default.EditNote)
            TextArea(
                value = state.motivo,
                onValueChange = azioni.motivo,
                placeholder = "es. Corretto l'orario di fine di Luca Bianchi",
                maxLength = 200,
                minHeight = 76.dp,
                footerStart = "Obbligatorio · comparirà nel PDF",
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }

    if (state.modifiche.isNotEmpty()) {
        AppPanel {
            SectionLabel(
                if (state.modifiche.size == 1) "Modificata 1 volta" else "Modificata ${state.modifiche.size} volte",
                icon = Icons.Default.History
            )
            state.modifiche.reversed().forEach { m ->
                Column(Modifier.padding(top = 8.dp)) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
                    Text("Rev. ${m.revisione} · ${Formati.dataOra(m.modificataAt)}", style = AppText.metaStrong, color = c.sub, modifier = Modifier.padding(top = 10.dp))
                    Text(
                        m.motivo + (m.dataPrecedente?.let { " · Data spostata dal ${Formati.dataBreve(it)}" } ?: ""),
                        style = AppText.body, color = c.ink
                    )
                }
            }
        }
    }

    AppPanel {
        SectionLabel("Note", icon = Icons.AutoMirrored.Filled.Notes)
        if (modificabile) {
            TextArea(state.note, azioni.note, "Eventuali note sulla giornata…", 500, Modifier.padding(top = 8.dp))
        } else {
            Text(state.note.ifBlank { "Nessuna nota" }, style = AppText.body, color = c.ink, modifier = Modifier.padding(top = 8.dp))
        }
    }

    AppPanel {
        SectionLabel("Data", icon = Icons.Default.Event)
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            val oggi = Formati.stessoGiorno(state.selectedDate, System.currentTimeMillis())
            Text((if (oggi) "Oggi · " else "") + Formati.dataLunga(state.selectedDate), style = AppText.cardTitle, color = c.ink)
            if (modificabile) {
                Box(
                    Modifier
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(c.soft)
                        .clickable(role = Role.Button, onClick = cambiaData)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) { Text("Cambia", style = AppText.buttonSmall, color = c.pri) }
            }
        }
        if (state.dataSpostata) {
            InfoBox(
                "Prima era il ${Formati.dataBreve(state.dataOriginale!!)}. PDF e CSV verranno rinominati con la nuova data.",
                InfoKind.WARN,
                Modifier.padding(top = 10.dp),
                Icons.Default.DriveFileRenameOutline
            )
        }
    }

    AppPanel {
        SectionHeaderRow("Personale", Icons.Default.Groups, if (modificabile) "Modifica" else null) { azioni.vaiAStep(0) }
        Row(Modifier.padding(top = 10.dp, bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MiniKpi("Presenti", "${r.totalePersonePresenti}", Modifier.weight(1f))
            MiniKpi("Lavoro", "${Formati.ore(r.totaleOreLavoro)} h", Modifier.weight(1f))
            MiniKpi("Viaggio", "${Formati.ore(r.totaleOreViaggio)} h", Modifier.weight(1f))
            MiniKpi("Pernotti", "${r.totalePernotti}", Modifier.weight(1f))
        }
        state.personaleSelections.keys.sortedBy { nomePersona(it) }.forEach { id ->
            val p = state.personaleSelections.getValue(id)
            Column(Modifier.padding(top = 4.dp)) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
                Text(nomePersona(id), style = AppText.itemTitle, color = c.ink, modifier = Modifier.padding(top = 10.dp))
                if (p.compilato()) {
                    Text("Lavoro ${Formati.ore(p.oreLavoro)} h · Viaggio ${Formati.ore(p.oreViaggioAndata + p.oreViaggioRitorno)} h", style = AppText.meta, color = c.sub)
                    Text(
                        "Turno ${Formati.hhmm((p.inizioTurno / 60_000).toInt())}–${Formati.hhmm((p.fineTurno / 60_000).toInt())}" + if (p.pernotto) " · Pernotto" else "",
                        style = AppText.meta, color = c.sub, modifier = Modifier.padding(bottom = 6.dp)
                    )
                } else {
                    Text("Dati da compilare", style = AppText.meta, color = c.err, modifier = Modifier.padding(bottom = 6.dp))
                }
            }
        }
    }

    AppPanel {
        SectionHeaderRow("Mezzi · ${Formati.km(r.totaleKm)} km", Icons.Default.LocalShipping, if (modificabile) "Modifica" else null) { azioni.vaiAStep(1) }
        val usati = mezzi.filter { it.id in state.mezzoSelections }
        usati.forEach { m ->
            val d = state.mezzoSelections.getValue(m.id)
            Column(Modifier.padding(top = 6.dp)) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(m.nome, style = AppText.itemTitle, color = c.ink)
                        Text(m.targa, style = AppText.meta, color = c.sub)
                    }
                    Text(
                        if (d.compilato()) "${Formati.km((d.kmAndata ?: 0f) + (d.kmRitorno ?: 0f))} km" else "da compilare",
                        style = AppText.rowTitle.copy(lineHeight = AppText.itemTitle.lineHeight),
                        color = if (d.compilato()) c.ink else c.err
                    )
                }
            }
        }
        if (usati.isEmpty()) Text("Nessun mezzo utilizzato", style = AppText.bodySmall, color = c.sub, modifier = Modifier.padding(top = 8.dp))
    }

    AppPanel {
        SectionHeaderRow("Attrezzature", Icons.Default.Construction, if (modificabile) "Modifica" else null) { azioni.vaiAStep(2) }
        val usate = attrezzature.filter { it.id in state.attrezzaturaSelections }
        if (usate.isEmpty()) {
            Text("Nessuna attrezzatura", style = AppText.bodySmall, color = c.sub)
        } else {
            Chips(usate.map { it.nome })
        }
    }
}

@Composable
private fun MiniKpi(label: String, value: String, modifier: Modifier) {
    val c = AppTheme.colors
    Column(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(c.soft)
            .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 10.dp)
    ) {
        Text(label, style = AppText.chip.copy(letterSpacing = AppText.chip.letterSpacing * 0), color = c.sub)
        Text(value, style = AppText.sheetTitle, color = c.ink, maxLines = 1)
    }
}

@Composable
private fun Chips(testi: List<String>) {
    val c = AppTheme.colors
    // A capo semplice: righe da 2
    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        testi.chunked(2).forEach { riga ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                riga.forEach {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(c.soft)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) { Text(it, style = AppText.buttonSmall, color = c.ink) }
                }
            }
        }
    }
}

// ───────────────────────── Fogli di dialogo ─────────────────────────

@Composable
private fun androidx.compose.foundation.layout.BoxScope.WizardSheets(state: WizardUiState, azioni: WizardAzioni) {
    val c = AppTheme.colors
    val d = state.dialog
    AppSheet(visible = d == WizardDialog.Esci, onDismiss = azioni.chiudiDialog) {
        SheetTitle("Uscire dalla giornaliera?", "Puoi salvarla come bozza e finirla più tardi.")
        SheetButton(if (state.modalita == ModalitaWizard.MODIFICA) "Salva modifiche" else "Salva bozza", azioni.salvaBozza, icon = Icons.Default.Save)
        SheetButton("Scarta", azioni.scarta, SheetButtonKind.DANGER, Icons.Default.Delete)
        SheetButton("Continua a compilare", azioni.chiudiDialog, SheetButtonKind.SOFT)
    }
    AppSheet(visible = d == WizardDialog.EsciModifica, onDismiss = azioni.chiudiDialog) {
        SheetTitle("Annullare le modifiche?", "La giornaliera inviata resta com'è, anche su Drive.")
        SheetButton("Annulla modifiche", azioni.annullaModifiche, SheetButtonKind.DANGER, Icons.AutoMirrored.Filled.Undo)
        SheetButton("Continua a modificare", azioni.chiudiDialog, SheetButtonKind.SOFT)
    }
    AppSheet(visible = d is WizardDialog.Inviata, onDismiss = azioni.fatto) {
        val inv = d as? WizardDialog.Inviata ?: return@AppSheet
        SheetIcon(Icons.Default.CheckCircle)
        SheetTitle("Giornaliera inviata")
        FileBox(listOf(inv.fileName to false, (inv.folderName ?: if (inv.condivisa) "Condivisione" else "") to true))
        if (inv.memoriaLocale) InfoBox("Archivio non raggiungibile: il PDF è stato salvato nella memoria del telefono.", InfoKind.WARN, icon = Icons.Default.Info)
        if (inv.csvErrore != null) InfoBox("CSV non salvato: ${inv.csvErrore}", InfoKind.WARN, icon = Icons.Default.Error)
        SheetButton("Fatto", azioni.fatto)
        inv.webViewLink?.let { link -> SheetButton("Apri su Drive", { azioni.apriLink(link); azioni.fatto() }, SheetButtonKind.SOFT_PRIMARY, Icons.AutoMirrored.Filled.OpenInNew) }
    }
    AppSheet(visible = d is WizardDialog.Aggiornata, onDismiss = azioni.chiudiDialog) {
        val e = (d as? WizardDialog.Aggiornata)?.esito ?: return@AppSheet
        SheetIcon(Icons.Default.CheckCircle)
        SheetTitle("Giornaliera aggiornata")
        val dove = when {
            e.archivio == ArchivioFile.CONDIVISO -> "Nuovo PDF da condividere"
            e.esitoPdf == EsitoSostituzione.CREATO_NON_TROVATO -> "File originale non trovato: creato di nuovo"
            e.esitoPdf == EsitoSostituzione.CREATO_SENZA_PERMESSI -> "File di un altro account: creato nuovo file"
            e.archivio == ArchivioFile.DRIVE -> "Sostituito su Google Drive"
            else -> "Sovrascritto sul telefono"
        }
        FileBox(
            listOfNotNull(
                e.fileName to false,
                "$dove · Rev. ${e.revisione}" to true,
                e.nomePrecedente?.let { "Rinominato da $it" to true }
            )
        )
        when {
            e.reportFalliti.isNotEmpty() -> InfoBox(
                "${e.reportFalliti.size} report non aggiornati: riprova dalla commessa.", InfoKind.WARN, icon = Icons.Default.Info
            )
            e.reportAggiornati.isNotEmpty() -> InfoBox(
                if (e.reportAggiornati.size == 1) "Report finale aggiornato: ${e.reportAggiornati.first()}" else "Report finali aggiornati: ${e.reportAggiornati.size}",
                InfoKind.OK, icon = Icons.Default.CheckCircle
            )
            else -> InfoBox("Nessun report esportato da questo telefono contiene questa data.", InfoKind.SOFT, icon = Icons.Default.Info)
        }
        if (e.registro != null) {
            InfoBox("Annotata nel registro ${e.registro}: l'ufficio sa cosa è cambiato.", InfoKind.SOFT, icon = Icons.Default.FactCheck)
        } else if (e.registroErrore != null) {
            InfoBox("Registro modifiche non scritto: ${e.registroErrore}", InfoKind.WARN, icon = Icons.Default.Error)
        }
        if (e.csvErrore != null) InfoBox("CSV non aggiornato: ${e.csvErrore}", InfoKind.WARN, icon = Icons.Default.Error)
        SheetButton("Fatto", azioni.chiudiDialog)
        e.webViewLink?.let { link -> SheetButton("Apri su Drive", { azioni.apriLink(link); azioni.chiudiDialog() }, SheetButtonKind.SOFT_PRIMARY, Icons.AutoMirrored.Filled.OpenInNew) }
    }
    AppSheet(visible = d is WizardDialog.Errore, onDismiss = azioni.chiudiDialog) {
        val err = d as? WizardDialog.Errore ?: return@AppSheet
        SheetIcon(Icons.Default.Error, c.err)
        SheetTitle(err.titolo, err.messaggio)
        SheetButton("OK", azioni.chiudiDialog)
    }
    AppSheet(visible = d is WizardDialog.Configurazione, onDismiss = azioni.chiudiDialog) {
        val cfg = d as? WizardDialog.Configurazione ?: return@AppSheet
        SheetIcon(Icons.Default.Settings, c.err)
        SheetTitle("Configurazione incompleta", cfg.messaggio)
        SheetButton("OK", azioni.chiudiDialog)
    }
    AppSheet(visible = d == WizardDialog.Offline, onDismiss = azioni.chiudiDialog) {
        SheetIcon(Icons.Default.WifiOff, c.err)
        SheetTitle("Connessione non disponibile", "Per salvare su Google Drive serve internet. Riprova quando sei online.")
        SheetButton("OK", azioni.chiudiDialog)
    }
}
