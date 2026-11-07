package com.idroambiente.giornalieradicantiere.ui

import com.idroambiente.giornalieradicantiere.database.entities.Commessa
import com.idroambiente.giornalieradicantiere.database.entities.Giornaliera
import com.idroambiente.giornalieradicantiere.database.entities.StatoCommessa
import com.idroambiente.giornalieradicantiere.database.entities.StatoGiornaliera
import com.idroambiente.giornalieradicantiere.ui.commesse.AzioneCommessa
import com.idroambiente.giornalieradicantiere.ui.commesse.FiltroPeriodo
import com.idroambiente.giornalieradicantiere.ui.commesse.Periodi
import com.idroambiente.giornalieradicantiere.ui.commesse.azionePer
import com.idroambiente.giornalieradicantiere.ui.common.Formati
import com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard.MezzoFormData
import com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard.PersonaleFormData
import com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard.TurnoForm
import com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard.problemaInvio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class RegoleTest {
    private fun giorno(d: Int, m: Int = 11, h: Int = 12) = Calendar.getInstance().apply { clear(); set(2026, m - 1, d, h, 0) }.timeInMillis
    private val oggi = giorno(5, h = 18)
    private val aperta = Commessa(1, 1234, "Comune", "", StatoCommessa.APERTA)

    @Test
    fun azioneHome() {
        assertEquals(AzioneCommessa.CompilaOggi, azionePer(aperta, emptyList(), oggi))
        val inviataOggi = Giornaliera(1, 1, giorno(5), StatoGiornaliera.INVIATA)
        assertTrue(azionePer(aperta, listOf(inviataOggi), oggi) is AzioneCommessa.OggiFatta)
        val bozzaIeri = Giornaliera(2, 1, giorno(4), StatoGiornaliera.BOZZA)
        val a = azionePer(aperta, listOf(bozzaIeri), oggi)
        assertTrue(a is AzioneCommessa.RiprendiBozza && !a.oggi)
        // La giornaliera di oggi già inviata vince sulla bozza vecchia
        assertTrue(azionePer(aperta, listOf(bozzaIeri, inviataOggi), oggi) is AzioneCommessa.OggiFatta)
        assertEquals(AzioneCommessa.VediGiornaliere, azionePer(aperta.copy(stato = StatoCommessa.CHIUSA), emptyList(), oggi))
    }

    @Test
    fun filtriPeriodo() {
        assertEquals(FiltroPeriodo.TUTTO, Periodi.filtro(null, null, oggi))
        val (s7, e7) = Periodi.setteGiorni(oggi)
        assertEquals(FiltroPeriodo.SETTE_GIORNI, Periodi.filtro(s7, e7, oggi))
        assertEquals(Formati.inizioGiorno(giorno(30, 10)), s7)
        val (sm, em) = Periodi.meseCorrente(oggi)
        assertEquals(FiltroPeriodo.MESE, Periodi.filtro(sm, em, oggi))
        assertEquals(Formati.inizioGiorno(giorno(30)), em)
        assertEquals(FiltroPeriodo.PERIODO, Periodi.filtro(giorno(1), giorno(3), oggi))
    }

    @Test
    fun moduloOrari() {
        val f = TurnoForm(inizio = 7 * 60 + 30, fine = 16 * 60 + 30, pausa = 60, andata = 30, ritorno = 45, pernotto = true)
        assertEquals(8 * 60, f.lavoroMinuti)
        val d = f.toFormData()
        assertEquals(8f, d.oreLavoro)
        assertEquals(0.75f, d.oreViaggioRitorno)
        assertEquals(f, TurnoForm.da(d))
        assertEquals("L'orario di fine deve essere dopo l'inizio", f.copy(fine = 7 * 60).errore())
        assertEquals("La pausa deve essere inferiore alle ore di lavoro", f.copy(pausa = 5 * 60).errore())
        assertNull(f.errore())
    }

    @Test
    fun controlliPrimaDellInvio() {
        val ok = TurnoForm().toFormData()
        val nome = { id: Long -> "P$id" }
        assertEquals("Seleziona almeno una persona", problemaInvio(emptyMap(), emptyMap(), nome, nome, false, "")?.messaggio)
        assertEquals("Mancano i dati di P2", problemaInvio(mapOf(1L to ok, 2L to PersonaleFormData(true)), emptyMap(), nome, nome, false, "")?.messaggio)
        // I mezzi restano obbligatori, come nell'app di prima
        assertEquals(1, problemaInvio(mapOf(1L to ok), emptyMap(), nome, nome, false, "")?.step)
        assertEquals("Mancano i km di P3", problemaInvio(mapOf(1L to ok), mapOf(3L to MezzoFormData(true)), nome, nome, false, "")?.messaggio)
        val mezzi = mapOf(3L to MezzoFormData(true, 10f, 10f))
        assertNull(problemaInvio(mapOf(1L to ok), mezzi, nome, nome, false, ""))
        // Nel reinvio serve il motivo
        assertEquals("Scrivi il motivo della modifica", problemaInvio(mapOf(1L to ok), mezzi, nome, nome, true, "  ")?.messaggio)
        assertNull(problemaInvio(mapOf(1L to ok), mezzi, nome, nome, true, "Orario corretto"))
    }
}
