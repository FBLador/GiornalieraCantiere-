package com.idroambiente.giornalieradicantiere.services

import com.idroambiente.giornalieradicantiere.data.preferences.PinHasher
import com.idroambiente.giornalieradicantiere.data.preferences.PinPolicy
import com.idroambiente.giornalieradicantiere.data.preferences.PinPredefinito
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinTest {

    @Test
    fun pinPredefinito202900() {
        assertTrue(PinHasher.verifica("202900", PinPredefinito.SALE, PinPredefinito.IMPRONTA))
        assertFalse(PinHasher.verifica("123456", PinPredefinito.SALE, PinPredefinito.IMPRONTA))
        assertFalse(PinHasher.verifica("202901", PinPredefinito.SALE, PinPredefinito.IMPRONTA))
    }

    @Test
    fun nuovoPin_verificabile() {
        val sale = PinHasher.nuovoSale()
        val imp = PinHasher.b64(PinHasher.impronta("481516", sale))
        assertTrue(PinHasher.verifica("481516", PinHasher.b64(sale), imp))
        assertFalse(PinHasher.verifica("202900", PinHasher.b64(sale), imp))
    }

    @Test
    fun cinqueErrori_bloccoDiCinqueMinuti() {
        var stato = PinPolicy.Stato(0, 0)
        val t0 = 1_000_000L
        for (i in 1..4) {
            val (esito, nuovo) = PinPolicy.valuta(stato, corretto = false, adesso = t0)
            assertEquals(PinPolicy.Esito.Errato(5 - i), esito)
            stato = nuovo
        }
        val (quinto, bloccato) = PinPolicy.valuta(stato, corretto = false, adesso = t0)
        assertTrue(quinto is PinPolicy.Esito.Bloccato)
        // Anche il PIN giusto viene rifiutato durante il blocco
        val (durante, _) = PinPolicy.valuta(bloccato, corretto = true, adesso = t0 + 60_000)
        assertTrue(durante is PinPolicy.Esito.Bloccato)
        // Dopo 5 minuti si può riprovare
        val (dopo, azzerato) = PinPolicy.valuta(bloccato, corretto = true, adesso = t0 + PinPolicy.BLOCCO_MS + 1)
        assertEquals(PinPolicy.Esito.Corretto, dopo)
        assertEquals(PinPolicy.Stato(0, 0), azzerato)
    }

    @Test
    fun pinGiusto_azzeraGliErrori() {
        val (_, dopoErrore) = PinPolicy.valuta(PinPolicy.Stato(0, 0), corretto = false, adesso = 0)
        assertEquals(1, dopoErrore.tentativiErrati)
        val (esito, stato) = PinPolicy.valuta(dopoErrore, corretto = true, adesso = 0)
        assertEquals(PinPolicy.Esito.Corretto, esito)
        assertEquals(0, stato.tentativiErrati)
    }
}
