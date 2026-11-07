package com.idroambiente.giornalieradicantiere.ui.commesse

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.idroambiente.giornalieradicantiere.R
import com.idroambiente.giornalieradicantiere.database.entities.StatoCommessa
import com.idroambiente.giornalieradicantiere.ui.common.Formati
import com.idroambiente.giornalieradicantiere.ui.common.ui
import com.idroambiente.giornalieradicantiere.ui.components.AppHeader
import com.idroambiente.giornalieradicantiere.ui.components.AppPanel
import com.idroambiente.giornalieradicantiere.ui.components.AppSheet
import com.idroambiente.giornalieradicantiere.ui.components.BigButton
import com.idroambiente.giornalieradicantiere.ui.components.BigTextField
import com.idroambiente.giornalieradicantiere.ui.components.HeaderIconButton
import com.idroambiente.giornalieradicantiere.ui.components.InfoBox
import com.idroambiente.giornalieradicantiere.ui.components.InfoKind
import com.idroambiente.giornalieradicantiere.ui.components.SheetButton
import com.idroambiente.giornalieradicantiere.ui.components.SheetButtonKind
import com.idroambiente.giornalieradicantiere.ui.components.SheetTitle
import com.idroambiente.giornalieradicantiere.ui.components.SoftIconButton
import com.idroambiente.giornalieradicantiere.ui.components.ToastHost
import com.idroambiente.giornalieradicantiere.ui.components.ToastState
import com.idroambiente.giornalieradicantiere.ui.components.rememberToastState
import com.idroambiente.giornalieradicantiere.ui.theme.AppText
import com.idroambiente.giornalieradicantiere.ui.theme.AppTheme
import com.idroambiente.giornalieradicantiere.util.formatCommessaNome

@Composable
fun CommesseScreen(
    viewModel: CommesseViewModel = hiltViewModel(),
    onNavigateToCommessa: (Long) -> Unit = {},
    onNavigateToRiepilogoCommessa: (Long) -> Unit = {},
    onNavigateToImpostazioni: () -> Unit = {},
    onNavigateToWizard: (cantiereId: Long, giornalieraId: Long?) -> Unit = { _, _ -> }
) {
    val cards by viewModel.cards.collectAsState()
    val showCreate by viewModel.showCreateDialog.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    val toastMsg by viewModel.toast.collectAsState()
    val toast = rememberToastState()
    LaunchedEffect(toastMsg) {
        toastMsg?.let { toast.show(it); viewModel.toastShown() }
    }

    CommesseContent(
        cards = cards,
        oggi = remember { System.currentTimeMillis() },
        showCreateSheet = showCreate,
        createError = error,
        toast = toast,
        onSettings = onNavigateToImpostazioni,
        onOpenCommessa = onNavigateToCommessa,
        onRiepilogo = onNavigateToRiepilogoCommessa,
        onAzione = { card ->
            when (val a = card.azione) {
                is AzioneCommessa.OggiFatta -> viewModel.conCantiere(card.commessa.id) { onNavigateToWizard(it, a.giornaliera.id) }
                is AzioneCommessa.RiprendiBozza -> viewModel.conCantiere(card.commessa.id) { onNavigateToWizard(it, a.giornaliera.id) }
                AzioneCommessa.VediGiornaliere -> onNavigateToCommessa(card.commessa.id)
                AzioneCommessa.CompilaOggi -> viewModel.conCantiere(card.commessa.id) { onNavigateToWizard(it, null) }
            }
        },
        onNuova = viewModel::showCreateDialog,
        onDismissNuova = viewModel::hideCreateDialog,
        onCrea = { codice, cliente -> viewModel.createCommessa(codice, cliente, "") }
    )
}

data class AzioneUi(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val bg: Color, val fg: Color)

@Composable
fun AzioneCommessa.ui(): AzioneUi {
    val c = AppTheme.colors
    return when (this) {
        is AzioneCommessa.OggiFatta -> AzioneUi(
            if (giornaliera.stato.name == "INVIATA") "Oggi inviata" else "Oggi completata",
            Icons.Default.TaskAlt, c.okBg, c.ok
        )
        is AzioneCommessa.RiprendiBozza -> AzioneUi(
            if (oggi) "Riprendi bozza di oggi" else "Riprendi bozza ${Formati.dataBreve(giornaliera.data)}",
            Icons.Default.EditNote, c.warnBg, c.warn
        )
        AzioneCommessa.VediGiornaliere -> AzioneUi("Vedi giornaliere", Icons.AutoMirrored.Filled.List, c.soft, c.pri)
        AzioneCommessa.CompilaOggi -> AzioneUi("Compila oggi", Icons.Default.EditCalendar, c.pri, c.onPri)
    }
}

@Composable
fun CommesseContent(
    cards: List<CommessaCard>,
    oggi: Long,
    showCreateSheet: Boolean,
    createError: String?,
    toast: ToastState,
    onSettings: () -> Unit,
    onOpenCommessa: (Long) -> Unit,
    onRiepilogo: (Long) -> Unit,
    onAzione: (CommessaCard) -> Unit,
    onNuova: () -> Unit,
    onDismissNuova: () -> Unit,
    onCrea: (Int, String) -> Unit,
    initiallyShowChiuse: Boolean = false
) {
    val c = AppTheme.colors
    var showChiuse by rememberSaveable { mutableStateOf(initiallyShowChiuse) }
    val aperte = cards.filter { it.commessa.stato != StatoCommessa.CHIUSA }
    val chiuse = cards.filter { it.commessa.stato == StatoCommessa.CHIUSA }

    Box(Modifier.fillMaxSize().background(c.bg)) {
        Column(Modifier.fillMaxSize()) {
            AppHeader(
                title = "Commesse",
                subtitle = Formati.oggiEsteso(oggi),
                titleStyle = AppText.headerLarge,
                leading = {
                    Image(
                        painter = painterResource(R.drawable.logo_idroambiente),
                        contentDescription = "Idroambiente",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 54.dp, height = 42.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )
                },
                trailing = { HeaderIconButton(Icons.Default.Settings, "Impostazioni", onSettings) }
            )
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (cards.isEmpty()) {
                    item {
                        Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Nessuna commessa", style = AppText.cardTitle, color = c.ink)
                            Text("Tocca «Nuova commessa» per iniziare", style = AppText.body, color = c.sub)
                        }
                    }
                }
                items(aperte, key = { it.commessa.id }) { card ->
                    CommessaCardView(card, onOpenCommessa, onRiepilogo, onAzione)
                }
                if (chiuse.isNotEmpty()) {
                    item(key = "chiuse") {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 54.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(c.soft)
                                .clickable { showChiuse = !showChiuse }
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Inventory2, contentDescription = null, tint = c.sub)
                            Text("Chiuse (${chiuse.size})", style = AppText.buttonSmall.copy(fontSize = AppText.body.fontSize), color = c.sub, modifier = Modifier.weight(1f))
                            Icon(if (showChiuse) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = c.sub)
                        }
                    }
                    if (showChiuse) {
                        items(chiuse, key = { it.commessa.id }) { card ->
                            AppPanel(onClick = { onOpenCommessa(card.commessa.id) }) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Column(Modifier.weight(1f)) {
                                        Text(formatCommessaNome(card.commessa.nome), style = AppText.header, color = c.sub)
                                        Text(card.commessa.cliente, style = AppText.body, color = c.sub)
                                    }
                                    SoftIconButton(Icons.Default.BarChart, "Riepilogo", { onRiepilogo(card.commessa.id) })
                                }
                            }
                        }
                    }
                }
            }
            Box(Modifier.navigationBarsPadding().padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 8.dp)) {
                BigButton("Nuova commessa", onNuova, icon = Icons.Default.Add, container = c.strong, content = c.onStrong)
            }
        }

        ToastHost(toast)

        AppSheet(visible = showCreateSheet, onDismiss = onDismissNuova) {
            NuovaCommessaSheet(createError, onCrea, onDismissNuova)
        }
    }
}

@Composable
private fun CommessaCardView(
    card: CommessaCard,
    onOpen: (Long) -> Unit,
    onRiepilogo: (Long) -> Unit,
    onAzione: (CommessaCard) -> Unit
) {
    val c = AppTheme.colors
    val stato = card.commessa.stato.ui()
    val azione = card.azione.ui()
    AppPanel {
        Column(Modifier.clickable { onOpen(card.commessa.id) }) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(formatCommessaNome(card.commessa.nome), style = AppText.pageTitle, color = c.ink)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(stato.icon, contentDescription = null, tint = stato.color, modifier = Modifier.size(18.dp))
                    Text(stato.label, style = AppText.metaStrong, color = stato.color)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(card.commessa.cliente, style = AppText.bodyLarge, color = c.sub, modifier = Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${card.numeroGiornaliere}", style = AppText.metaStrong, color = c.sub)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "giornaliere", tint = c.sub)
                }
            }
        }
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BigButton(
                text = azione.label,
                onClick = { onAzione(card) },
                icon = azione.icon,
                container = azione.bg,
                content = azione.fg,
                height = 58.dp,
                modifier = Modifier.weight(1f)
            )
            SoftIconButton(Icons.Default.BarChart, "Riepilogo commessa", { onRiepilogo(card.commessa.id) }, size = 58.dp)
        }
    }
}

@Composable
private fun NuovaCommessaSheet(error: String?, onCrea: (Int, String) -> Unit, onAnnulla: () -> Unit) {
    val c = AppTheme.colors
    var p1 by rememberSaveable { mutableStateOf("") }
    var p2 by rememberSaveable { mutableStateOf("") }
    var cliente by rememberSaveable { mutableStateOf("") }
    val suffisso = remember { FocusRequester() }
    val ok = p1.length == 4 && p2.length == 3 && cliente.isNotBlank()
    val codiceStyle = AppText.sheetTitle.copy(fontSize = AppText.valueLarge.fontSize * 0.86f, letterSpacing = AppText.section.letterSpacing * 2.5f)

    SheetTitle("Nuova commessa")
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BigTextField(
            value = p1,
            onValueChange = {
                p1 = it.filter(Char::isDigit).take(4)
                if (p1.length == 4) runCatching { suffisso.requestFocus() }
            },
            label = "Codice · ${p1.length}/4",
            placeholder = "0000",
            textStyle = codiceStyle,
            keyboardType = KeyboardType.Number,
            modifier = Modifier.weight(4f)
        )
        Text("–", style = AppText.pageTitle, color = c.sub, modifier = Modifier.padding(bottom = 14.dp))
        BigTextField(
            value = p2,
            onValueChange = { p2 = it.filter(Char::isDigit).take(3) },
            label = "Suffisso · ${p2.length}/3",
            placeholder = "000",
            textStyle = codiceStyle,
            keyboardType = KeyboardType.Number,
            modifier = Modifier.weight(3f).focusRequester(suffisso)
        )
    }
    BigTextField(value = cliente, onValueChange = { cliente = it.take(100) }, label = "Cliente", placeholder = "es. Uniacque Bergamo")
    if (error != null) InfoBox(error, InfoKind.ERROR, icon = Icons.Default.Error)
    SheetButton("Crea commessa", { if (ok) onCrea((p1 + p2).toInt(), cliente) }, icon = Icons.Default.Add, enabled = ok)
    SheetButton("Annulla", onAnnulla, SheetButtonKind.SOFT)
}
