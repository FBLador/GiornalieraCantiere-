package com.idroambiente.giornalieradicantiere.screenshots

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.idroambiente.giornalieradicantiere.ui.commesse.CommesseContent
import com.idroambiente.giornalieradicantiere.ui.commesse.RiepilogoCommessaContent
import com.idroambiente.giornalieradicantiere.ui.commesse.RiepilogoCommessaData
import com.idroambiente.giornalieradicantiere.ui.components.ToastState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h892dp-xxhdpi")
class HomeScreenshots {
    @get:Rule
    val compose = createComposeRule()

    private fun home(dark: Boolean, nuova: Boolean = false) {
        compose.render(dark) {
            CommesseContent(
                cards = Campioni.cards,
                oggi = Campioni.OGGI,
                showCreateSheet = nuova,
                createError = null,
                toast = ToastState(),
                onSettings = {}, onOpenCommessa = {}, onRiepilogo = {}, onAzione = {},
                onNuova = {}, onDismissNuova = {}, onCrea = { _, _ -> },
                initiallyShowChiuse = true
            )
        }
    }

    @Test fun home_chiaro() { home(false); compose.shot("01_home_chiaro") }
    @Test fun home_scuro() { home(true); compose.shot("01_home_scuro") }
    @Test fun home_nuovaCommessa() { home(false, nuova = true); compose.shot("02_nuova_commessa") }

    @Test
    fun riepilogo() {
        compose.render {
            RiepilogoCommessaContent(
                commessa = Campioni.commesse[1],
                giornaliere = Campioni.giornaliere.filter { it.cantiereId == 2L },
                oreGiornaliera = mapOf(3L to 16f, 4L to 16f, 5L to 24f),
                riepilogo = RiepilogoCommessaData(56f, 6f, 154f),
                startDate = null, endDate = null, oggi = Campioni.OGGI,
                exporting = false, success = null, error = null, configError = null, offline = false,
                onBack = {}, onPeriodo = { _, _ -> }, onOpenGiornaliera = {}, onExport = {},
                onDismissSuccess = {}, onOpenDrive = {}, onDismissError = {}, onDismissConfig = {}, onDismissOffline = {}
            )
        }
        compose.shot("20_riepilogo_commessa")
    }
}
