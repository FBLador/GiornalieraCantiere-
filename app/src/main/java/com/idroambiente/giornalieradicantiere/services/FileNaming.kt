package com.idroambiente.giornalieradicantiere.services

import com.idroambiente.giornalieradicantiere.database.entities.ArchivioFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Nomi dei file prodotti dall'app, in un solo posto.
 *
 * I nomi sono IDENTICI a quelli usati finora (anche dove sono diversi tra Drive
 * e telefono): servono per ritrovare i file delle giornaliere inviate prima
 * di questa versione.
 */
object FileNaming {

    private fun giorno(data: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.ITALY).format(Date(data))

    private val caratteriVietati = Regex("[\\\\/:*?\"<>|]")

    /** Come FileStorageService (salvataggio sul telefono). */
    private fun pulisciLocale(nome: String): String = nome
        .replace(caratteriVietati, "")
        .replace(Regex("\\s+"), "_")
        .trim()
        .take(50)
        .ifEmpty { "Commessa" }

    /** Come GoogleDriveService.sanitizeFolderName. */
    private fun pulisciDrive(nome: String): String = nome
        .replace(caratteriVietati, "")
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(100)
        .ifEmpty { "Commessa" }

    /** Come PdfGeneratorService / CsvGeneratorService. */
    private fun pulisciCantiere(nome: String): String = nome.replace(Regex("[^a-zA-Z0-9]"), "_")

    fun pdfGiornaliera(archivio: ArchivioFile, commessaNome: String, cantiereNome: String, data: Long): String =
        when (archivio) {
            ArchivioFile.DRIVE -> "${pulisciDrive(commessaNome)}_${giorno(data)}.pdf"
            ArchivioFile.LOCALE -> "${pulisciLocale(commessaNome)}_${giorno(data)}.pdf"
            ArchivioFile.APP, ArchivioFile.CONDIVISO -> "Giornaliera_${pulisciCantiere(cantiereNome)}_${giorno(data)}.pdf"
        }

    fun csvGiornaliera(archivio: ArchivioFile, commessaNome: String, cantiereNome: String, data: Long): String =
        when (archivio) {
            ArchivioFile.LOCALE -> "${pulisciLocale(commessaNome)}_${giorno(data)}.csv"
            ArchivioFile.DRIVE, ArchivioFile.APP, ArchivioFile.CONDIVISO ->
                "Giornaliera_${pulisciCantiere(cantiereNome)}_${giorno(data)}.csv"
        }

    /** "0001234_2026-11-04.pdf" → "0001234_2026-11-04_rev2.pdf" */
    fun conRevisione(nome: String, revisione: Int): String {
        val punto = nome.lastIndexOf('.')
        return if (punto <= 0) "${nome}_rev$revisione" else "${nome.substring(0, punto)}_rev$revisione${nome.substring(punto)}"
    }

    /** File del registro modifiche: uno per ogni modifica, in ordine di data. */
    fun registroModifica(modificataAt: Long, commessaNome: String, dataGiornaliera: Long, revisione: Int): String {
        val quando = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ITALY).format(Date(modificataAt))
        return "${quando}_${pulisciLocale(commessaNome)}_${giorno(dataGiornaliera)}_rev$revisione.csv"
    }

    fun senzaEstensione(nome: String): String = nome.substringBeforeLast('.', nome)
}
