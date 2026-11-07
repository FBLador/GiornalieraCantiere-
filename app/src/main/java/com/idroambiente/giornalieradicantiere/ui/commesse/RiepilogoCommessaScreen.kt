package com.idroambiente.giornalieradicantiere.ui.commesse

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Commute
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.idroambiente.giornalieradicantiere.database.entities.Commessa
import com.idroambiente.giornalieradicantiere.database.entities.Giornaliera
import com.idroambiente.giornalieradicantiere.ui.common.Formati
import com.idroambiente.giornalieradicantiere.ui.components.AppHeader
import com.idroambiente.giornalieradicantiere.ui.components.AppPanel
import com.idroambiente.giornalieradicantiere.ui.components.AppSheet
import com.idroambiente.giornalieradicantiere.ui.components.BigButton
import com.idroambiente.giornalieradicantiere.ui.components.BottomBar
import com.idroambiente.giornalieradicantiere.ui.components.FileBox
import com.idroambiente.giornalieradicantiere.ui.components.InfoBox
import com.idroambiente.giornalieradicantiere.ui.components.InfoKind
import com.idroambiente.giornalieradicantiere.ui.components.SectionLabel
import com.idroambiente.giornalieradicantiere.ui.components.SegItem
import com.idroambiente.giornalieradicantiere.ui.components.SegmentedControl
import com.idroambiente.giornalieradicantiere.ui.components.SheetButton
import com.idroambiente.giornalieradicantiere.ui.components.SheetButtonKind
import com.idroambiente.giornalieradicantiere.ui.components.SheetIcon
import com.idroambiente.giornalieradicantiere.ui.components.SheetTitle
import com.idroambiente.giornalieradicantiere.ui.components.StatusChip
import com.idroambiente.giornalieradicantiere.ui.components.TextLink
import com.idroambiente.giornalieradicantiere.ui.theme.AppText
import com.idroambiente.giornalieradicantiere.ui.theme.AppTheme
import com.idroambiente.giornalieradicantiere.util.formatCommessaNome
import java.util.Calendar

/** Filtri rapidi del periodo. */
enum class FiltroPeriodo { TUTTO, SETTE_GIORNI, MESE, PERIODO }

object Periodi {
    fun setteGiorni(oggi: Long): Pair<Long, Long> {
        val inizio = Calendar.getInstance().apply { timeInMillis = Formati.inizioGiorno(oggi); add(Calendar.DAY_OF_MONTH, -6) }.timeInMillis
        return inizio to Formati.inizioGiorno(oggi)
    }

    fun meseCorrente(oggi: Long): Pair<Long, Long> {
        val inizio = Calendar.getInstance().apply { timeInMillis = Formati.inizioGiorno(oggi); set(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
        val fine = Calendar.getInstance().apply {
            timeInMillis = inizio; add(Calendar.MONTH, 1); add(Calendar.DAY_OF_MONTH, -1)
        }.timeInMillis
        return inizio to fine
    }

    fun filtro(start: Long?, end: Long?, oggi: Long): FiltroPeriodo = when {
        start == null && end == null -> FiltroPeriodo.TUTTO
        start to end == setteGiorni(oggi) -> FiltroPeriodo.SETTE_GIORNI
        start to end == meseCorrente(oggi) -> FiltroPeriodo.MESE
        else -> FiltroPeriodo.PERIODO
    }
}

@Composable
fun RiepilogoCommessaScreen(
    viewModel: RiepilogoCommessaViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onOpenGiornaliera: (cantiereId: Long, giornalieraId: Long) -> Unit = { _, _ -> }
) {
    val commessa by viewModel.commessa.collectAsState()
    val giornaliere by viewModel.giornaliere.collectAsState()
    val riepilogo by viewModel.riepilogo.collectAsState()
    val ore by viewModel.oreGiornaliera.collectAsState()
    val startDate by viewModel.startDate.collectAsState()
    val endDate by viewModel.endDate.collectAsState()
    val exporting by viewModel.pdfExporting.collectAsState()
    val success by viewModel.pdfExportSuccess.collectAsState()
    val error by viewModel.pdfExportError.collectAsState()
    val configError by viewModel.showConfigError.collectAsState()
    val offline by viewModel.showOfflineDialog.collectAsState()
    val cantiereId by viewModel.cantiereId.collectAsState()
    val context = LocalContext.current

    RiepilogoCommessaContent(
        commessa = commessa,
        giornaliere = giornaliere,
        oreGiornaliera = ore,
        riepilogo = riepilogo,
        startDate = startDate,
        endDate = endDate,
        oggi = remember { System.currentTimeMillis() },
        exporting = exporting,
        success = success,
        error = error,
        configError = configError,
        offline = offline,
        onBack = onNavigateBack,
        onPeriodo = viewModel::setPeriodo,
        onOpenGiornaliera = { g -> cantiereId?.let { onOpenGiornaliera(it, g.id) } },
        onExport = viewModel::exportPdf,
        onDismissSuccess = viewModel::dismissExportSuccess,
        onOpenDrive = { link ->
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) }
            viewModel.dismissExportSuccess()
        },
        onDismissError = viewModel::dismissExportError,
        onDismissConfig = viewModel::dismissConfigError,
        onDismissOffline = viewModel::dismissOfflineDialog
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiepilogoCommessaContent(
    commessa: Commessa?,
    giornaliere: List<Giornaliera>,
    oreGiornaliera: Map<Long, Float>,
    riepilogo: RiepilogoCommessaData,
    startDate: Long?,
    endDate: Long?,
    oggi: Long,
    exporting: Boolean,
    success: PdfExportResult?,
    error: String?,
    configError: String?,
    offline: Boolean,
    onBack: () -> Unit,
    onPeriodo: (Long?, Long?) -> Unit,
    onOpenGiornaliera: (Giornaliera) -> Unit,
    onExport: () -> Unit,
    onDismissSuccess: () -> Unit,
    onOpenDrive: (String) -> Unit,
    onDismissError: () -> Unit,
    onDismissConfig: () -> Unit,
    onDismissOffline: () -> Unit
) {
    val c = AppTheme.colors
    val codice = commessa?.let { formatCommessaNome(it.nome) } ?: ""
    val filtro = Periodi.filtro(startDate, endDate, oggi)
    var pickStart by remember { mutableStateOf(false) }
    var pickEnd by remember { mutableStateOf(false) }
    val ordinate = giornaliere.sortedByDescending { it.data }

    Box(Modifier.fillMaxSize().background(c.bg)) {
        Column(Modifier.fillMaxSize()) {
            AppHeader(title = "Riepilogo $codice", subtitle = commessa?.cliente, onBack = onBack)
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    SegmentedControl(
                        items = listOf(SegItem("Tutto"), SegItem("7 giorni"), SegItem(Formati.meseCapitalizzato(oggi)), SegItem("Periodo")),
                        selectedIndex = filtro.ordinal,
                        height = 48.dp,
                        onSelect = { i ->
                            when (FiltroPeriodo.entries[i]) {
                                FiltroPeriodo.TUTTO -> onPeriodo(null, null)
                                FiltroPeriodo.SETTE_GIORNI -> Periodi.setteGiorni(oggi).let { onPeriodo(it.first, it.second) }
                                FiltroPeriodo.MESE -> Periodi.meseCorrente(oggi).let { onPeriodo(it.first, it.second) }
                                FiltroPeriodo.PERIODO -> pickStart = true
                            }
                        }
                    )
                }
                if (filtro == FiltroPeriodo.PERIODO) {
                    item {
                        AppPanel(padding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Dal ${startDate?.let { Formati.dataBreve(it) } ?: "inizio"} al ${endDate?.let { Formati.dataBreve(it) } ?: "fine"}",
                                    style = AppText.itemTitle, color = c.ink, modifier = Modifier.weight(1f)
                                )
                                TextLink("Cambia", { pickStart = true })
                            }
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Kpi(Icons.Default.Engineering, Formati.ore(riepilogo.totaleOreLavoro), "Ore lavoro", Modifier.weight(1f))
                        Kpi(Icons.Default.Commute, Formati.ore(riepilogo.totaleOreViaggio), "Ore viaggio", Modifier.weight(1f))
                        Kpi(Icons.Default.LocalShipping, Formati.km(riepilogo.totaleKm), "Km mezzi", Modifier.weight(1f))
                    }
                }
                item {
                    SectionLabel(
                        "${ordinate.size} ${if (ordinate.size == 1) "giornaliera" else "giornaliere"}",
                        Modifier.padding(start = 4.dp, top = 8.dp)
                    )
                }
                items(ordinate, key = { it.id }) { g ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 60.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(c.card)
                            .clickable { onOpenGiornaliera(g) }
                            .padding(start = 14.dp, end = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(Formati.dataLunga(g.data), style = AppText.itemTitle, color = c.ink, modifier = Modifier.weight(1f))
                        Text("${Formati.ore(oreGiornaliera[g.id] ?: 0f)} h", style = AppText.meta, color = c.sub)
                        StatusChip(g.stato)
                    }
                }
                if (ordinate.isEmpty()) {
                    item {
                        Text(
                            "Nessuna giornaliera nel periodo",
                            style = AppText.body, color = c.sub, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(24.dp)
                        )
                    }
                }
            }
            BottomBar {
                BigButton(
                    text = if (exporting) "Esportazione in corso…" else "Esporta PDF riepilogo",
                    onClick = { if (!exporting) onExport() },
                    icon = if (exporting) Icons.Default.HourglassTop else Icons.Default.PictureAsPdf,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        AppSheet(visible = success != null, onDismiss = onDismissSuccess) {
            val s = success ?: return@AppSheet
            SheetIcon(Icons.Default.PictureAsPdf, c.pri)
            SheetTitle(if (s.sovrascritto) "Report aggiornato" else "PDF esportato")
            FileBox(listOf(s.fileName to false, (s.folderName ?: "") to true))
            if (s.sovrascritto) InfoBox("Sostituito il report con lo stesso nome: nessun doppione.", InfoKind.OK)
            if (s.csvErrore != null) InfoBox("CSV non salvato: ${s.csvErrore}", InfoKind.WARN, icon = Icons.Default.Error)
            SheetButton("OK", onDismissSuccess)
            s.webViewLink?.let { link -> SheetButton("Apri su Drive", { onOpenDrive(link) }, SheetButtonKind.SOFT_PRIMARY, Icons.AutoMirrored.Filled.OpenInNew) }
        }
        AppSheet(visible = error != null, onDismiss = onDismissError) {
            SheetTitle("Errore esportazione", error)
            SheetButton("OK", onDismissError)
        }
        AppSheet(visible = configError != null, onDismiss = onDismissConfig) {
            SheetIcon(Icons.Default.Settings, c.err)
            SheetTitle("Configurazione incompleta", configError)
            SheetButton("OK", onDismissConfig)
        }
        AppSheet(visible = offline, onDismiss = onDismissOffline) {
            SheetIcon(Icons.Default.WifiOff, c.err)
            SheetTitle("Connessione non disponibile", "Per salvare su Google Drive serve internet. Riprova quando sei online.")
            SheetButton("OK", onDismissOffline)
        }
    }

    if (pickStart) {
        val state = rememberDatePickerState(initialSelectedDateMillis = startDate)
        DatePickerDialog(
            onDismissRequest = { pickStart = false },
            confirmButton = {
                TextButton(onClick = {
                    pickStart = false
                    onPeriodo(state.selectedDateMillis, endDate)
                    pickEnd = true
                }) { Text("Avanti: fine") }
            },
            dismissButton = { TextButton(onClick = { pickStart = false }) { Text("Annulla") } }
        ) { DatePicker(state = state, title = { Text("Dal giorno", Modifier.padding(start = 24.dp, top = 16.dp)) }) }
    }
    if (pickEnd) {
        val state = rememberDatePickerState(initialSelectedDateMillis = endDate ?: oggi)
        DatePickerDialog(
            onDismissRequest = { pickEnd = false },
            confirmButton = {
                TextButton(onClick = {
                    pickEnd = false
                    onPeriodo(startDate, state.selectedDateMillis)
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickEnd = false }) { Text("Annulla") } }
        ) { DatePicker(state = state, title = { Text("Al giorno", Modifier.padding(start = 24.dp, top = 16.dp)) }) }
    }
}

@Composable
private fun Kpi(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String, modifier: Modifier) {
    val c = AppTheme.colors
    AppPanel(modifier, padding = PaddingValues(12.dp)) {
        Icon(icon, contentDescription = null, tint = c.pri)
        Text(value, style = AppText.valueLarge, color = c.ink, modifier = Modifier.padding(top = 4.dp))
        Text(label, style = AppText.meta.copy(fontSize = AppText.section.fontSize), color = c.sub)
    }
}
