package com.idroambiente.giornalieradicantiere.services

import com.idroambiente.giornalieradicantiere.util.formatCommessaNome
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Una riga del registro modifiche. */
data class VoceRegistro(
    val modificataAt: Long,
    val commessaNome: Int,
    val cliente: String,
    val dataOriginale: Long,
    val dataNuova: Long,
    val revisione: Int,
    val motivo: String,
    val filePdf: String?,
    val fileCsv: String?,
    val reportAggiornati: List<String>,
    val reportNonAggiornati: List<String>
)

/**
 * Registro delle modifiche per l'ufficio: cartella "Modifiche", UN FILE PER MODIFICA.
 * Ogni file viene solo creato, mai riscritto: niente righe perse se due telefoni
 * modificano insieme, e funziona con qualsiasi account Google.
 */
@Singleton
class RegistroModifiche @Inject constructor(
    private val fileStorageService: FileStorageService
) {
    companion object {
        private const val SEP = ";"
        const val INTESTAZIONE =
            "modificata_il;commessa;cliente;data_originale;data_nuova;revisione;motivo;file_pdf;file_csv;report_aggiornati;report_da_rigenerare"

        fun contenuto(voce: VoceRegistro): String {
            val dataOra = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY)
            val giorno = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY)
            val riga = listOf(
                dataOra.format(Date(voce.modificataAt)),
                formatCommessaNome(voce.commessaNome),
                voce.cliente,
                giorno.format(Date(voce.dataOriginale)),
                giorno.format(Date(voce.dataNuova)),
                voce.revisione.toString(),
                voce.motivo,
                voce.filePdf ?: "",
                voce.fileCsv ?: "",
                voce.reportAggiornati.joinToString(", "),
                voce.reportNonAggiornati.joinToString(", ")
            ).joinToString(SEP) { escape(it) }
            return INTESTAZIONE + "\n" + riga + "\n"
        }

        private fun escape(value: String): String =
            if (value.contains(SEP) || value.contains('"') || value.contains('\n')) {
                "\"" + value.replace("\"", "\"\"").replace('\n', ' ') + "\""
            } else value
    }

    /** @return dove è stato scritto (per mostrarlo), oppure l'errore */
    suspend fun scrivi(voce: VoceRegistro): Result<String> {
        val nome = FileNaming.registroModifica(
            voce.modificataAt,
            formatCommessaNome(voce.commessaNome),
            voce.dataNuova,
            voce.revisione
        )
        return fileStorageService.scriviRegistro(nome, contenuto(voce))
    }
}
