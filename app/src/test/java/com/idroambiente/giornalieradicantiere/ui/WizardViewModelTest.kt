package com.idroambiente.giornalieradicantiere.ui

import android.content.Context
import android.os.Looper
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.idroambiente.giornalieradicantiere.data.preferences.ExportBehavior
import com.idroambiente.giornalieradicantiere.data.preferences.PinRepository
import com.idroambiente.giornalieradicantiere.data.preferences.PreferencesRepository
import com.idroambiente.giornalieradicantiere.data.preferences.StorageMode
import com.idroambiente.giornalieradicantiere.database.AppDatabase
import com.idroambiente.giornalieradicantiere.database.GiornalieraStore
import com.idroambiente.giornalieradicantiere.database.entities.ArchivioFile
import com.idroambiente.giornalieradicantiere.database.entities.Attrezzatura
import com.idroambiente.giornalieradicantiere.database.entities.Cantiere
import com.idroambiente.giornalieradicantiere.database.entities.Commessa
import com.idroambiente.giornalieradicantiere.database.entities.Mezzo
import com.idroambiente.giornalieradicantiere.database.entities.Personale
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
import com.idroambiente.giornalieradicantiere.services.CsvGeneratorService
import com.idroambiente.giornalieradicantiere.services.FileStorageService
import com.idroambiente.giornalieradicantiere.services.GiornalieraExporter
import com.idroambiente.giornalieradicantiere.services.GoogleAuthService
import com.idroambiente.giornalieradicantiere.services.GoogleDriveService
import com.idroambiente.giornalieradicantiere.services.NetworkUtils
import com.idroambiente.giornalieradicantiere.services.GiornalieraPdfData
import com.idroambiente.giornalieradicantiere.services.PdfGeneratorService
import com.idroambiente.giornalieradicantiere.services.RiepilogoCommessaPdfData
import com.idroambiente.giornalieradicantiere.services.RegistroModifiche
import com.idroambiente.giornalieradicantiere.services.RiepilogoExporter
import com.idroambiente.giornalieradicantiere.ui.common.Formati
import com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard.AzioneCta
import com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard.GiornalieraWizardViewModel
import com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard.ModalitaWizard
import com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard.StileCta
import com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard.WizardDialog
import com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard.barraWizard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Percorre il wizard come farebbe un caposquadra, con un database vero (in memoria):
 * compilazione e invio, poi modifica con PIN, cambio data e reinvio.
 * Salvataggio "Sul telefono" + "Condividi", così non serve Google Drive.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class WizardViewModelTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var prefs: PreferencesRepository
    private lateinit var pin: PinRepository
    private lateinit var fileStorage: FileStorageService
    private lateinit var exporter: GiornalieraExporter
    private lateinit var riepilogo: RiepilogoExporter
    private lateinit var registro: RegistroModifiche
    private var cantiereId = 0L

    private fun aspetta(cosa: String, timeoutMs: Long = 20_000, condizione: () -> Boolean) {
        val fine = System.currentTimeMillis() + timeoutMs
        while (!condizione()) {
            shadowOf(Looper.getMainLooper()).idle()
            if (System.currentTimeMillis() > fine) error("Timeout: $cosa")
            Thread.sleep(10)
        }
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // FileProvider ricorda in una cache statica la cartella del primo test: la azzeriamo
        runCatching {
            androidx.core.content.FileProvider::class.java.getDeclaredField("sCache")
                .apply { isAccessible = true }.get(null).let { (it as MutableMap<*, *>).clear() }
        }
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val store = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = { File(context.filesDir, "test-${System.nanoTime()}.preferences_pb") }
        )
        prefs = PreferencesRepository(store)
        pin = PinRepository(store)
        val drive = GoogleDriveService(context, GoogleAuthService(context))
        fileStorage = FileStorageService(context, prefs, drive)
        // PDF finto con lo stesso nome di quello vero: il motore PDF di Android non gira su JVM
        val pdf = object : PdfGeneratorService(context) {
            override suspend fun generateGiornalieraPdf(data: GiornalieraPdfData): File {
                val giorno = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ITALY).format(java.util.Date(data.giornaliera.data))
                return File(context.cacheDir, "Giornaliera_${data.cantiere.nome}_$giorno.pdf").apply {
                    writeText("PDF rev ${data.giornaliera.revisione} ${data.motivoModifica ?: ""}")
                }
            }

            override suspend fun generateRiepilogoCommessaPdf(data: RiepilogoCommessaPdfData): File =
                File(context.cacheDir, generateRiepilogoFileName(data.commessaNome, data.startDate, data.endDate)).apply { writeText("PDF") }
        }
        val csv = CsvGeneratorService(context)
        exporter = GiornalieraExporter(pdf, csv, fileStorage, prefs)
        riepilogo = RiepilogoExporter(
            CommessaRepository(db.commessaDao()), CantiereRepository(db.cantiereDao()), GiornalieraRepository(db.giornalieraDao()),
            PersonaleGiornalieraRepository(db.personaleGiornalieraDao()), MezzoGiornalieraRepository(db.mezzoGiornalieraDao()),
            MezzoRepository(db.mezzoDao()), AttrezzaturaRepository(db.attrezzaturaDao()),
            AttrezzaturaGiornalieraRepository(db.attrezzaturaGiornalieraDao()), pdf, csv, fileStorage, prefs, NetworkUtils(context), db.reportEsportatoDao()
        )
        registro = RegistroModifiche(fileStorage)
        File(context.getExternalFilesDir(null), "exports").deleteRecursively()

        runBlocking {
            prefs.setStorageMode(StorageMode.LOCAL)
            prefs.setExportBehavior(ExportBehavior.SHARE)
            val commessaId = db.commessaDao().insert(Commessa(nome = 1234, cliente = "Comune di Bergamo", indirizzo = "Piazza Matteotti 27"))
            cantiereId = db.cantiereDao().insert(Cantiere(commessaId = commessaId, nome = "0001234", indirizzo = ""))
            db.personaleDao().insertAll(listOf(Personale(1, "Stefano", "Ricci"), Personale(2, "Paolo", "Esposito"), Personale(3, "Davide", "Greco")))
            db.mezzoDao().insertAll(listOf(Mezzo(1, "FK 302 LM", "Furgone TV ispezioni"), Mezzo(2, "FG 123 AB", "Iveco Daily")))
            db.attrezzaturaDao().insertAll(listOf(Attrezzatura(1, "Telecamera ispezione"), Attrezzatura(2, "Generatore 5 kW")))
        }
    }

    @After
    fun tearDown() = db.close()

    private fun viewModel(giornalieraId: Long? = null) = GiornalieraWizardViewModel(
        SavedStateHandle(buildMap { put("cantiereId", cantiereId); giornalieraId?.let { put("giornalieraId", it) } }),
        PersonaleRepository(db.personaleDao()),
        GiornalieraRepository(db.giornalieraDao()),
        PersonaleGiornalieraRepository(db.personaleGiornalieraDao()),
        MezzoRepository(db.mezzoDao()),
        MezzoGiornalieraRepository(db.mezzoGiornalieraDao()),
        AttrezzaturaRepository(db.attrezzaturaDao()),
        AttrezzaturaGiornalieraRepository(db.attrezzaturaGiornalieraDao()),
        CantiereRepository(db.cantiereDao()),
        CommessaRepository(db.commessaDao()),
        fileStorage, prefs, NetworkUtils(context), GiornalieraStore(db), exporter, riepilogo, registro, pin,
        db.giornalieraModificaDao()
    ).also { vm -> aspetta("caricamento") { !vm.uiState.value.isLoading } }

    /** Compila e invia una giornaliera nuova; restituisce il suo id. */
    private fun compilaEInvia(): Long {
        val vm = viewModel()
        assertEquals(ModalitaWizard.NUOVA, vm.uiState.value.modalita)

        vm.togglePresente(1); vm.togglePresente(2)
        vm.vaiADatiPersonale()
        assertEquals(setOf(1L, 2L), vm.uiState.value.applica)
        // Non si può andare avanti senza orari
        vm.avantiDaPersonale()
        assertEquals(0, vm.uiState.value.currentStep)
        vm.applicaOrari()
        vm.avantiDaPersonale()
        assertEquals(1, vm.uiState.value.currentStep)

        vm.toggleMezzo(1)
        vm.vaiADatiMezzi()
        vm.setKmAndata("22"); vm.setKmRitorno("22")
        vm.applicaKm()
        vm.avantiDaMezzi()
        assertEquals(2, vm.uiState.value.currentStep)

        vm.toggleAttrezzatura(1)
        vm.avantiARiepilogo()
        val s = vm.uiState.value
        assertEquals(3, s.currentStep)
        assertEquals(16f, s.riepilogo.totaleOreLavoro)
        assertEquals(44f, s.riepilogo.totaleKm)
        val barra = barraWizard(s, vm::nomePersona, vm::nomeMezzo)
        assertEquals(AzioneCta.INVIA, barra.azione)
        assertEquals(StileCta.PRIMARIO, barra.stile)

        vm.invia()
        aspetta("invio") { vm.uiState.value.dialog != null }
        val dialog = vm.uiState.value.dialog
        assertTrue("Dialog: $dialog", dialog is WizardDialog.Inviata)
        assertNotNull("Deve aprire la condivisione", vm.uiState.value.condividi)
        return vm.uiState.value.giornaliera!!.id
    }

    @Test
    fun compilazioneEInvio_salvaTuttoNelDatabase() = runBlocking {
        val id = compilaEInvia()
        val g = db.giornalieraDao().getById(id)!!
        assertEquals(StatoGiornaliera.INVIATA, g.stato)
        assertEquals(ArchivioFile.CONDIVISO.name, g.fileArchivio)
        assertEquals(2, db.personaleGiornalieraDao().getByGiornalieraId(id).first().size)
        assertEquals(8f, db.personaleGiornalieraDao().getByGiornalieraId(id).first().first().oreLavoro)
        assertNotNull("Il CSV salvato va ricordato", g.csvFileRef)
        assertTrue(File(g.csvFileRef!!).exists())
    }

    @Test
    fun modificaConPinCambioDataEReinvio() = runBlocking {
        val id = compilaEInvia()
        val primo = db.giornalieraDao().getById(id)!!
        val vecchioCsv = File(primo.csvFileRef!!)

        val vm = viewModel(id)
        assertEquals(ModalitaWizard.DETTAGLIO, vm.uiState.value.modalita)

        // PIN sbagliato: resta in sola lettura
        vm.richiediModifica()
        aspetta("pin") { vm.uiState.value.pin != null }
        "111111".forEach(vm::pinCifra)
        aspetta("pin errato") { vm.uiState.value.pin?.errore != null }
        assertEquals(ModalitaWizard.DETTAGLIO, vm.uiState.value.modalita)
        assertTrue(vm.uiState.value.pin!!.errore!!.contains("4 tentativi"))

        // PIN aziendale 202900
        "202900".forEach(vm::pinCifra)
        aspetta("sblocco") { vm.uiState.value.modalita == ModalitaWizard.MODIFICA_INVIATA }
        assertNull(vm.uiState.value.pin)

        // Senza motivo non si reinvia
        val barra = barraWizard(vm.uiState.value, vm::nomePersona, vm::nomeMezzo)
        assertEquals(StileCta.DISATTIVO, barra.stile)
        assertEquals("Scrivi il motivo della modifica", barra.suggerimento)

        // Data futura rifiutata, ieri accettata
        val ieri = primo.data - 86_400_000L
        vm.cambiaData(primo.data + 3 * 86_400_000L)
        aspetta("toast data futura") { vm.uiState.value.toast != null }
        assertEquals("Non puoi scegliere una data futura", vm.uiState.value.toast)
        vm.toastMostrato()
        vm.cambiaData(ieri)
        aspetta("data cambiata") { Formati.stessoGiorno(vm.uiState.value.selectedDate, ieri) }
        assertTrue(vm.uiState.value.dataSpostata)

        // Corregge l'orario di fine di Paolo: 17:30
        vm.vaiAStep(0)
        vm.impostaValoreForm("fine", 17 * 60 + 30)
        vm.toggleApplica(2)
        vm.applicaOrari()
        assertEquals(9f, vm.uiState.value.personaleSelections.getValue(2).oreLavoro)
        vm.vaiAStep(3)
        vm.setMotivo("Data sbagliata e orario di fine di Paolo")

        vm.invia()
        aspetta("reinvio") { vm.uiState.value.dialog != null }
        val d = vm.uiState.value.dialog
        assertTrue("Dialog: $d", d is WizardDialog.Aggiornata)
        d as WizardDialog.Aggiornata
        assertEquals(1, d.esito.revisione)
        assertNotNull("Registro scritto", d.esito.registro)

        // Database: revisione, data nuova, righe nuove, storico
        val g = db.giornalieraDao().getById(id)!!
        assertEquals(1, g.revisione)
        assertTrue(Formati.stessoGiorno(g.data, ieri))
        assertEquals(StatoGiornaliera.INVIATA, g.stato)
        assertEquals(17f, db.personaleGiornalieraDao().getByGiornalieraId(id).first().sumOf { it.oreLavoro.toDouble() }.toFloat())
        val storico = db.giornalieraModificaDao().getByGiornalieraIdSync(id)
        assertEquals(1, storico.size)
        assertEquals("Data sbagliata e orario di fine di Paolo", storico[0].motivo)
        assertEquals(primo.data, storico[0].dataPrecedente)

        // Il CSV è stato sostituito e rinominato con la nuova data (niente doppioni)
        val nuovoCsv = File(g.csvFileRef!!)
        assertTrue(nuovoCsv.exists())
        assertFalse("Il CSV con la data vecchia non deve restare", vecchioCsv.exists())
        val csvGiornaliere = File(context.getExternalFilesDir(null), "exports").listFiles { f -> f.name.startsWith("Giornaliera_") && f.name.endsWith(".csv") }!!
        assertEquals(1, csvGiornaliere.size)

        // Registro: un file nella cartella Modifiche
        val registro = File(context.getExternalFilesDir(null), "exports/Modifiche").listFiles()!!
        assertEquals(1, registro.size)
        assertTrue(registro[0].readText().contains("Data sbagliata e orario di fine di Paolo"))

        // Torna in sola lettura con lo storico
        assertEquals(ModalitaWizard.DETTAGLIO, vm.uiState.value.modalita)
        assertEquals(1, vm.uiState.value.modifiche.size)
    }

    @Test
    fun annullaModifiche_ripristinaLaGiornalieraInviata() = runBlocking {
        val id = compilaEInvia()
        val vm = viewModel(id)
        vm.richiediModifica()
        aspetta("pin") { vm.uiState.value.pin != null }
        "202900".forEach(vm::pinCifra)
        aspetta("sblocco") { vm.uiState.value.modalita == ModalitaWizard.MODIFICA_INVIATA }
        vm.setNote("nota che non deve restare")
        vm.togglePresente(3)

        vm.esci()
        assertEquals(WizardDialog.EsciModifica, vm.uiState.value.dialog)
        vm.annullaModifiche()
        aspetta("ripristino") { vm.uiState.value.modalita == ModalitaWizard.DETTAGLIO }
        assertEquals("", vm.uiState.value.note)
        assertEquals(setOf(1L, 2L), vm.uiState.value.personaleSelections.keys)
        assertEquals(0, db.giornalieraDao().getById(id)!!.revisione)
    }

    @Test
    fun dataGiaOccupata_vieneRifiutata() = runBlocking {
        val id = compilaEInvia()
        val g = db.giornalieraDao().getById(id)!!
        // Un'altra giornaliera della stessa commessa due giorni prima
        val altra = g.copy(id = 0, data = g.data - 2 * 86_400_000L)
        db.giornalieraDao().insert(altra)

        val vm = viewModel(id)
        vm.richiediModifica()
        aspetta("pin") { vm.uiState.value.pin != null }
        "202900".forEach(vm::pinCifra)
        aspetta("sblocco") { vm.uiState.value.modalita == ModalitaWizard.MODIFICA_INVIATA }
        vm.cambiaData(altra.data)
        aspetta("toast") { vm.uiState.value.toast != null }
        assertTrue(vm.uiState.value.toast!!.startsWith("Esiste già una giornaliera"))
        assertTrue(Formati.stessoGiorno(vm.uiState.value.selectedDate, g.data))
    }
}
