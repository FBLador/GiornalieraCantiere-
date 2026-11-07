package com.idroambiente.giornalieradicantiere.ui.impostazioni

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.idroambiente.giornalieradicantiere.ui.components.AppHeader
import com.idroambiente.giornalieradicantiere.ui.components.AppSheet
import com.idroambiente.giornalieradicantiere.ui.components.BigButton
import com.idroambiente.giornalieradicantiere.ui.components.BigTextField
import com.idroambiente.giornalieradicantiere.ui.components.BottomBar
import com.idroambiente.giornalieradicantiere.ui.components.InfoBox
import com.idroambiente.giornalieradicantiere.ui.components.InfoKind
import com.idroambiente.giornalieradicantiere.ui.components.SegItem
import com.idroambiente.giornalieradicantiere.ui.components.SegmentedControl
import com.idroambiente.giornalieradicantiere.ui.components.SheetButton
import com.idroambiente.giornalieradicantiere.ui.components.SheetButtonKind
import com.idroambiente.giornalieradicantiere.ui.components.SheetTitle
import com.idroambiente.giornalieradicantiere.ui.components.SoftIconButton
import com.idroambiente.giornalieradicantiere.ui.components.cardShadow
import com.idroambiente.giornalieradicantiere.ui.theme.AppText
import com.idroambiente.giornalieradicantiere.ui.theme.AppTheme

/** Una riga dell'elenco, uguale per persone, mezzi e attrezzature. */
data class VoceLista(val id: Long, val badge: String, val titolo: String, val sotto: String)

/** Modulo aggiungi/modifica: campi e valori. */
data class ModuloLista(
    val titolo: String,
    val campi: List<String>,
    val valori: List<String>,
    /** Quanti campi sono obbligatori (i primi N) */
    val obbligatori: Int
)

@Composable
fun ListeGestioneScreen(
    onNavigateBack: () -> Unit = {},
    personaleViewModel: PersonaleViewModel = hiltViewModel(),
    mezzoViewModel: MezzoViewModel = hiltViewModel(),
    attrezzaturaViewModel: AttrezzaturaViewModel = hiltViewModel()
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val persone by personaleViewModel.personaleList.collectAsState()
    val mezzi by mezzoViewModel.mezzoList.collectAsState()
    val attrezzature by attrezzaturaViewModel.attrezzaturaList.collectAsState()
    val nuovaPersona by personaleViewModel.showCreateDialog.collectAsState()
    val personaDaModificare by personaleViewModel.personaleToEdit.collectAsState()
    val duplicato by personaleViewModel.duplicateDialogState.collectAsState()
    val nuovoMezzo by mezzoViewModel.showCreateDialog.collectAsState()
    val mezzoDaModificare by mezzoViewModel.mezzoToEdit.collectAsState()
    val erroreMezzo by mezzoViewModel.errorMessage.collectAsState()
    val nuovaAttr by attrezzaturaViewModel.showCreateDialog.collectAsState()
    val attrDaModificare by attrezzaturaViewModel.attrezzaturaToEdit.collectAsState()
    val erroreAttr by attrezzaturaViewModel.errorMessage.collectAsState()

    val voci = when (tab) {
        0 -> persone.map { VoceLista(it.id, "${it.nome.take(1)}${it.cognome.take(1)}".uppercase(), "${it.nome} ${it.cognome}", "Personale") }
        1 -> mezzi.map { VoceLista(it.id, it.nome.take(2).uppercase(), it.nome, listOf(it.targa, it.codiceArticoloKm.ifBlank { null }?.let { "art. $it" }).filterNotNull().joinToString(" · ")) }
        else -> attrezzature.map { VoceLista(it.id, it.nome.take(2).uppercase(), it.nome, it.codiceArticolo.ifBlank { "Attrezzatura" }.let { if (it == "Attrezzatura") it else "art. $it" }) }
    }

    val modulo: ModuloLista? = when {
        tab == 0 && nuovaPersona -> ModuloLista("Nuova persona", listOf("Nome", "Cognome"), listOf("", ""), 2)
        tab == 0 && personaDaModificare != null -> personaDaModificare!!.let { ModuloLista("Modifica persona", listOf("Nome", "Cognome"), listOf(it.nome, it.cognome), 2) }
        tab == 1 && nuovoMezzo -> ModuloLista("Nuovo mezzo", listOf("Nome", "Targa", "Codice articolo km", "Codice articolo giorni"), listOf("", "", "", ""), 2)
        tab == 1 && mezzoDaModificare != null -> mezzoDaModificare!!.let {
            ModuloLista("Modifica mezzo", listOf("Nome", "Targa", "Codice articolo km", "Codice articolo giorni"), listOf(it.nome, it.targa, it.codiceArticoloKm, it.codiceArticoloGiorni), 2)
        }
        tab == 2 && nuovaAttr -> ModuloLista("Nuova attrezzatura", listOf("Nome", "Codice articolo"), listOf("", ""), 1)
        tab == 2 && attrDaModificare != null -> attrDaModificare!!.let { ModuloLista("Modifica attrezzatura", listOf("Nome", "Codice articolo"), listOf(it.nome, it.codiceArticolo), 1) }
        else -> null
    }

    ListeContent(
        tab = tab,
        voci = voci,
        modulo = modulo,
        errore = when (tab) {
            0 -> duplicato?.let { "Esiste già ${it.existingPersonale.nome} ${it.existingPersonale.cognome}" }
            1 -> erroreMezzo
            else -> erroreAttr
        },
        onBack = onNavigateBack,
        onTab = { tab = it },
        onAggiungi = {
            when (tab) {
                0 -> personaleViewModel.showCreateDialog()
                1 -> mezzoViewModel.showCreateDialog()
                else -> attrezzaturaViewModel.showCreateDialog()
            }
        },
        onModifica = { id ->
            when (tab) {
                0 -> persone.firstOrNull { it.id == id }?.let(personaleViewModel::showEditDialog)
                1 -> mezzi.firstOrNull { it.id == id }?.let(mezzoViewModel::showEditDialog)
                else -> attrezzature.firstOrNull { it.id == id }?.let(attrezzaturaViewModel::showEditDialog)
            }
        },
        onElimina = { id ->
            when (tab) {
                0 -> persone.firstOrNull { it.id == id }?.let(personaleViewModel::deletePersonale)
                1 -> mezzi.firstOrNull { it.id == id }?.let(mezzoViewModel::deleteMezzo)
                else -> attrezzature.firstOrNull { it.id == id }?.let(attrezzaturaViewModel::deleteAttrezzatura)
            }
        },
        onSalva = { v ->
            when {
                tab == 0 && personaDaModificare != null -> personaleViewModel.updatePersonale(v[0], v[1])
                tab == 0 -> personaleViewModel.createPersonale(v[0], v[1])
                tab == 1 && mezzoDaModificare != null -> mezzoViewModel.updateMezzo(v[0], v[1], v[2], v[3])
                tab == 1 -> mezzoViewModel.createMezzo(v[0], v[1], v[2], v[3])
                tab == 2 && attrDaModificare != null -> attrezzaturaViewModel.updateAttrezzatura(v[0], v[1])
                else -> attrezzaturaViewModel.createAttrezzatura(v[0], v[1])
            }
        },
        onChiudiModulo = {
            personaleViewModel.dismissDuplicateDialog()
            personaleViewModel.hideCreateDialog(); personaleViewModel.hideEditDialog()
            mezzoViewModel.hideCreateDialog(); mezzoViewModel.hideEditDialog(); mezzoViewModel.clearError()
            attrezzaturaViewModel.hideCreateDialog(); attrezzaturaViewModel.hideEditDialog(); attrezzaturaViewModel.clearError()
        },
        onCorreggi = {
            personaleViewModel.confirmDuplicateAndModify()
            mezzoViewModel.clearError()
            attrezzaturaViewModel.clearError()
        }
    )
}

@Composable
fun ListeContent(
    tab: Int,
    voci: List<VoceLista>,
    modulo: ModuloLista?,
    errore: String?,
    onBack: () -> Unit,
    onTab: (Int) -> Unit,
    onAggiungi: () -> Unit,
    onModifica: (Long) -> Unit,
    onElimina: (Long) -> Unit,
    onSalva: (List<String>) -> Unit,
    onChiudiModulo: () -> Unit,
    onCorreggi: () -> Unit,
    daEliminareIniziale: VoceLista? = null
) {
    val c = AppTheme.colors
    var daEliminare by remember { mutableStateOf(daEliminareIniziale) }
    val etichettaNuovo = listOf("Aggiungi persona", "Aggiungi mezzo", "Aggiungi attrezzatura")[tab]

    Box(Modifier.fillMaxSize().background(c.bg)) {
        Column(Modifier.fillMaxSize()) {
            AppHeader(title = "Gestione liste", onBack = onBack)
            Box(Modifier.padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 4.dp)) {
                SegmentedControl(
                    items = listOf(SegItem("Persone", Icons.Default.Groups), SegItem("Mezzi", Icons.Default.LocalShipping), SegItem("Attrezz.", Icons.Default.Construction)),
                    selectedIndex = tab,
                    onSelect = onTab
                )
            }
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (voci.isEmpty()) {
                    item { Text("Elenco vuoto. Tocca «$etichettaNuovo».", style = AppText.body, color = c.sub, modifier = Modifier.padding(16.dp)) }
                }
                items(voci, key = { it.id }) { v ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 68.dp)
                            .cardShadow(RoundedCornerShape(12.dp), c.isDark)
                            .clip(RoundedCornerShape(12.dp))
                            .background(c.card)
                            .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(c.soft), contentAlignment = Alignment.Center) {
                            Text(v.badge, style = AppText.buttonSmall, color = c.pri)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(v.titolo, style = AppText.itemTitle, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(v.sotto, style = AppText.meta, color = c.sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        SoftIconButton(Icons.Default.Edit, "Modifica ${v.titolo}", { onModifica(v.id) }, size = 48.dp)
                        SoftIconButton(Icons.Default.Delete, "Elimina ${v.titolo}", { daEliminare = v }, size = 48.dp, container = c.errBg, tint = c.err)
                    }
                }
            }
            BottomBar {
                BigButton(etichettaNuovo, onAggiungi, icon = Icons.Default.Add, container = c.strong, content = c.onStrong, modifier = Modifier.weight(1f))
            }
        }

        AppSheet(visible = modulo != null, onDismiss = onChiudiModulo) {
            val m = modulo ?: return@AppSheet
            var valori by remember(m) { mutableStateOf(m.valori) }
            val ok = valori.take(m.obbligatori).all { it.isNotBlank() }
            SheetTitle(m.titolo)
            m.campi.forEachIndexed { i, campo ->
                BigTextField(
                    value = valori[i],
                    onValueChange = { nuovo -> valori = valori.toMutableList().also { it[i] = nuovo.take(60) } },
                    label = if (i >= m.obbligatori) "$campo (facoltativo)" else campo
                )
            }
            if (errore != null) {
                InfoBox(errore, InfoKind.ERROR, icon = Icons.Default.Error)
            }
            SheetButton("Salva", { if (ok) { onCorreggi(); onSalva(valori) } }, icon = Icons.Default.Check, enabled = ok)
            SheetButton("Annulla", onChiudiModulo, SheetButtonKind.SOFT)
        }
        AppSheet(visible = daEliminare != null, onDismiss = { daEliminare = null }) {
            val v = daEliminare ?: return@AppSheet
            SheetTitle("Eliminare ${v.titolo}?", "Non comparirà più nelle nuove giornaliere. Quelle già compilate e inviate restano invariate.")
            SheetButton("Elimina", { onElimina(v.id); daEliminare = null }, SheetButtonKind.DANGER_SOLID, Icons.Default.Delete)
            SheetButton("Annulla", { daEliminare = null }, SheetButtonKind.SOFT)
        }
    }
}
