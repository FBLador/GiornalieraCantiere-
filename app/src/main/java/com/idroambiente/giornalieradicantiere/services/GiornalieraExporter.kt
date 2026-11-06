package com.idroambiente.giornalieradicantiere.services

import android.content.Intent
import com.idroambiente.giornalieradicantiere.data.preferences.ExportBehavior
import com.idroambiente.giornalieradicantiere.data.preferences.PreferencesRepository
import com.idroambiente.giornalieradicantiere.data.preferences.StorageMode
import com.idroambiente.giornalieradicantiere.database.entities.ArchivioFile
import com.idroambiente.giornalieradicantiere.database.entities.Giornaliera
import com.idroambiente.giornalieradicantiere.util.formatCommessaNome
import kotlinx.coroutines.flow.first
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Passi mostrati nella barra di avanzamento durante l'invio. */
enum class FaseInvio {
    PREPARAZIONE,
    GENERAZIONE_PDF,
    GENERAZIONE_CSV,
    CARICAMENTO_PDF,
    CARICAMENTO_CSV,
    SOSTITUZIONE_PDF,
    SOSTITUZIONE_CSV,
    AGGIORNAMENTO_REPORT,
    REGISTRO,
    FINALIZZAZIONE
}

/** Esito del primo invio di una giornaliera. */
sealed interface InvioEsito {
    /** PDF salvato (Drive, cartella o memoria dell'app). */
    data class Salvato(
        val pdf: SaveResult.Success,
        val csv: SaveResult.Success?,
        val csvErrore: String?,
        val memoriaLocale: Boolean
    ) : InvioEsito

    /** Modalità "Condividi"/"Salva e condividi": resta da aprire la condivisione. */
    data class DaCondividere(
        val shareIntent: Intent,
        val pdfFile: File,
        val salvato: SaveResult.Success?,
        val csv: SaveResult.Success?
    ) : InvioEsito

    data class Errore(val messaggio: String) : InvioEsito
}

/** File di una giornaliera dopo il reinvio. */
data class FileReinviato(
    val nome: String,
    val ref: String?,
    val esito: EsitoSostituzione?,
    val nomePrecedente: String?,
    val webViewLink: String?
)

/** Esito del reinvio di una giornaliera modificata. */
sealed interface ReinvioEsito {
    data class Ok(
        val archivio: ArchivioFile,
        val pdf: FileReinviato,
        val csv: FileReinviato?,
        val csvErrore: String?,
        /** Non nullo se va aperta la condivisione (modalità Condividi / Salva e condividi). */
        val shareIntent: Intent?
    ) : ReinvioEsito

    data class Errore(val messaggio: String) : ReinvioEsito
}

/**
 * Genera e salva PDF e CSV di una giornaliera.
 * - [invia]: primo invio (stesso comportamento di prima di questa versione)
 * - [reinvia]: dopo una modifica SOSTITUISCE i file già inviati invece di crearne di nuovi
 */
@Singleton
class GiornalieraExporter @Inject constructor(
    private val pdfGeneratorService: PdfGeneratorService,
    private val csvGeneratorService: CsvGeneratorService,
    private val fileStorageService: FileStorageService,
    private val preferencesRepository: PreferencesRepository
) {

    suspend fun comportamentoInvio(): Pair<StorageMode, ExportBehavior> {
        val storageMode = preferencesRepository.storageMode.first()
        val behavior = if (storageMode == StorageMode.GOOGLE_DRIVE) ExportBehavior.SAVE
        else preferencesRepository.exportBehavior.first()
        return storageMode to behavior
    }

    suspend fun invia(data: GiornalieraPdfData, onFase: (FaseInvio) -> Unit): InvioEsito {
        val (_, behavior) = comportamentoInvio()
        val commessaNome = formatCommessaNome(data.commessa.nome)
        val giornalieraData = data.giornaliera.data

        onFase(FaseInvio.GENERAZIONE_PDF)
        val pdfFile = pdfGeneratorService.generateGiornalieraPdf(data)
        onFase(FaseInvio.GENERAZIONE_CSV)
        val csvFile = runCatching { csvGeneratorService.generateGiornalieraCsv(data) }.getOrNull()

        return when (behavior) {
            ExportBehavior.SAVE -> {
                onFase(FaseInvio.CARICAMENTO_PDF)
                when (val saved = fileStorageService.savePdf(pdfFile, commessaNome, giornalieraData)) {
                    is SaveResult.Success -> {
                        onFase(FaseInvio.CARICAMENTO_CSV)
                        val csv = csvFile?.let { fileStorageService.saveCsv(it, commessaNome, giornalieraData) }
                        InvioEsito.Salvato(
                            pdf = saved,
                            csv = csv as? SaveResult.Success,
                            csvErrore = (csv as? SaveResult.Error)?.message ?: if (csvFile == null) "CSV non generato" else null,
                            memoriaLocale = false
                        )
                    }
                    is SaveResult.Error -> {
                        // Fallback: memoria dell'app (come prima)
                        onFase(FaseInvio.CARICAMENTO_PDF)
                        when (val local = fileStorageService.savePdfToAppStorage(pdfFile)) {
                            is SaveResult.Success -> {
                                onFase(FaseInvio.CARICAMENTO_CSV)
                                val csv = csvFile?.let { fileStorageService.saveCsvToAppStorage(it) }
                                InvioEsito.Salvato(local.copy(folderName = "memoria locale"), csv as? SaveResult.Success, null, memoriaLocale = true)
                            }
                            is SaveResult.Error -> InvioEsito.Errore(local.message)
                        }
                    }
                }
            }
            ExportBehavior.SHARE -> {
                onFase(FaseInvio.CARICAMENTO_CSV)
                val csv = csvFile?.let { fileStorageService.saveCsvToAppStorage(it) }
                InvioEsito.DaCondividere(fileStorageService.createShareChooserIntent(pdfFile), pdfFile, null, csv as? SaveResult.Success)
            }
            ExportBehavior.BOTH -> {
                onFase(FaseInvio.CARICAMENTO_PDF)
                val pdfCopy = File(pdfFile.parent, "share_${pdfFile.name}")
                pdfFile.copyTo(pdfCopy, overwrite = true)
                val saved = fileStorageService.savePdf(pdfFile, commessaNome, giornalieraData)
                val shareIntent = fileStorageService.createShareChooserIntent(pdfCopy)
                if (saved is SaveResult.Success) {
                    onFase(FaseInvio.CARICAMENTO_CSV)
                    val csv = csvFile?.let { fileStorageService.saveCsv(it, commessaNome, giornalieraData) }
                    InvioEsito.DaCondividere(shareIntent, pdfCopy, saved, csv as? SaveResult.Success)
                } else {
                    InvioEsito.DaCondividere(shareIntent, pdfCopy, null, null)
                }
            }
        }
    }

    /**
     * Reinvio dopo una modifica.
     *
     * @param data dati NUOVI (giornaliera con data/note/revisione aggiornate)
     * @param originale la giornaliera com'era prima (per ritrovare i file e il vecchio nome)
     */
    suspend fun reinvia(data: GiornalieraPdfData, originale: Giornaliera, onFase: (FaseInvio) -> Unit): ReinvioEsito {
        val (storageMode, behavior) = comportamentoInvio()
        val archivio = when {
            storageMode == StorageMode.GOOGLE_DRIVE -> ArchivioFile.DRIVE
            behavior == ExportBehavior.SHARE -> ArchivioFile.CONDIVISO
            else -> ArchivioFile.LOCALE
        }
        val revisione = data.giornaliera.revisione
        val commessaNome = formatCommessaNome(data.commessa.nome)
        val cantiereNome = data.cantiere.nome
        val stessoArchivio = originale.fileArchivio == archivio.name
        val knownArchivio = originale.fileArchivio?.let { runCatching { ArchivioFile.valueOf(it) }.getOrNull() }

        onFase(FaseInvio.GENERAZIONE_PDF)
        val pdfFile = pdfGeneratorService.generateGiornalieraPdf(data)
        onFase(FaseInvio.GENERAZIONE_CSV)
        val csvFile = runCatching { csvGeneratorService.generateGiornalieraCsv(data) }.getOrNull()

        if (archivio == ArchivioFile.CONDIVISO) {
            // Il PDF mandato prima (WhatsApp, email...) non si può richiamare:
            // si condivide il nuovo con "_revN" nel nome.
            onFase(FaseInvio.SOSTITUZIONE_PDF)
            val nomePdf = FileNaming.conRevisione(FileNaming.pdfGiornaliera(archivio, commessaNome, cantiereNome, data.giornaliera.data), revisione)
            val condiviso = File(pdfFile.parentFile, nomePdf)
            pdfFile.copyTo(condiviso, overwrite = true)
            pdfFile.delete()
            onFase(FaseInvio.SOSTITUZIONE_CSV)
            val csv = csvFile?.let { sostituisciCsv(it, ArchivioFile.APP, originale, knownArchivio, commessaNome, cantiereNome, data, revisione) }
            return ReinvioEsito.Ok(
                archivio = archivio,
                pdf = FileReinviato(nomePdf, null, null, null, null),
                csv = (csv as? ReplaceResult.Done)?.toFile(),
                csvErrore = (csv as? ReplaceResult.Failed)?.message,
                shareIntent = fileStorageService.createShareChooserIntent(condiviso)
            )
        }

        val nuovoNomePdf = FileNaming.pdfGiornaliera(archivio, commessaNome, cantiereNome, data.giornaliera.data)
        val vecchioNomePdf = originale.pdfFileName?.takeIf { stessoArchivio }
            ?: FileNaming.pdfGiornaliera(archivio, commessaNome, cantiereNome, originale.data)

        // Per "Salva e condividi" teniamo una copia da condividere
        val pdfDaCondividere = if (behavior == ExportBehavior.BOTH && archivio == ArchivioFile.LOCALE) {
            File(pdfFile.parentFile, "share_" + FileNaming.conRevisione(nuovoNomePdf, revisione)).also { pdfFile.copyTo(it, overwrite = true) }
        } else null

        onFase(FaseInvio.SOSTITUZIONE_PDF)
        val pdf = fileStorageService.sostituisciFile(
            file = pdfFile,
            mimeType = FileStorageService.PDF_MIME_TYPE,
            archivio = archivio,
            knownRef = originale.pdfFileRef,
            knownArchivio = knownArchivio,
            oldName = vecchioNomePdf,
            newName = nuovoNomePdf,
            fallbackName = FileNaming.conRevisione(nuovoNomePdf, revisione),
            cartella = FileStorageService.Cartella.Giornaliere(commessaNome)
        )
        pdfFile.delete()
        if (pdf is ReplaceResult.Failed) {
            csvFile?.delete()
            return ReinvioEsito.Errore(pdf.message)
        }
        pdf as ReplaceResult.Done

        onFase(FaseInvio.SOSTITUZIONE_CSV)
        val csv = csvFile?.let { sostituisciCsv(it, archivio, originale, knownArchivio, commessaNome, cantiereNome, data, revisione) }

        return ReinvioEsito.Ok(
            archivio = archivio,
            pdf = pdf.toFile(),
            csv = (csv as? ReplaceResult.Done)?.toFile(),
            csvErrore = (csv as? ReplaceResult.Failed)?.message ?: if (csvFile == null) "CSV non generato" else null,
            shareIntent = pdfDaCondividere?.let { fileStorageService.createShareChooserIntent(it) }
        )
    }

    private suspend fun sostituisciCsv(
        csvFile: File,
        archivio: ArchivioFile,
        originale: Giornaliera,
        knownArchivio: ArchivioFile?,
        commessaNome: String,
        cantiereNome: String,
        data: GiornalieraPdfData,
        revisione: Int
    ): ReplaceResult {
        val nuovoNome = FileNaming.csvGiornaliera(archivio, commessaNome, cantiereNome, data.giornaliera.data)
        val vecchioNome = originale.csvFileName?.takeIf { originale.fileArchivio == archivio.name }
            ?: FileNaming.csvGiornaliera(archivio, commessaNome, cantiereNome, originale.data)
        return fileStorageService.sostituisciFile(
            file = csvFile,
            mimeType = FileStorageService.CSV_MIME_TYPE,
            archivio = archivio,
            knownRef = originale.csvFileRef,
            knownArchivio = knownArchivio,
            oldName = vecchioNome,
            newName = nuovoNome,
            fallbackName = FileNaming.conRevisione(nuovoNome, revisione),
            cartella = FileStorageService.Cartella.Giornaliere(commessaNome)
        ).also { csvFile.delete() }
    }

    private fun ReplaceResult.Done.toFile() = FileReinviato(name, ref, esito, previousName, webViewLink)
}
