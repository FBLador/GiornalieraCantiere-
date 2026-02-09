package com.idroambiente.giornalieradicantiere.services

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.idroambiente.giornalieradicantiere.database.entities.Attrezzatura
import com.idroambiente.giornalieradicantiere.database.entities.Cantiere
import com.idroambiente.giornalieradicantiere.database.entities.Commessa
import com.idroambiente.giornalieradicantiere.database.entities.Giornaliera
import com.idroambiente.giornalieradicantiere.database.entities.Mezzo
import com.idroambiente.giornalieradicantiere.database.entities.MezzoGiornaliera
import com.idroambiente.giornalieradicantiere.database.entities.Personale
import com.idroambiente.giornalieradicantiere.database.entities.PersonaleGiornaliera
import com.idroambiente.giornalieradicantiere.util.formatCommessaNome
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dati aggregati per il personale presente in una giornaliera
 */
data class PersonaleWithData(
    val personale: Personale,
    val giornalieraData: PersonaleGiornaliera
)

/**
 * Dati aggregati per i mezzi utilizzati in una giornaliera
 */
data class MezzoWithData(
    val mezzo: Mezzo,
    val giornalieraData: MezzoGiornaliera
)

/**
 * Dati di riepilogo per il PDF
 */
data class PdfRiepilogoData(
    val totalePersonePresenti: Int = 0,
    val totaleOreLavoro: Float = 0f,
    val totaleOreViaggio: Float = 0f,
    val totalePernotti: Int = 0,
    val totaleMezziUtilizzati: Int = 0,
    val totaleKm: Float = 0f,
    val totaleAttrezzatureUtilizzate: Int = 0
)

/**
 * Dati di una singola giornaliera per il riepilogo commessa
 */
data class RiepilogoGiornalieraItem(
    val data: Long,
    val stato: String
)

/**
 * Dati necessari per generare il PDF del riepilogo commessa
 */
data class RiepilogoCommessaPdfData(
    val commessaNome: String,
    val cliente: String,
    val indirizzo: String,
    val stato: String,
    val totaleOreLavoro: Float,
    val totaleOreViaggio: Float,
    val totaleKm: Float,
    val giornaliere: List<RiepilogoGiornalieraItem>,
    val startDate: Long? = null,
    val endDate: Long? = null
)

/**
 * Tutti i dati necessari per generare il PDF di una giornaliera
 */
data class GiornalieraPdfData(
    val giornaliera: Giornaliera,
    val cantiere: Cantiere,
    val commessa: Commessa,
    val personaleList: List<PersonaleWithData>,
    val mezziList: List<MezzoWithData>,
    val attrezzatureList: List<Attrezzatura>,
    val riepilogo: PdfRiepilogoData,
    val userName: String? = null
)

@Singleton
class PdfGeneratorService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        // Dimensioni pagina A4 in punti (72 punti per pollice)
        private const val PAGE_WIDTH = 595 // A4 width
        private const val PAGE_HEIGHT = 842 // A4 height
        private const val MARGIN = 40f
        private const val LINE_HEIGHT = 18f
        private const val SECTION_SPACING = 20f
    }

    private val titlePaint = Paint().apply {
        color = Color.parseColor("#00527E") // Blu primario
        textSize = 24f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }

    private val headerPaint = Paint().apply {
        color = Color.parseColor("#424242")
        textSize = 14f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.parseColor("#212121")
        textSize = 11f
        typeface = Typeface.DEFAULT
        isAntiAlias = true
    }

    private val smallTextPaint = Paint().apply {
        color = Color.parseColor("#757575")
        textSize = 10f
        typeface = Typeface.DEFAULT
        isAntiAlias = true
    }

    private val tablePaint = Paint().apply {
        color = Color.parseColor("#E0E0E0")
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    private val tableHeaderBgPaint = Paint().apply {
        color = Color.parseColor("#E3F2FD")
        style = Paint.Style.FILL
    }

    private val checkboxPaint = Paint().apply {
        color = Color.parseColor("#00527E")
        textSize = 12f
        isAntiAlias = true
    }

    /**
     * Helper class per gestire lo stato del PDF durante la generazione
     */
    private class PdfState(
        val document: PdfDocument,
        var currentPage: PdfDocument.Page? = null,
        var canvas: Canvas? = null,
        var yPosition: Float = MARGIN,
        var pageNumber: Int = 1
    ) {
        fun startNewPage(): Canvas {
            currentPage?.let { document.finishPage(it) }
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            currentPage = document.startPage(pageInfo)
            pageNumber++
            yPosition = MARGIN
            canvas = currentPage!!.canvas
            return canvas!!
        }

        fun checkNewPage(requiredSpace: Float): Canvas {
            if (yPosition + requiredSpace > PAGE_HEIGHT - MARGIN) {
                return startNewPage()
            }
            return canvas!!
        }

        fun finalize() {
            currentPage?.let { document.finishPage(it) }
        }
    }

    /**
     * Genera il PDF per una giornaliera e lo salva nella cache dell'app
     * @return Il file PDF generato
     */
    suspend fun generateGiornalieraPdf(data: GiornalieraPdfData): File = withContext(Dispatchers.IO) {
        val state = PdfState(PdfDocument())
        
        state.startNewPage()

        // Intestazione
        state.yPosition = drawHeader(state.canvas!!, data, state.yPosition)
        state.yPosition += SECTION_SPACING

        // Info Commessa/Cantiere
        state.yPosition = drawCommessaInfo(state.canvas!!, data, state.yPosition)
        state.yPosition += SECTION_SPACING

        // Sezione Personale
        state.checkNewPage(100f)
        state.yPosition = drawPersonaleSection(state, data.personaleList, state.yPosition)
        state.yPosition += SECTION_SPACING

        // Sezione Mezzi
        state.checkNewPage(100f)
        state.yPosition = drawMezziSection(state, data.mezziList, state.yPosition)
        state.yPosition += SECTION_SPACING

        // Sezione Attrezzature
        state.checkNewPage(80f)
        state.yPosition = drawAttrezzatureSection(state.canvas!!, data.attrezzatureList, state.yPosition)
        state.yPosition += SECTION_SPACING

        // Riepilogo
        state.checkNewPage(120f)
        state.yPosition = drawRiepilogoSection(state.canvas!!, data.riepilogo, state.yPosition)
        state.yPosition += SECTION_SPACING

        // Note
        if (!data.giornaliera.note.isNullOrBlank()) {
            state.checkNewPage(80f)
            state.yPosition = drawNoteSection(state.canvas!!, data.giornaliera.note, state.yPosition)
            state.yPosition += SECTION_SPACING
        }

        // Footer (DPI, POS, timestamp)
        state.checkNewPage(60f)
        drawFooter(state.canvas!!, data.giornaliera, state.yPosition)

        // Finalizza
        state.finalize()

        // Salva il file
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ITALY)
        val dateStr = dateFormat.format(Date(data.giornaliera.data))
        val sanitizedCantiere = data.cantiere.nome.replace(Regex("[^a-zA-Z0-9]"), "_")
        val fileName = "Giornaliera_${sanitizedCantiere}_$dateStr.pdf"
        
        val outputFile = File(context.cacheDir, fileName)
        FileOutputStream(outputFile).use { out ->
            state.document.writeTo(out)
        }
        state.document.close()

        outputFile
    }

    private fun drawHeader(canvas: Canvas, data: GiornalieraPdfData, startY: Float): Float {
        var y = startY

        // Titolo
        canvas.drawText("RAPPORTO GIORNALIERO", MARGIN, y + 24f, titlePaint)
        y += 35f

        // Data
        val dateFormat = SimpleDateFormat("EEEE dd MMMM yyyy", Locale.ITALY)
        val dateStr = dateFormat.format(Date(data.giornaliera.data))
        canvas.drawText("Data: $dateStr", MARGIN, y + 14f, headerPaint)
        y += 25f

        // Compilato da
        if (!data.userName.isNullOrBlank()) {
            canvas.drawText("Compilato da: ${data.userName}", MARGIN, y + 11f, textPaint)
            y += LINE_HEIGHT
        }

        // Linea separatrice
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, tablePaint)
        y += 10f

        return y
    }

    private fun drawCommessaInfo(canvas: Canvas, data: GiornalieraPdfData, startY: Float): Float {
        var y = startY

        canvas.drawText("INFORMAZIONI COMMESSA", MARGIN, y + 14f, headerPaint)
        y += 25f

        val infoLines = listOf(
            "Commessa: ${formatCommessaNome(data.commessa.nome)}",
            "Cliente: ${data.commessa.cliente}",
            "Cantiere: ${data.cantiere.nome}",
            //"Indirizzo: ${data.cantiere.indirizzo}"
        )

        infoLines.forEach { line ->
            canvas.drawText(line, MARGIN, y + 11f, textPaint)
            y += LINE_HEIGHT
        }

        return y
    }

    private fun drawPersonaleSection(
        state: PdfState,
        personaleList: List<PersonaleWithData>,
        startY: Float
    ): Float {
        var y = startY

        state.canvas!!.drawText("PERSONALE PRESENTE", MARGIN, y + 14f, headerPaint)
        y += 25f

        if (personaleList.isEmpty()) {
            state.canvas!!.drawText("Nessuna persona presente", MARGIN, y + 11f, smallTextPaint)
            return y + LINE_HEIGHT
        }

        // Header tabella
        val colWidths = floatArrayOf(150f, 80f, 80f, 100f, 60f)
        val headers = arrayOf("Nome", "Ore Lavoro", "Ore Viaggio", "Turno", "Pernotto")
        
        // Sfondo header
        state.canvas!!.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 20f, tableHeaderBgPaint)
        
        var x = MARGIN + 5f
        headers.forEachIndexed { index, header ->
            state.canvas!!.drawText(header, x, y + 14f, smallTextPaint)
            x += colWidths[index]
        }
        y += 22f

        // Righe
        personaleList.forEach { item ->
            state.yPosition = y
            state.checkNewPage(25f)
            y = state.yPosition
            
            val nome = "${item.personale.nome} ${item.personale.cognome}"
            val oreLavoro = formatHours(item.giornalieraData.oreLavoro)
            val oreViaggio = formatHours(item.giornalieraData.oreViaggioAndata + item.giornalieraData.oreViaggioRitorno)
            val turno = if (item.giornalieraData.inizioTurno > 0 && item.giornalieraData.fineTurno > 0) {
                "${formatTime(item.giornalieraData.inizioTurno)} - ${formatTime(item.giornalieraData.fineTurno)}"
            } else "-"
            val pernotto = if (item.giornalieraData.pernotto) "Si" else "No"

            x = MARGIN + 5f
            val nomeHeight = drawWrappedText(state.canvas!!, nome, x, y, colWidths[0] - 10f, textPaint)
            x += colWidths[0]
            state.canvas!!.drawText(oreLavoro, x, y + 11f, textPaint)
            x += colWidths[1]
            state.canvas!!.drawText(oreViaggio, x, y + 11f, textPaint)
            x += colWidths[2]
            state.canvas!!.drawText(turno, x, y + 11f, textPaint)
            x += colWidths[3]
            state.canvas!!.drawText(pernotto, x, y + 11f, textPaint)

            y += maxOf(LINE_HEIGHT, nomeHeight)
        }

        return y
    }

    private fun drawMezziSection(
        state: PdfState,
        mezziList: List<MezzoWithData>,
        startY: Float
    ): Float {
        var y = startY

        state.canvas!!.drawText("MEZZI UTILIZZATI", MARGIN, y + 14f, headerPaint)
        y += 25f

        if (mezziList.isEmpty()) {
            state.canvas!!.drawText("Nessun mezzo utilizzato", MARGIN, y + 11f, smallTextPaint)
            return y + LINE_HEIGHT
        }

        // Header tabella
        val colWidths = floatArrayOf(150f, 100f, 80f, 80f, 80f)
        val headers = arrayOf("Mezzo", "Targa", "KM Andata", "KM Ritorno", "Totale")
        
        // Sfondo header
        state.canvas!!.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 20f, tableHeaderBgPaint)
        
        var x = MARGIN + 5f
        headers.forEachIndexed { index, header ->
            state.canvas!!.drawText(header, x, y + 14f, smallTextPaint)
            x += colWidths[index]
        }
        y += 22f

        // Righe
        mezziList.forEach { item ->
            state.yPosition = y
            state.checkNewPage(25f)
            y = state.yPosition
            
            val totaleKm = item.giornalieraData.kmAndata + item.giornalieraData.kmRitorno

            x = MARGIN + 5f
            val nomeHeight = drawWrappedText(state.canvas!!, item.mezzo.nome, x, y, colWidths[0] - 10f, textPaint)
            x += colWidths[0]
            state.canvas!!.drawText(item.mezzo.targa, x, y + 11f, textPaint)
            x += colWidths[1]
            state.canvas!!.drawText(formatKm(item.giornalieraData.kmAndata), x, y + 11f, textPaint)
            x += colWidths[2]
            state.canvas!!.drawText(formatKm(item.giornalieraData.kmRitorno), x, y + 11f, textPaint)
            x += colWidths[3]
            state.canvas!!.drawText(formatKm(totaleKm), x, y + 11f, textPaint)

            y += maxOf(LINE_HEIGHT, nomeHeight)
        }

        return y
    }

    private fun drawAttrezzatureSection(canvas: Canvas, attrezzatureList: List<Attrezzatura>, startY: Float): Float {
        var y = startY

        canvas.drawText("ATTREZZATURE UTILIZZATE", MARGIN, y + 14f, headerPaint)
        y += 25f

        if (attrezzatureList.isEmpty()) {
            canvas.drawText("Nessuna attrezzatura utilizzata", MARGIN, y + 11f, smallTextPaint)
            return y + LINE_HEIGHT
        }

        attrezzatureList.forEach { attrezzatura ->
            val h = drawWrappedText(canvas, "• ${attrezzatura.nome}", MARGIN + 10f, y, PAGE_WIDTH - MARGIN - (MARGIN + 10f), textPaint)
            y += maxOf(LINE_HEIGHT, h)
        }

        return y
    }

    private fun drawRiepilogoSection(canvas: Canvas, riepilogo: PdfRiepilogoData, startY: Float): Float {
        var y = startY

        canvas.drawText("RIEPILOGO", MARGIN, y + 14f, headerPaint)
        y += 25f

        // Box riepilogo
        val boxHeight = 80f
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + boxHeight, tableHeaderBgPaint)
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + boxHeight, tablePaint)

        y += 20f
        val col1X = MARGIN + 20f
        val col2X = MARGIN + 200f
        val col3X = MARGIN + 380f

        // Riga 1
        canvas.drawText("Persone presenti: ${riepilogo.totalePersonePresenti}", col1X, y, textPaint)
        canvas.drawText("Ore lavoro totali: ${formatHours(riepilogo.totaleOreLavoro)}", col2X, y, textPaint)
        canvas.drawText("Pernotti: ${riepilogo.totalePernotti}", col3X, y, textPaint)
        y += 20f

        // Riga 2
        canvas.drawText("Mezzi utilizzati: ${riepilogo.totaleMezziUtilizzati}", col1X, y, textPaint)
        canvas.drawText("Ore viaggio totali: ${formatHours(riepilogo.totaleOreViaggio)}", col2X, y, textPaint)
        canvas.drawText("KM totali: ${formatKm(riepilogo.totaleKm)}", col3X, y, textPaint)
        y += 20f

        // Riga 3
        canvas.drawText("Attrezzature: ${riepilogo.totaleAttrezzatureUtilizzate}", col1X, y, textPaint)

        return startY + 25f + boxHeight + 10f
    }

    private fun drawNoteSection(canvas: Canvas, note: String, startY: Float): Float {
        var y = startY

        canvas.drawText("NOTE", MARGIN, y + 14f, headerPaint)
        y += 25f

        // Wrap text
        val maxWidth = PAGE_WIDTH - 2 * MARGIN
        val words = note.split(" ")
        var currentLine = ""
        
        words.forEach { word ->
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val textWidth = textPaint.measureText(testLine)
            
            if (textWidth > maxWidth) {
                canvas.drawText(currentLine, MARGIN, y + 11f, textPaint)
                y += LINE_HEIGHT
                currentLine = word
            } else {
                currentLine = testLine
            }
        }
        
        if (currentLine.isNotEmpty()) {
            canvas.drawText(currentLine, MARGIN, y + 11f, textPaint)
            y += LINE_HEIGHT
        }

        return y
    }

    private fun drawFooter(canvas: Canvas, giornaliera: Giornaliera, startY: Float): Float {
        var y = startY

        // Linea separatrice
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, tablePaint)
        y += 15f

        // Checkbox POS
        val posCheck = if (giornaliera.presaVisionePOS) "☑" else "☐"
        canvas.drawText("$posCheck Presa visione POS", MARGIN, y + 12f, checkboxPaint)
        y += 25f

        // Timestamp generazione
        val timestampFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY)
        val timestamp = timestampFormat.format(Date())
        canvas.drawText("Documento generato il: $timestamp", MARGIN, y + 10f, smallTextPaint)

        return y + LINE_HEIGHT
    }

    private fun formatHours(hours: Float): String {
        return if (hours == hours.toInt().toFloat()) {
            "${hours.toInt()}h"
        } else {
            String.format(Locale.ITALY, "%.1fh", hours)
        }
    }

    private fun formatKm(km: Float): String {
        return if (km == km.toInt().toFloat()) {
            "${km.toInt()} km"
        } else {
            String.format(Locale.ITALY, "%.1f km", km)
        }
    }

    private fun formatTime(millis: Long): String {
        val h = ((millis / 1000 / 60 / 60) % 24).toInt()
        val m = ((millis / 1000 / 60) % 60).toInt()
        return String.format("%02d:%02d", h, m)
    }

    /**
     * Disegna testo con word-wrap dentro una larghezza massima.
     * @return l'altezza totale occupata dal testo (LINE_HEIGHT * numero di righe)
     */
    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        x: Float,
        startY: Float,
        maxWidth: Float,
        paint: Paint
    ): Float {
        if (paint.measureText(text) <= maxWidth) {
            canvas.drawText(text, x, startY + 11f, paint)
            return LINE_HEIGHT
        }

        var y = startY
        val words = text.split(" ")
        var currentLine = ""

        words.forEach { word ->
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) > maxWidth && currentLine.isNotEmpty()) {
                canvas.drawText(currentLine, x, y + 11f, paint)
                y += LINE_HEIGHT
                currentLine = word
            } else {
                currentLine = testLine
            }
        }
        if (currentLine.isNotEmpty()) {
            canvas.drawText(currentLine, x, y + 11f, paint)
            y += LINE_HEIGHT
        }

        return y - startY
    }

    /**
     * Genera il PDF di riepilogo commessa e lo salva nella cache dell'app
     * @return Il file PDF generato
     */
    suspend fun generateRiepilogoCommessaPdf(data: RiepilogoCommessaPdfData): File = withContext(Dispatchers.IO) {
        val state = PdfState(PdfDocument())

        state.startNewPage()

        // Titolo
        state.yPosition = drawRiepilogoHeader(state.canvas!!, data, state.yPosition)
        state.yPosition += SECTION_SPACING

        // Info Commessa
        state.yPosition = drawRiepilogoCommessaInfo(state.canvas!!, data, state.yPosition)
        state.yPosition += SECTION_SPACING

        // Box Totali
        state.checkNewPage(120f)
        state.yPosition = drawRiepilogoTotali(state.canvas!!, data, state.yPosition)
        state.yPosition += SECTION_SPACING

        // Tabella Giornaliere
        state.checkNewPage(80f)
        state.yPosition = drawRiepilogoGiornaliereTable(state, data.giornaliere, state.yPosition)
        state.yPosition += SECTION_SPACING

        // Footer
        state.checkNewPage(40f)
        drawRiepilogoFooter(state.canvas!!, state.yPosition)

        // Finalizza
        state.finalize()

        // Genera il nome file
        val sanitizedCommessa = formatCommessaNome(data.commessaNome)
            .replace(Regex("[^a-zA-Z0-9\\-]"), "_")
        val fileName = generateRiepilogoFileName(sanitizedCommessa, data.startDate, data.endDate)

        val outputFile = File(context.cacheDir, fileName)
        FileOutputStream(outputFile).use { out ->
            state.document.writeTo(out)
        }
        state.document.close()

        outputFile
    }

    /**
     * Genera il nome file per il riepilogo
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
            "Riepilogo_${sanitized}_${from}_${to}.pdf"
        } else {
            "Riepilogo_${sanitized}_completo.pdf"
        }
    }

    private fun drawRiepilogoHeader(canvas: Canvas, data: RiepilogoCommessaPdfData, startY: Float): Float {
        var y = startY

        canvas.drawText("RIEPILOGO COMMESSA", MARGIN, y + 24f, titlePaint)
        y += 35f

        // Periodo
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY)
        val periodoText = if (data.startDate != null || data.endDate != null) {
            val from = data.startDate?.let { dateFormat.format(Date(it)) } ?: "inizio"
            val to = data.endDate?.let { dateFormat.format(Date(it)) } ?: "oggi"
            "Periodo: $from - $to"
        } else {
            "Periodo: completo"
        }
        canvas.drawText(periodoText, MARGIN, y + 14f, headerPaint)
        y += 25f

        // Linea separatrice
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, tablePaint)
        y += 10f

        return y
    }

    private fun drawRiepilogoCommessaInfo(canvas: Canvas, data: RiepilogoCommessaPdfData, startY: Float): Float {
        var y = startY

        canvas.drawText("INFORMAZIONI COMMESSA", MARGIN, y + 14f, headerPaint)
        y += 25f

        val infoLines = mutableListOf(
            "Commessa: ${formatCommessaNome(data.commessaNome)}",
            "Cliente: ${data.cliente}",
        )
        if (data.indirizzo.isNotBlank()) {
            infoLines.add("Indirizzo: ${data.indirizzo}")
        }
        infoLines.add("Stato: ${data.stato}")

        infoLines.forEach { line ->
            canvas.drawText(line, MARGIN, y + 11f, textPaint)
            y += LINE_HEIGHT
        }

        return y
    }

    private fun drawRiepilogoTotali(canvas: Canvas, data: RiepilogoCommessaPdfData, startY: Float): Float {
        var y = startY

        canvas.drawText("TOTALI", MARGIN, y + 14f, headerPaint)
        y += 25f

        val boxHeight = 60f
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + boxHeight, tableHeaderBgPaint)
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + boxHeight, tablePaint)

        y += 20f
        val col1X = MARGIN + 20f
        val col2X = MARGIN + 200f
        val col3X = MARGIN + 380f

        canvas.drawText("Ore lavoro: ${formatHours(data.totaleOreLavoro)}", col1X, y, textPaint)
        canvas.drawText("Ore viaggio: ${formatHours(data.totaleOreViaggio)}", col2X, y, textPaint)
        canvas.drawText("KM totali: ${formatKm(data.totaleKm)}", col3X, y, textPaint)
        y += 20f

        canvas.drawText("Totale giornaliere: ${data.giornaliere.size}", col1X, y, textPaint)

        return startY + 25f + boxHeight + 10f
    }

    private fun drawRiepilogoGiornaliereTable(
        state: PdfState,
        giornaliere: List<RiepilogoGiornalieraItem>,
        startY: Float
    ): Float {
        var y = startY

        state.canvas!!.drawText("ELENCO GIORNALIERE", MARGIN, y + 14f, headerPaint)
        y += 25f

        if (giornaliere.isEmpty()) {
            state.canvas!!.drawText("Nessuna giornaliera presente", MARGIN, y + 11f, smallTextPaint)
            return y + LINE_HEIGHT
        }

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY)

        // Header tabella
        val colWidths = floatArrayOf(200f, 200f)
        val headers = arrayOf("Data", "Stato")

        state.canvas!!.drawRect(MARGIN, y, MARGIN + colWidths[0] + colWidths[1], y + 20f, tableHeaderBgPaint)

        var x = MARGIN + 5f
        headers.forEachIndexed { index, header ->
            state.canvas!!.drawText(header, x, y + 14f, smallTextPaint)
            x += colWidths[index]
        }
        y += 22f

        // Righe
        giornaliere.forEach { item ->
            state.yPosition = y
            state.checkNewPage(25f)
            y = state.yPosition

            x = MARGIN + 5f
            state.canvas!!.drawText(dateFormat.format(Date(item.data)), x, y + 11f, textPaint)
            x += colWidths[0]
            state.canvas!!.drawText(item.stato, x, y + 11f, textPaint)

            y += LINE_HEIGHT
        }

        return y
    }

    private fun drawRiepilogoFooter(canvas: Canvas, startY: Float): Float {
        var y = startY

        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, tablePaint)
        y += 15f

        val timestampFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY)
        val timestamp = timestampFormat.format(Date())
        canvas.drawText("Documento generato il: $timestamp", MARGIN, y + 10f, smallTextPaint)

        return y + LINE_HEIGHT
    }
}
