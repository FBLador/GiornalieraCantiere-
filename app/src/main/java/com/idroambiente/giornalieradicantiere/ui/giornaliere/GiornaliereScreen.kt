package com.idroambiente.giornalieradicantiere.ui.giornaliere

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.idroambiente.giornalieradicantiere.database.dao.GiornalieraSommario
import com.idroambiente.giornalieradicantiere.database.entities.Commessa
import com.idroambiente.giornalieradicantiere.database.entities.Giornaliera
import com.idroambiente.giornalieradicantiere.ui.commesse.AzioneCommessa
import com.idroambiente.giornalieradicantiere.ui.commesse.azionePer
import com.idroambiente.giornalieradicantiere.ui.commesse.ui
import com.idroambiente.giornalieradicantiere.ui.common.Formati
import com.idroambiente.giornalieradicantiere.ui.common.ui
import com.idroambiente.giornalieradicantiere.ui.components.AppHeader
import com.idroambiente.giornalieradicantiere.ui.components.AppPanel
import com.idroambiente.giornalieradicantiere.ui.components.BigButton
import com.idroambiente.giornalieradicantiere.ui.components.HeaderIconButton
import com.idroambiente.giornalieradicantiere.ui.components.SectionLabel
import com.idroambiente.giornalieradicantiere.ui.components.StatusChip
import com.idroambiente.giornalieradicantiere.ui.components.ToastHost
import com.idroambiente.giornalieradicantiere.ui.components.ToastState
import com.idroambiente.giornalieradicantiere.ui.components.rememberToastState
import com.idroambiente.giornalieradicantiere.ui.theme.AppText
import com.idroambiente.giornalieradicantiere.ui.theme.AppTheme
import com.idroambiente.giornalieradicantiere.util.formatCommessaNome

@Composable
fun GiornaliereScreen(
    viewModel: GiornaliereViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToGiornaliera: (Long, Long) -> Unit = { _, _ -> },
    onNavigateToWizard: (Long) -> Unit = {},
    onNavigateToRiepilogoCommessa: (Long) -> Unit = {}
) {
    val commessa by viewModel.commessa.collectAsState()
    val giornaliere by viewModel.giornaliere.collectAsState()
    val sommari by viewModel.sommari.collectAsState()
    val cantiereId by viewModel.cantiereId.collectAsState()
    val daAggiornare by viewModel.reportDaAggiornare.collectAsState()
    val aggiornando by viewModel.aggiornandoReport.collectAsState()
    val toastMsg by viewModel.toast.collectAsState()
    val toast = rememberToastState()
    LaunchedEffect(toastMsg) { toastMsg?.let { toast.show(it); viewModel.toastShown() } }

    GiornaliereContent(
        commessa = commessa,
        giornaliere = giornaliere,
        sommari = sommari,
        oggi = remember { System.currentTimeMillis() },
        reportDaAggiornare = daAggiornare,
        aggiornandoReport = aggiornando,
        toast = toast,
        onBack = onNavigateBack,
        onRiepilogo = { onNavigateToRiepilogoCommessa(viewModel.commessaId) },
        onApri = { g -> cantiereId?.let { onNavigateToGiornaliera(it, g.id) } },
        onNuova = { cantiereId?.let(onNavigateToWizard) },
        onRiprovaReport = viewModel::riprovaReport
    )
}

@Composable
fun GiornaliereContent(
    commessa: Commessa?,
    giornaliere: List<Giornaliera>,
    sommari: Map<Long, GiornalieraSommario>,
    oggi: Long,
    reportDaAggiornare: Int,
    aggiornandoReport: Boolean,
    toast: ToastState,
    onBack: () -> Unit,
    onRiepilogo: () -> Unit,
    onApri: (Giornaliera) -> Unit,
    onNuova: () -> Unit,
    onRiprovaReport: () -> Unit
) {
    val c = AppTheme.colors
    Box(Modifier.fillMaxSize().background(c.bg)) {
        Column(Modifier.fillMaxSize()) {
            AppHeader(
                title = commessa?.let { formatCommessaNome(it.nome) } ?: "",
                subtitle = commessa?.cliente,
                onBack = onBack,
                trailing = { HeaderIconButton(Icons.Default.BarChart, "Riepilogo commessa", onRiepilogo) }
            )
            LazyColumn(
                Modifier.weight(1f).navigationBarsPadding(),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (commessa != null) {
                    item {
                        val stato = commessa.stato.ui()
                        val azione = azionePer(commessa, giornaliere, oggi)
                        val azioneUi = azione.ui()
                        AppPanel {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(stato.icon, contentDescription = null, tint = stato.color, modifier = Modifier.size(18.dp))
                                Text(stato.label, style = AppText.metaStrong, color = stato.color)
                            }
                            Text(commessa.cliente, style = AppText.cardTitle, color = c.ink, modifier = Modifier.padding(top = 6.dp))
                            if (commessa.indirizzo.isNotBlank()) {
                                Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Place, contentDescription = null, tint = c.sub, modifier = Modifier.size(18.dp))
                                    Text(commessa.indirizzo, style = AppText.bodySmall, color = c.sub)
                                }
                            }
                            BigButton(
                                text = azioneUi.label,
                                onClick = {
                                    when (azione) {
                                        is AzioneCommessa.OggiFatta -> onApri(azione.giornaliera)
                                        is AzioneCommessa.RiprendiBozza -> onApri(azione.giornaliera)
                                        AzioneCommessa.VediGiornaliere -> Unit
                                        AzioneCommessa.CompilaOggi -> onNuova()
                                    }
                                },
                                icon = azioneUi.icon,
                                container = azioneUi.bg,
                                content = azioneUi.fg,
                                modifier = Modifier.padding(top = 14.dp)
                            )
                        }
                    }
                }
                if (reportDaAggiornare > 0) {
                    item {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(c.warnBg)
                                .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.SyncProblem, contentDescription = null, tint = c.warn)
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (reportDaAggiornare == 1) "1 report da aggiornare" else "$reportDaAggiornare report da aggiornare",
                                    style = AppText.itemTitle, color = c.warn
                                )
                                Text("Non includono l'ultima modifica", style = AppText.meta, color = c.warn)
                            }
                            BigButton(
                                text = if (aggiornandoReport) "…" else "Riprova",
                                onClick = onRiprovaReport,
                                icon = Icons.Default.Sync,
                                container = c.yel,
                                content = c.onYel,
                                height = 48.dp,
                                textStyle = AppText.buttonSmall,
                                modifier = Modifier.width110()
                            )
                        }
                    }
                }
                item {
                    SectionLabel("Giornaliere (${giornaliere.size})", Modifier.padding(start = 4.dp, top = 8.dp))
                }
                items(giornaliere, key = { it.id }) { g ->
                    val s = sommari[g.id]
                    val info = buildString {
                        append("${s?.persone ?: 0} ${if (s?.persone == 1) "persona" else "persone"}")
                        append(" · ${Formati.ore(s?.oreLavoro ?: 0f)} h lavoro")
                        if (g.revisione > 0) append(" · rev. ${g.revisione}")
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 68.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(c.card)
                            .clickable { onApri(g) }
                            .padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                (if (Formati.stessoGiorno(g.data, oggi)) "Oggi · " else "") + Formati.dataLunga(g.data),
                                style = AppText.rowTitle, color = c.ink
                            )
                            Text(info, style = AppText.meta, color = c.sub)
                        }
                        StatusChip(g.stato)
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = c.sub)
                    }
                }
                if (giornaliere.isEmpty()) {
                    item {
                        Text(
                            "Nessuna giornaliera. Tocca «Compila oggi» per iniziare.",
                            style = AppText.body, color = c.sub, modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
        ToastHost(toast, bottomPadding = 24.dp)
    }
}

private fun Modifier.width110() = this.then(Modifier.size(width = 120.dp, height = 48.dp))
