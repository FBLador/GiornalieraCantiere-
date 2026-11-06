package com.idroambiente.giornalieradicantiere.services

import com.idroambiente.giornalieradicantiere.data.preferences.PreferencesRepository
import com.idroambiente.giornalieradicantiere.database.dao.ReportEsportatoDao
import com.idroambiente.giornalieradicantiere.database.entities.ArchivioFile
import com.idroambiente.giornalieradicantiere.database.entities.Commessa
import com.idroambiente.giornalieradicantiere.database.entities.ReportEsportato
import com.idroambiente.giornalieradicantiere.database.repository.AttrezzaturaGiornalieraRepository
import com.idroambiente.giornalieradicantiere.database.repository.AttrezzaturaRepository
import com.idroambiente.giornalieradicantiere.database.repository.CantiereRepository
import com.idroambiente.giornalieradicantiere.database.repository.CommessaRepository
import com.idroambiente.giornalieradicantiere.database.repository.GiornalieraRepository
import com.idroambiente.giornalieradicantiere.database.repository.MezzoGiornalieraRepository
import com.idroambiente.giornalieradicantiere.database.repository.MezzoRepository
import com.idroambiente.giornalieradicantiere.database.repository.PersonaleGiornalieraRepository
import com.idroambiente.giornalieradicantiere.util.formatCommessaNome
import kotlinx.coroutines.flow.first
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Esito di un'esportazione del report finale (riepilogo commessa). */
data class EsportazioneReport(
    val fileName: String,
    val folderName: String?,
    val webViewLink: String?,
    /** null se salvato nella memoria dell'app perché l'archivio non era raggiungibile. */
    val esito: EsitoSostituzione?,
    val csvErrore: String? = null,
    val memoriaLocale: Boolean = false
)

/** Esito dell'aggiornamento automatico dei report dopo una modifica. */
data class AggiornamentoReport(
    val aggiornati: List<String> = emptyList(),
    val falliti: List<String> = emptyList()
)

/**
 * Crea e aggiorna il report finale di una commessa (PDF + CSV del riepilogo).
 *
 * Il report viene sempre SOVRASCRITTO se esiste già con lo stesso nome (niente doppioni)
 * e viene annotato in [ReportEsportato]: così, quando si modifica una giornaliera,
 * l'app sa quali report rigenerare.
 */
@Singleton
class RiepilogoExporter @Inject constructor(
    private val commessaRepository: CommessaRepository,
    private val cantiereRepository: CantiereRepository,
    private val giornalieraRepository: GiornalieraRepository,
    private val personaleGiornalieraRepository: PersonaleGiornalieraRepository,
    private val mezzoGiornalieraRepository: MezzoGiornalieraRepository,
    private val mezzoRepository: MezzoRepository,
    private val attrezzaturaRepository: AttrezzaturaRepository,
    private val attrezzaturaGiornalieraRepository: AttrezzaturaGiornalieraRepository,
    private val pdfGeneratorService: PdfGeneratorService,
    private val csvGeneratorService: CsvGeneratorService,
    private val fileStorageService: FileStorageService,
    private val preferencesRepository: PreferencesRepository,
    private val networkUtils: NetworkUtils,
    private val reportDao: ReportEsportatoDao
) {

    /** Esportazione manuale dalla schermata Riepilogo commessa. */
    suspend fun esporta(commessaId: Long, startDate: Long?, endDate: Long?): Result<EsportazioneReport> = runCatching {
        val commessa = commessaRepository.getById(commessaId) ?: error("Commessa non trovata")
        val file = genera(commessa, startDate, endDate)
        val archivio = fileStorageService.archivioCorrente()
        val tracciato = reportDao.find(commessaId, archivio.name, file.pdf.name)

        when (val pdf = sostituisci(file.pdf, FileStorageService.PDF_MIME_TYPE, archivio, tracciato?.pdfFileRef, commessa)) {
            is ReplaceResult.Done -> {
                val csv = file.csv?.let { sostituisci(it, FileStorageService.CSV_MIME_TYPE, archivio, tracciato?.csvFileRef, commessa) }
                registra(commessa.id, startDate, endDate, archivio, pdf, csv as? ReplaceResult.Done, file, tracciato)
                EsportazioneReport(
                    fileName = pdf.name,
                    folderName = cartellaVisibile(archivio, commessa),
                    webViewLink = pdf.webViewLink,
                    esito = pdf.esito,
                    csvErrore = (csv as? ReplaceResult.Failed)?.message
                )
            }
            is ReplaceResult.Failed -> {
                // Come prima di questa versione: se l'archivio non risponde, salva nella memoria dell'app
                val local = fileStorageService.savePdfToAppStorage(file.pdf)
                file.csv?.let { runCatching { fileStorageService.saveCsvToAppStorage(it) } }
                when (local) {
                    is SaveResult.Success -> EsportazioneReport(local.fileName, "memoria locale", null, null, memoriaLocale = true)
                    is SaveResult.Error -> error(local.message)
                }
            }
        }
    }

    /**
     * Dopo una modifica: rigenera i report esportati da questo telefono che contengono
     * almeno una delle [date] (la data vecchia e quella nuova, se è stata spostata).
     */
    suspend fun aggiornaReportCheContengono(commessaId: Long, date: Collection<Long>): AggiornamentoReport {
        val archivio = fileStorageService.archivioCorrente()
        val daAggiornare = reportDao.getByCommessa(commessaId, archivio.name)
            .filter { ReportCoverage.contieneAlmenoUna(it.startDate, it.endDate, date) }
        return rigenera(commessaId, daAggiornare)
    }

    /** Riprova i report rimasti indietro (pulsante "Riprova" nella commessa). */
    suspend fun riprovaDaAggiornare(commessaId: Long): AggiornamentoReport =
        rigenera(commessaId, reportDao.getDaAggiornare(commessaId))

    private suspend fun rigenera(commessaId: Long, reports: List<ReportEsportato>): AggiornamentoReport {
        if (reports.isEmpty()) return AggiornamentoReport()
        val commessa = commessaRepository.getById(commessaId) ?: return AggiornamentoReport()
        val archivio = fileStorageService.archivioCorrente()
        val online = archivio != ArchivioFile.DRIVE || networkUtils.isOnline()

        val aggiornati = mutableListOf<String>()
        val falliti = mutableListOf<String>()
        for (report in reports) {
            val ok = online && runCatching {
                val file = genera(commessa, report.startDate, report.endDate)
                val pdf = sostituisci(file.pdf, FileStorageService.PDF_MIME_TYPE, archivio, report.pdfFileRef, commessa)
                if (pdf !is ReplaceResult.Done) return@runCatching false
                val csv = file.csv?.let { sostituisci(it, FileStorageService.CSV_MIME_TYPE, archivio, report.csvFileRef, commessa) }
                registra(commessaId, report.startDate, report.endDate, archivio, pdf, csv as? ReplaceResult.Done, file, report)
                true
            }.getOrDefault(false)
            if (ok) {
                aggiornati += report.pdfFileName
            } else {
                falliti += report.pdfFileName
                reportDao.setDaAggiornare(report.id, true)
            }
        }
        return AggiornamentoReport(aggiornati, falliti)
    }

    private suspend fun sostituisci(
        file: File,
        mime: String,
        archivio: ArchivioFile,
        knownRef: String?,
        commessa: Commessa
    ): ReplaceResult = fileStorageService.sostituisciFile(
        file = file,
        mimeType = mime,
        archivio = archivio,
        knownRef = knownRef,
        knownArchivio = archivio,
        oldName = file.name,
        newName = file.name,
        fallbackName = file.name,
        cartella = FileStorageService.Cartella.Riepiloghi(formatCommessaNome(commessa.nome))
    ).also { file.delete() }

    private suspend fun registra(
        commessaId: Long,
        startDate: Long?,
        endDate: Long?,
        archivio: ArchivioFile,
        pdf: ReplaceResult.Done,
        csv: ReplaceResult.Done?,
        file: FileReport,
        esistente: ReportEsportato?
    ) {
        val report = ReportEsportato(
            id = esistente?.id ?: 0,
            commessaId = commessaId,
            startDate = startDate,
            endDate = endDate,
            archivio = archivio.name,
            pdfFileName = file.nomePdf,
            pdfFileRef = pdf.ref,
            csvFileName = file.nomeCsv,
            csvFileRef = csv?.ref ?: esistente?.csvFileRef,
            exportedAt = System.currentTimeMillis(),
            daAggiornare = false
        )
        if (esistente == null) {
            val gia = reportDao.find(commessaId, archivio.name, file.nomePdf)
            if (gia == null) reportDao.insert(report) else reportDao.update(report.copy(id = gia.id))
        } else {
            reportDao.update(report)
        }
    }

    private suspend fun cartellaVisibile(archivio: ArchivioFile, commessa: Commessa): String = when (archivio) {
        ArchivioFile.DRIVE -> "${formatCommessaNome(commessa.nome)} › Riepiloghi"
        else -> preferencesRepository.exportFolderName.first() ?: "Cartella del telefono"
    }

    private class FileReport(val pdf: File, val csv: File?) {
        val nomePdf: String = pdf.name
        val nomeCsv: String? = csv?.name
    }

    /** Genera PDF e CSV del riepilogo (stessa logica di prima, spostata qui dalla schermata). */
    private suspend fun genera(commessa: Commessa, startDate: Long?, endDate: Long?): FileReport {
        val cantiereId = cantiereRepository.getByCommessaId(commessa.id).first().firstOrNull()?.id
        val giornaliere = if (cantiereId == null) emptyList() else
            giornalieraRepository.getByCantiereId(cantiereId).first().filter { g ->
                (startDate == null || g.data >= startDate) && (endDate == null || g.data <= endDate + 86_399_999L)
            }

        var oreLavoro = 0f
        var oreViaggio = 0f
        var km = 0f
        val mezzoAggregation = mutableMapOf<Long, Pair<MutableSet<Long>, Float>>()
        val attrezzaturaAggregation = mutableMapOf<Long, MutableSet<Long>>()
        for (g in giornaliere) {
            val personale = personaleGiornalieraRepository.getByGiornalieraId(g.id).first()
            oreLavoro += personale.sumOf { it.oreLavoro.toDouble() }.toFloat()
            oreViaggio += personale.sumOf { (it.oreViaggioAndata + it.oreViaggioRitorno).toDouble() }.toFloat()
            val mezzi = mezzoGiornalieraRepository.getByGiornalieraId(g.id).first()
            km += mezzi.sumOf { (it.kmAndata + it.kmRitorno).toDouble() }.toFloat()
            for (mg in mezzi) {
                val (giorni, kmPrecedenti) = mezzoAggregation.getOrPut(mg.mezzoId) { mutableSetOf<Long>() to 0f }
                giorni.add(g.id)
                mezzoAggregation[mg.mezzoId] = giorni to (kmPrecedenti + mg.kmAndata + mg.kmRitorno)
            }
            for (ag in attrezzaturaGiornalieraRepository.getByGiornalieraId(g.id).first()) {
                attrezzaturaAggregation.getOrPut(ag.attrezzaturaId) { mutableSetOf() }.add(g.id)
            }
        }

        val mezziDettaglio = if (mezzoAggregation.isEmpty()) emptyList() else
            mezzoRepository.getByIds(mezzoAggregation.keys.toList()).map { mezzo ->
                val entry = mezzoAggregation[mezzo.id]
                MezzoRiepilogoItem(
                    nome = mezzo.nome,
                    targa = mezzo.targa,
                    codiceArticoloGiorni = mezzo.codiceArticoloGiorni,
                    giorniUtilizzo = entry?.first?.size ?: 0,
                    codiceArticoloKm = mezzo.codiceArticoloKm,
                    totaleKm = entry?.second ?: 0f
                )
            }
        val attrezzatureDettaglio = if (attrezzaturaAggregation.isEmpty()) emptyList() else
            attrezzaturaRepository.getByIds(attrezzaturaAggregation.keys.toList()).map { attr ->
                AttrezzaturaRiepilogoItem(
                    nome = attr.nome,
                    codiceArticolo = attr.codiceArticolo,
                    giorniUtilizzo = attrezzaturaAggregation[attr.id]?.size ?: 0
                )
            }

        val commessaNome = formatCommessaNome(commessa.nome)
        val pdfData = RiepilogoCommessaPdfData(
            commessaNome = commessaNome,
            cliente = commessa.cliente,
            indirizzo = commessa.indirizzo,
            stato = commessa.stato.name,
            totaleOreLavoro = oreLavoro,
            totaleOreViaggio = oreViaggio,
            totaleKm = km,
            giornaliere = giornaliere.map { RiepilogoGiornalieraItem(data = it.data, stato = it.stato.name) },
            startDate = startDate,
            endDate = endDate,
            mezziDettaglio = mezziDettaglio,
            attrezzatureDettaglio = attrezzatureDettaglio
        )
        val csvData = RiepilogoCommessaCsvData(
            commessaNome = commessaNome,
            totaleOreLavoro = oreLavoro,
            totaleOreViaggio = oreViaggio,
            codiceArticoloOreLavoro = preferencesRepository.codiceArticoloOreLavoro.first(),
            codiceArticoloOreViaggio = preferencesRepository.codiceArticoloOreViaggio.first(),
            mezzi = mezziDettaglio.map { MezzoCsvData(it.codiceArticoloGiorni, it.giorniUtilizzo, it.codiceArticoloKm, it.totaleKm) },
            attrezzature = attrezzatureDettaglio.map { AttrezzaturaCsvData(it.codiceArticolo, it.giorniUtilizzo) },
            startDate = startDate,
            endDate = endDate
        )
        val pdf = pdfGeneratorService.generateRiepilogoCommessaPdf(pdfData)
        val csv = runCatching { csvGeneratorService.generateRiepilogoCommessaCsv(csvData) }.getOrNull()
        return FileReport(pdf, csv)
    }
}
