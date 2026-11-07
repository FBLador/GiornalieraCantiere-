package com.idroambiente.giornalieradicantiere.screenshots

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.idroambiente.giornalieradicantiere.ui.components.ToastState
import com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard.WizardAzioni
import com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard.WizardContent
import com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard.WizardUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h892dp-xxhdpi")
class WizardScreenshots {
    @get:Rule
    val compose = createComposeRule()

    private fun wizard(nome: String, s: WizardUiState, dark: Boolean = false, scelta: Triple<String, String, Boolean>? = null) {
        compose.render(dark) {
            WizardContent(
                state = s,
                persone = CampioniWizard.persone,
                mezzi = CampioniWizard.mezzi,
                attrezzature = CampioniWizard.attrezzature,
                toastState = ToastState(),
                nomePersona = CampioniWizard::nome,
                nomeMezzo = CampioniWizard::nomeMezzo,
                azioni = WizardAzioni(),
                sceltaIniziale = scelta
            )
        }
        compose.shot(nome)
    }

    @Test fun persone() = wizard("03_wizard_persone", CampioniWizard.personeSelezione)
    @Test fun orari() = wizard("04_wizard_orari", CampioniWizard.personeDati)
    @Test fun rotella() = wizard("05_wizard_rotella", CampioniWizard.personeDati, scelta = Triple("fine", "Fine turno", true))
    @Test fun mezzi() = wizard("06_wizard_mezzi", CampioniWizard.mezziSelezione)
    @Test fun km() = wizard("07_wizard_km", CampioniWizard.mezziDati)
    @Test fun attrezzature() = wizard("08_wizard_attrezzature", CampioniWizard.attrezzatureStep)
    @Test fun riepilogo() = wizard("09_wizard_riepilogo", CampioniWizard.riepilogoNuova)
    @Test fun dettaglio() = wizard("10_dettaglio_inviata", CampioniWizard.dettaglio)
    @Test fun pin() = wizard("11_pin", CampioniWizard.pin)
    @Test fun pinErrato() = wizard("12_pin_errato", CampioniWizard.pinErrato)
    @Test fun modificaSenzaMotivo() = wizard("13_modifica_motivo_mancante", CampioniWizard.modificaSenzaMotivo)
    @Test fun modifica() = wizard("14_modifica_cambio_data", CampioniWizard.modifica)
    @Test fun invio() = wizard("15_reinvio_in_corso", CampioniWizard.invio)
    @Test fun aggiornata() = wizard("16_giornaliera_aggiornata", CampioniWizard.aggiornata)
    @Test fun dettaglioScuro() = wizard("10_dettaglio_inviata_scuro", CampioniWizard.dettaglio, dark = true)
    @Test fun orariScuro() = wizard("04_wizard_orari_scuro", CampioniWizard.personeDati, dark = true)
}
