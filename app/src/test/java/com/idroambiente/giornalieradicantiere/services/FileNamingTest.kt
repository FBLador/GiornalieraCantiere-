package com.idroambiente.giornalieradicantiere.services

import com.idroambiente.giornalieradicantiere.database.entities.ArchivioFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class FileNamingTest {
    private fun giorno(y: Int, m: Int, d: Int): Long = Calendar.getInstance().apply {
        clear(); set(y, m - 1, d, 12, 0)
    }.timeInMillis

    @Test
    fun nomiUgualiAQuelliDiPrima() {
        val d = giorno(2026, 11, 4)
        assertEquals("0001234_2026-11-04.pdf", FileNaming.pdfGiornaliera(ArchivioFile.DRIVE, "0001234", "0001234", d))
        assertEquals("0001234_2026-11-04.pdf", FileNaming.pdfGiornaliera(ArchivioFile.LOCALE, "0001234", "0001234", d))
        assertEquals("Giornaliera_0001234_2026-11-04.csv", FileNaming.csvGiornaliera(ArchivioFile.DRIVE, "0001234", "0001234", d))
        assertEquals("0001234_2026-11-04.csv", FileNaming.csvGiornaliera(ArchivioFile.LOCALE, "0001234", "0001234", d))
        assertEquals("Giornaliera_0001234_2026-11-04.pdf", FileNaming.pdfGiornaliera(ArchivioFile.APP, "0001234", "0001234", d))
    }

    @Test
    fun revisioneNelNome() {
        assertEquals("0001234_2026-11-04_rev2.pdf", FileNaming.conRevisione("0001234_2026-11-04.pdf", 2))
        assertEquals("senza_rev1", FileNaming.conRevisione("senza", 1))
    }

    @Test
    fun registro_unFilePerModifica() {
        val n = FileNaming.registroModifica(giorno(2026, 11, 5), "0001234", giorno(2026, 11, 2), 2)
        assertTrue(n.startsWith("2026-11-05_12-00-00_0001234_2026-11-02_rev2"))
        assertTrue(n.endsWith(".csv"))
    }

    @Test
    fun copertureReport() {
        val d = giorno(2026, 11, 4)
        assertTrue(ReportCoverage.contiene(null, null, d)) // completo
        assertTrue(ReportCoverage.contiene(giorno(2026, 11, 1), giorno(2026, 11, 4) - 12 * 3_600_000L, d)) // fine inclusa
        assertFalse(ReportCoverage.contiene(giorno(2026, 11, 5), null, d))
        assertFalse(ReportCoverage.contiene(null, giorno(2026, 11, 3) - 12 * 3_600_000L, d))
        assertTrue(ReportCoverage.contieneAlmenoUna(giorno(2026, 11, 1), giorno(2026, 11, 2), listOf(giorno(2026, 11, 2), d)))
    }

    @Test
    fun registro_contenutoCsv() {
        val csv = RegistroModifiche.contenuto(
            VoceRegistro(
                modificataAt = giorno(2026, 11, 5),
                commessaNome = 1234,
                cliente = "Comune di Bergamo",
                dataOriginale = giorno(2026, 11, 4),
                dataNuova = giorno(2026, 11, 2),
                revisione = 2,
                motivo = "Data sbagliata; era lunedì",
                filePdf = "0001234_2026-11-02.pdf",
                fileCsv = null,
                reportAggiornati = listOf("Riepilogo_0001234_completo.pdf"),
                reportNonAggiornati = emptyList()
            )
        )
        val righe = csv.trim().lines()
        assertEquals(RegistroModifiche.INTESTAZIONE, righe[0])
        assertTrue(righe[1].startsWith("05/11/2026 12:00;0001234;Comune di Bergamo;04/11/2026;02/11/2026;2;\"Data sbagliata; era lunedì\""))
    }
}
