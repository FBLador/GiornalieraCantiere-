package com.idroambiente.giornalieradicantiere.screenshots

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.idroambiente.giornalieradicantiere.data.preferences.ExportBehavior
import com.idroambiente.giornalieradicantiere.data.preferences.StorageMode
import com.idroambiente.giornalieradicantiere.database.dao.GiornalieraSommario
import com.idroambiente.giornalieradicantiere.ui.components.ToastState
import com.idroambiente.giornalieradicantiere.ui.giornaliere.GiornaliereContent
import com.idroambiente.giornalieradicantiere.ui.impostazioni.CambioPinState
import com.idroambiente.giornalieradicantiere.ui.impostazioni.ImpostazioniAzioni
import com.idroambiente.giornalieradicantiere.ui.impostazioni.ImpostazioniContent
import com.idroambiente.giornalieradicantiere.ui.impostazioni.ImpostazioniUi
import com.idroambiente.giornalieradicantiere.ui.impostazioni.ListeContent
import com.idroambiente.giornalieradicantiere.ui.impostazioni.ModuloLista
import com.idroambiente.giornalieradicantiere.ui.impostazioni.PassoPin
import com.idroambiente.giornalieradicantiere.ui.impostazioni.VoceLista
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Calendar

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h892dp-xxhdpi")
class AltreScreenshots {
    @get:Rule
    val compose = createComposeRule()

    private fun commessa(nome: String, daAggiornare: Int) {
        compose.render {
            GiornaliereContent(
                commessa = Campioni.commesse[1],
                giornaliere = Campioni.giornaliere.filter { it.cantiereId == 2L }.sortedByDescending { it.data },
                sommari = mapOf(3L to GiornalieraSommario(3, 2, 16f), 4L to GiornalieraSommario(4, 2, 16f), 5L to GiornalieraSommario(5, 3, 24f)),
                oggi = Campioni.OGGI,
                reportDaAggiornare = daAggiornare,
                aggiornandoReport = false,
                toast = ToastState(),
                onBack = {}, onRiepilogo = {}, onApri = {}, onNuova = {}, onRiprovaReport = {}
            )
        }
        compose.shot(nome)
    }

    @Test fun commessa() = commessa("17_commessa_giornaliere", 0)
    @Test fun commessaReport() = commessa("18_commessa_report_da_aggiornare", 1)

    private val ui = ImpostazioniUi(
        versione = "1.1.0", promemoriaAttivo = true, ora = 17, minuto = 30,
        giorni = setOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY),
        storageMode = StorageMode.GOOGLE_DRIVE, driveConnesso = true, driveAccount = "ufficio@idroambiente.it", driveCartella = "Giornaliere 2026"
    )

    @Test
    fun impostazioni() {
        compose.render { ImpostazioniContent(ui, null, ToastState(), ImpostazioniAzioni()) }
        compose.shot("21_impostazioni")
    }

    @Test
    @Config(qualifiers = "w412dp-h1500dp-xxhdpi")
    fun impostazioniTelefono() {
        compose.render { ImpostazioniContent(ui.copy(storageMode = StorageMode.LOCAL, cartellaLocale = "Documenti › Giornaliere", comportamento = ExportBehavior.SHARE), null, ToastState(), ImpostazioniAzioni()) }
        compose.shot("22_impostazioni_telefono_condividi")
    }

    @Test
    fun cambioPin() {
        compose.render { ImpostazioniContent(ui, CambioPinState(passo = PassoPin.NUOVO, cifre = "48"), ToastState(), ImpostazioniAzioni()) }
        compose.shot("23_cambio_pin")
    }

    private val voci = CampioniWizard.persone.map { VoceLista(it.id, "${it.nome[0]}${it.cognome[0]}", "${it.nome} ${it.cognome}", "Personale") }

    @Test
    fun liste() {
        compose.render { ListeContent(0, voci, null, null, {}, {}, {}, {}, {}, {}, {}, {}) }
        compose.shot("24_gestione_liste")
    }

    @Test
    fun listeMezzo() {
        val mezzi = CampioniWizard.mezzi.map { VoceLista(it.id, it.nome.take(2).uppercase(), it.nome, it.targa) }
        compose.render {
            ListeContent(
                1, mezzi,
                ModuloLista("Nuovo mezzo", listOf("Nome", "Targa", "Codice articolo km", "Codice articolo giorni"), listOf("Iveco Daily", "FG 123 AB", "", ""), 2),
                null, {}, {}, {}, {}, {}, {}, {}, {}
            )
        }
        compose.shot("25_nuovo_mezzo")
    }

    @Test
    fun listeElimina() {
        compose.render { ListeContent(0, voci, null, null, {}, {}, {}, {}, {}, {}, {}, {}, daEliminareIniziale = voci[1]) }
        compose.shot("26_elimina_persona")
    }
}
