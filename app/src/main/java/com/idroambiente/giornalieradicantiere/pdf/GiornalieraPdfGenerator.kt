package com.idroambiente.giornalieradicantiere.pdf

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.idroambiente.giornalieradicantiere.database.entities.Cantiere
import com.idroambiente.giornalieradicantiere.database.entities.Commessa
import com.idroambiente.giornalieradicantiere.database.entities.Giornaliera
import com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard.RiepilogoData
import com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard.formatTimeFromMillis
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class PersonalePdfEntry(
    val nomeCompleto: String,
    val oreLavoro: Float,
    val oreViaggio: Float,
    val pernotto: Boolean,
    val inizioTurno: Long,
    val fineTurno: Long
)

data class MezzoPdfEntry(
    val nome: String,
    val targa: String,
    val kmAndata: Float,
    val kmRitorno: Float
)

data class AttrezzaturaPdfEntry(
    val nome: String
)

class GiornalieraPdfGenerator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun generatePdf(
        giornaliera: Giornaliera,
        cantiere: Cantiere?,
        commessa: Commessa?,
        personale: List<PersonalePdfEntry>,
        mezzi: List<MezzoPdfEntry>,
        attrezzature: List<AttrezzaturaPdfEntry>,
        riepilogo: RiepilogoData,
        note: String,
        folderUri: Uri?
    ): File {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 32
        val maxWidth = pageWidth - margin * 2

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 18f
            isFakeBoldText = true
        }
        val sectionPaint = Paint().apply {
            color = Color.BLACK
            textSize = 14f
            isFakeBoldText = true
        }
        val bodyPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
        }
        val mutedPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 12f
        }

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var y = margin.toFloat()
        val lineHeight = bodyPaint.textSize * 1.4f

        fun newPage() {
            document.finishPage(page)
            pageNumber += 1
            page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            canvas = page.canvas
            y = margin.toFloat()
        }

        fun ensureSpace() {
            if (y + lineHeight > pageHeight - margin) {
                newPage()
            }
        }

        fun drawLine(text: String, paint: Paint = bodyPaint) {
            ensureSpace()
            canvas.drawText(text, margin.toFloat(), y, paint)
            y += lineHeight
        }

        fun drawWrappedText(text: String, paint: Paint = bodyPaint) {
            val words = text.split(" ")
            var currentLine = ""
            words.forEach { word ->
                val tentative = if (currentLine.isBlank()) word else "$currentLine $word"
                if (paint.measureText(tentative) > maxWidth) {
                    if (currentLine.isNotBlank()) {
                        drawLine(currentLine, paint)
                    }
                    currentLine = word
                } else {
                    currentLine = tentative
                }
            }
            if (currentLine.isNotBlank()) {
                drawLine(currentLine, paint)
            }
        }

        val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY)
        val fileFormatter = SimpleDateFormat("yyyyMMdd_HHmm", Locale.ITALY)
        val data = dateFormatter.format(Date(giornaliera.data))

        drawLine("Giornaliera di cantiere", titlePaint)
        drawLine("Data: $data", mutedPaint)
        drawLine("Stato: ${giornaliera.stato.name}", mutedPaint)
        if (cantiere != null) {
            drawWrappedText("Cantiere: ${cantiere.nome} - ${cantiere.indirizzo}")
        }
        if (commessa != null) {
            drawWrappedText("Commessa: ${commessa.nome} (${commessa.cliente})")
            drawWrappedText("Indirizzo commessa: ${commessa.indirizzo}", mutedPaint)
        }
        drawLine("DPI idonei: ${if (giornaliera.dpiIdonei) "Sì" else "No"}", mutedPaint)
        drawLine("Presa visione POS: ${if (giornaliera.presaVisionePOS) "Sì" else "No"}", mutedPaint)

        y += lineHeight * 0.5f
        drawLine("Note", sectionPaint)
        if (note.isBlank()) {
            drawLine("Nessuna nota", mutedPaint)
        } else {
            drawWrappedText(note)
        }

        y += lineHeight * 0.5f
        drawLine("Personale", sectionPaint)
        if (personale.isEmpty()) {
            drawLine("Nessuna persona presente", mutedPaint)
        } else {
            personale.forEach { persona ->
                val turno = if (persona.inizioTurno > 0 && persona.fineTurno > 0) {
                    " | Turno: ${formatTimeFromMillis(persona.inizioTurno)}-${formatTimeFromMillis(persona.fineTurno)}"
                } else {
                    ""
                }
                val pernotto = if (persona.pernotto) " | Pernotto" else ""
                drawWrappedText(
                    "- ${persona.nomeCompleto} | Lavoro: ${formatHours(persona.oreLavoro)}h | " +
                        "Viaggio: ${formatHours(persona.oreViaggio)}h$pernotto$turno"
                )
            }
        }
        drawLine(
            "Totale presenti: ${riepilogo.totalePersonePresenti} | " +
                "Ore lavoro: ${formatHours(riepilogo.totaleOreLavoro)} | " +
                "Ore viaggio: ${formatHours(riepilogo.totaleOreViaggio)} | " +
                "Pernotti: ${riepilogo.totalePernotti}",
            mutedPaint
        )

        y += lineHeight * 0.5f
        drawLine("Mezzi", sectionPaint)
        if (mezzi.isEmpty()) {
            drawLine("Nessun mezzo utilizzato", mutedPaint)
        } else {
            mezzi.forEach { mezzo ->
                drawWrappedText(
                    "- ${mezzo.nome} (${mezzo.targa}) | " +
                        "Km andata: ${formatKm(mezzo.kmAndata)} | Km ritorno: ${formatKm(mezzo.kmRitorno)}"
                )
            }
        }
        drawLine(
            "Totale mezzi: ${riepilogo.totaleMezziUtilizzati} | " +
                "Totale km: ${formatKm(riepilogo.totaleKm)}",
            mutedPaint
        )

        y += lineHeight * 0.5f
        drawLine("Attrezzature", sectionPaint)
        if (attrezzature.isEmpty()) {
            drawLine("Nessuna attrezzatura utilizzata", mutedPaint)
        } else {
            attrezzature.forEach { attrezzatura ->
                drawWrappedText("- ${attrezzatura.nome}")
            }
        }
        drawLine(
            "Totale attrezzature: ${riepilogo.totaleAttrezzatureUtilizzate}",
            mutedPaint
        )

        document.finishPage(page)

        val fileName = "giornaliera_${giornaliera.id}_${fileFormatter.format(Date(giornaliera.data))}.pdf"
        val outputFile = folderUri?.let { uri ->
            saveToDocumentTree(uri, fileName, document)
        } ?: saveToLocalFolder(fileName, document)
        document.close()
        return outputFile
    }

    private fun saveToLocalFolder(fileName: String, document: PdfDocument): File {
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        val outputDir = File(baseDir, "giornaliere")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        val outputFile = File(outputDir, fileName)
        FileOutputStream(outputFile).use { outputStream ->
            document.writeTo(outputStream)
        }
        return outputFile
    }

    private fun saveToDocumentTree(folderUri: Uri, fileName: String, document: PdfDocument): File {
        val documentFile = DocumentFile.fromTreeUri(context, folderUri)
        val outputDocument = documentFile?.createFile("application/pdf", fileName)
        if (outputDocument == null) {
            return saveToLocalFolder(fileName, document)
        }
        context.contentResolver.openOutputStream(outputDocument.uri).use { outputStream ->
            if (outputStream != null) {
                document.writeTo(outputStream)
            } else {
                return saveToLocalFolder(fileName, document)
            }
        }
        return File(fileName)
    }

    private fun formatHours(hours: Float): String {
        return if (hours == hours.toInt().toFloat()) {
            hours.toInt().toString()
        } else {
            String.format(Locale.ITALY, "%.1f", hours)
        }
    }

    private fun formatKm(km: Float): String {
        return if (km == km.toInt().toFloat()) {
            km.toInt().toString()
        } else {
            String.format(Locale.ITALY, "%.1f", km)
        }
    }
}
