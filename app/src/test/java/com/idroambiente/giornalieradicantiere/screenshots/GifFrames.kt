package com.idroambiente.giornalieradicantiere.screenshots

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.idroambiente.giornalieradicantiere.database.entities.StatoGiornaliera
import com.idroambiente.giornalieradicantiere.services.FaseInvio
import com.idroambiente.giornalieradicantiere.ui.components.ToastState
import com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard.AttrezzaturaFormData
import com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard.Fase
import com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard.MezzoFormData
import com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard.PinUiState
import com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard.RiepilogoData
import com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard.TurnoForm
import com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard.WizardAzioni
import com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard.WizardContent
import com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard.WizardDialog
import com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard.WizardUiState
import com.idroambiente.giornalieradicantiere.ui.theme.GiornalieraDiCantiereTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Fotogrammi per le GIF dei flussi (montati poi con uno script). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h892dp-xxhdpi")
class GifFrames {
    @get:Rule
    val compose = createComposeRule()

    private fun filma(cartella: String, sequenza: List<Pair<String, WizardUiState>>) {
        val dir = File(SCREENSHOT_DIR, "gif/$cartella").apply { mkdirs() }
        var stato by mutableStateOf(sequenza.first().second)
        compose.setContent {
            GiornalieraDiCantiereTheme {
                WizardContent(
                    state = stato,
                    persone = CampioniWizard.persone,
                    mezzi = CampioniWizard.mezzi,
                    attrezzature = CampioniWizard.attrezzature,
                    toastState = ToastState(),
                    nomePersona = CampioniWizard::nome,
                    nomeMezzo = CampioniWizard::nomeMezzo,
                    azioni = WizardAzioni()
                )
            }
        }
        sequenza.forEachIndexed { i, (nome, s) ->
            stato = s
            compose.waitForIdle()
            compose.mainClock.advanceTimeBy(700)
            compose.waitForIdle()
            compose.onRoot().captureRoboImage(File(dir, "%02d_%s.png".format(i, nome)).path)
        }
    }

    @Test
    fun modifica() {
        val w = CampioniWizard
        val pin = (0..6).map { n -> "pin$n" to w.dettaglio.copy(pin = PinUiState(cifre = "202900".take(n))) }
        val motivo = "Data sbagliata: il lavoro era di lunedì 2"
        val scrittura = listOf(10, 24, motivo.length).map { n -> "motivo$n" to w.modificaSenzaMotivo.copy(motivo = motivo.take(n)) }
        val fasi = w.invio.fasiInvio.map { f -> "fase_${f.name}" to w.invio.copy(faseCorrente = f) }
        filma(
            "modifica",
            listOf("dettaglio" to w.dettaglio) + pin + listOf("motivo_vuoto" to w.modificaSenzaMotivo) + scrittura +
                listOf("data_spostata" to w.modifica) + fasi + listOf("aggiornata" to w.aggiornata)
        )
    }

    @Test
    fun compilazione() {
        val w = CampioniWizard
        val comp = TurnoForm().toFormData()
        val vuoto = comp.copy(oreLavoro = 0f)
        val base = w.nuova
        val fasi = listOf(
            FaseInvio.PREPARAZIONE, FaseInvio.GENERAZIONE_PDF, FaseInvio.GENERAZIONE_CSV,
            FaseInvio.CARICAMENTO_PDF, FaseInvio.CARICAMENTO_CSV, FaseInvio.FINALIZZAZIONE
        )
        val riep = w.riepilogoNuova
        filma(
            "compilazione",
            listOf(
                "vuota" to base,
                "uno" to base.copy(personaleSelections = mapOf(1L to vuoto)),
                "squadra" to w.personeSelezione,
                "orari" to w.personeSelezione.copy(persPhase = Fase.DATI, applica = setOf(1L, 2L, 3L)),
                "orari_ok" to w.personeSelezione.copy(persPhase = Fase.DATI, personaleSelections = mapOf(1L to comp, 2L to comp, 3L to comp)),
                "mezzi" to w.mezziSelezione,
                "km" to w.mezziDati.copy(mezzoSelections = mapOf(1L to MezzoFormData(true), 2L to MezzoFormData(true)), applicaMezzi = setOf(1L, 2L)),
                "km_ok" to w.mezziDati.copy(mezzoSelections = mapOf(1L to MezzoFormData(true, 19f, 19f), 2L to MezzoFormData(true, 19f, 19f)), applicaMezzi = emptySet()),
                "attrezzature" to w.attrezzatureStep,
                "riepilogo" to riep
            ) + fasi.map { f -> "fase_${f.name}" to riep.copy(fasiInvio = fasi, faseCorrente = f) } + listOf(
                "inviata" to riep.copy(
                    giornaliera = riep.giornaliera,
                    dialog = WizardDialog.Inviata("0001221_2026-11-05.pdf", "0001221 › Giornaliere", "https://drive.google.com/x", false, null, false)
                )
            )
        )
    }

    @Suppress("unused")
    private val usati = listOf(StatoGiornaliera.BOZZA, AttrezzaturaFormData(), RiepilogoData())
}
