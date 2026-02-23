package com.idroambiente.giornalieradicantiere.services

import android.content.Context
import com.idroambiente.giornalieradicantiere.util.formatCommessaNome
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dati per generare il CSV riepilogo commessa nel formato gestionale
 */
data class RiepilogoCommessaCsvData(
    val commessaNome: String,
    val totaleOreLavoro: Float,
    val totaleOreViaggio: Float,
    val codiceArticoloOreLavoro: String,
    val codiceArticoloOreViaggio: String,
    val mezzi: List<MezzoCsvData>,
    val attrezzature: List<AttrezzaturaCsvData>,
    val startDate: Long? = null,
    val endDate: Long? = null
)

data class MezzoCsvData(
    val codiceArticoloGiorni: String,
    val giorniUtilizzo: Int,
    val codiceArticoloKm: String,
    val totaleKm: Float
)

data class AttrezzaturaCsvData(
    val codiceArticolo: String,
    val giorniUtilizzo: Int
)

@Singleton
class CsvGeneratorService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val SEPARATOR = ";"
        private const val BOM = "\uFEFF"
    }

    /**
     * Genera il CSV per una giornaliera e lo salva nella cache dell'app.
     * Una riga per risorsa (personale/mezzo/attrezzatura).
     */
    fun generateGiornalieraCsv(data: GiornalieraPdfData): File {
        val sb = StringBuilder()
        sb.append(BOM)

        // Header
        sb.appendLine(
            listOf(
                "Data", "Commessa", "Cliente", "Cantiere", "Compilato Da",
                "Tipo", "Nome", "Cognome",
                "Ore Lavoro", "Ore Viaggio Andata", "Ore Viaggio Ritorno",
                "Inizio Turno", "Fine Turno", "Pernotto",
                "Targa", "KM Andata", "KM Ritorno", "KM Totale",
                "Codice Articolo", "Codice Articolo KM", "Codice Articolo Giorni"
            ).joinToString(SEPARATOR)
        )

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY)
        val dataStr = dateFormat.format(Date(data.giornaliera.data))
        val commessaNome = formatCommessaNome(data.commessa.nome)
        val cliente = escapeCsv(data.commessa.cliente)
        val cantiere = escapeCsv(data.cantiere.nome)
        val compilatoDa = escapeCsv(data.userName ?: "")

        // Righe personale
        for (item in data.personaleList) {
            val p = item.personale
            val g = item.giornalieraData
            sb.appendLine(
                listOf(
                    dataStr, commessaNome, cliente, cantiere, compilatoDa,
                    "Personale",
                    escapeCsv(p.nome),
                    escapeCsv(p.cognome),
                    formatDecimal(g.oreLavoro),
                    formatDecimal(g.oreViaggioAndata),
                    formatDecimal(g.oreViaggioRitorno),
                    formatTime(g.inizioTurno),
                    formatTime(g.fineTurno),
                    if (g.pernotto) "Si" else "No",
                    "", "", "", "", "", "", ""
                ).joinToString(SEPARATOR)
            )
        }

        // Righe mezzi
        for (item in data.mezziList) {
            val m = item.mezzo
            val g = item.giornalieraData
            val kmTotale = g.kmAndata + g.kmRitorno
            sb.appendLine(
                listOf(
                    dataStr, commessaNome, cliente, cantiere, compilatoDa,
                    "Mezzo",
                    escapeCsv(m.nome),
                    "",
                    "", "", "", "", "", "",
                    escapeCsv(m.targa),
                    formatDecimal(g.kmAndata),
                    formatDecimal(g.kmRitorno),
                    formatDecimal(kmTotale),
                    "",
                    escapeCsv(m.codiceArticoloKm),
                    escapeCsv(m.codiceArticoloGiorni)
                ).joinToString(SEPARATOR)
            )
        }

        // Righe attrezzature
        for (attrezzatura in data.attrezzatureList) {
            sb.appendLine(
                listOf(
                    dataStr, commessaNome, cliente, cantiere, compilatoDa,
                    "Attrezzatura",
                    escapeCsv(attrezzatura.nome),
                    "",
                    "", "", "", "", "", "",
                    "", "", "", "",
                    escapeCsv(attrezzatura.codiceArticolo),
                    "", ""
                ).joinToString(SEPARATOR)
            )
        }

        val dateFileFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ITALY)
        val dateStr = dateFileFormat.format(Date(data.giornaliera.data))
        val sanitizedCantiere = data.cantiere.nome.replace(Regex("[^a-zA-Z0-9]"), "_")
        val fileName = "Giornaliera_${sanitizedCantiere}_$dateStr.csv"

        val outputFile = File(context.cacheDir, fileName)
        outputFile.writeText(sb.toString(), Charsets.UTF_8)
        return outputFile
    }

    /**
     * Genera il CSV per il riepilogo commessa nel formato gestionale.
     * Colonne: Data;Commessa;Codice Articolo;Quantita
     */
    fun generateRiepilogoCommessaCsv(data: RiepilogoCommessaCsvData): File {
        val sb = StringBuilder()
        sb.append(BOM)

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY)
        val dataStr = dateFormat.format(Date())
        val commessa = escapeCsv(formatCommessaNome(data.commessaNome))

        // Header
        sb.appendLine(listOf("Data", "Commessa", "Codice Articolo", "Quantita").joinToString(SEPARATOR))

        // Riga ore lavorate totali
        if (data.codiceArticoloOreLavoro.isNotBlank() && data.totaleOreLavoro > 0f) {
            sb.appendLine(
                listOf(dataStr, commessa, escapeCsv(data.codiceArticoloOreLavoro), formatDecimal(data.totaleOreLavoro))
                    .joinToString(SEPARATOR)
            )
        }

        // Riga ore viaggio totali
        if (data.codiceArticoloOreViaggio.isNotBlank() && data.totaleOreViaggio > 0f) {
            sb.appendLine(
                listOf(dataStr, commessa, escapeCsv(data.codiceArticoloOreViaggio), formatDecimal(data.totaleOreViaggio))
                    .joinToString(SEPARATOR)
            )
        }

        // Righe mezzi: giorni di utilizzo + km
        for (mezzo in data.mezzi) {
            if (mezzo.codiceArticoloGiorni.isNotBlank() && mezzo.giorniUtilizzo > 0) {
                sb.appendLine(
                    listOf(dataStr, commessa, escapeCsv(mezzo.codiceArticoloGiorni), mezzo.giorniUtilizzo.toString())
                        .joinToString(SEPARATOR)
                )
            }
            if (mezzo.codiceArticoloKm.isNotBlank() && mezzo.totaleKm > 0f) {
                sb.appendLine(
                    listOf(dataStr, commessa, escapeCsv(mezzo.codiceArticoloKm), formatDecimal(mezzo.totaleKm))
                        .joinToString(SEPARATOR)
                )
            }
        }

        // Righe attrezzature: giorni di utilizzo
        for (attr in data.attrezzature) {
            if (attr.codiceArticolo.isNotBlank() && attr.giorniUtilizzo > 0) {
                sb.appendLine(
                    listOf(dataStr, commessa, escapeCsv(attr.codiceArticolo), attr.giorniUtilizzo.toString())
                        .joinToString(SEPARATOR)
                )
            }
        }

        val sanitizedCommessa = formatCommessaNome(data.commessaNome)
            .replace(Regex("[^a-zA-Z0-9\\-]"), "_")
        val fileName = generateRiepilogoFileName(sanitizedCommessa, data.startDate, data.endDate)

        val outputFile = File(context.cacheDir, fileName)
        outputFile.writeText(sb.toString(), Charsets.UTF_8)
        return outputFile
    }

    /**
     * Genera il nome file per il CSV riepilogo.
     */
    fun generateRiepilogoFileName(commessaNome: String, startDate: Long?, endDate: Long?): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ITALY)
        val sanitized = commessaNome
            .replace(Regex("[\\\\/:*?\"<>|]"), "")
            .replace(Regex("\\s+"), "_")
            .trim()
            .take(50)
            .ifEmpty { "Commessa" }
        return if (startDate != null || endDate != null) {
            val from = startDate?.let { dateFormat.format(Date(it)) } ?: "inizio"
            val to = endDate?.let { dateFormat.format(Date(it)) } ?: "fine"
            "Riepilogo_${sanitized}_${from}_${to}.csv"
        } else {
            "Riepilogo_${sanitized}_completo.csv"
        }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(SEPARATOR) || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun formatDecimal(value: Float): String {
        return if (value == 0f) {
            ""
        } else if (value == value.toInt().toFloat()) {
            value.toInt().toString()
        } else {
            String.format(Locale.ITALY, "%.1f", value)
        }
    }

    private fun formatTime(millis: Long): String {
        if (millis <= 0) return ""
        val h = ((millis / 1000 / 60 / 60) % 24).toInt()
        val m = ((millis / 1000 / 60) % 60).toInt()
        return String.format("%02d:%02d", h, m)
    }
}
