package com.idroambiente.giornalieradicantiere.ui.impostazioni

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idroambiente.giornalieradicantiere.database.entities.Attrezzatura
import com.idroambiente.giornalieradicantiere.database.entities.AttrezzaturaGiornaliera
import com.idroambiente.giornalieradicantiere.database.entities.Cantiere
import com.idroambiente.giornalieradicantiere.database.entities.Commessa
import com.idroambiente.giornalieradicantiere.database.entities.Giornaliera
import com.idroambiente.giornalieradicantiere.database.entities.Mezzo
import com.idroambiente.giornalieradicantiere.database.entities.MezzoGiornaliera
import com.idroambiente.giornalieradicantiere.database.entities.Personale
import com.idroambiente.giornalieradicantiere.database.entities.PersonaleGiornaliera
import com.idroambiente.giornalieradicantiere.database.entities.StatoGiornaliera
import com.idroambiente.giornalieradicantiere.database.repository.AttrezzaturaGiornalieraRepository
import com.idroambiente.giornalieradicantiere.database.repository.AttrezzaturaRepository
import com.idroambiente.giornalieradicantiere.database.repository.CantiereRepository
import com.idroambiente.giornalieradicantiere.database.repository.CommessaRepository
import com.idroambiente.giornalieradicantiere.database.repository.GiornalieraRepository
import com.idroambiente.giornalieradicantiere.database.repository.MezzoGiornalieraRepository
import com.idroambiente.giornalieradicantiere.database.repository.MezzoRepository
import com.idroambiente.giornalieradicantiere.database.repository.PersonaleGiornalieraRepository
import com.idroambiente.giornalieradicantiere.database.repository.PersonaleRepository
import com.idroambiente.giornalieradicantiere.data.preferences.PreferencesRepository
import com.idroambiente.giornalieradicantiere.util.formatCommessaNome
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

data class GiornalieraParseResult(
    val fileCount: Int,
    val giornaliere: List<ParsedGiornaliera>,
    val newPersonale: Set<String>,
    val newMezzi: Set<String>,
    val newAttrezzature: Set<String>,
    val errors: List<String>
)

data class ParsedGiornaliera(
    val data: Long,
    val commessaNomeInt: Int,
    val cliente: String,
    val personaleRows: List<ParsedPersonaleRow>,
    val mezzoRows: List<ParsedMezzoRow>,
    val attrezzatureNomi: List<String>
)

data class ParsedPersonaleRow(
    val nome: String,
    val cognome: String,
    val oreLavoro: Float,
    val oreViaggioAndata: Float,
    val oreViaggioRitorno: Float,
    val inizioTurno: Long,
    val fineTurno: Long,
    val pernotto: Boolean
)

data class ParsedMezzoRow(
    val nome: String,
    val targa: String,
    val kmAndata: Float,
    val kmRitorno: Float,
    val codiceArticoloKm: String,
    val codiceArticoloGiorni: String
)

sealed class GiornalieraImportState {
    data object Idle : GiornalieraImportState()
    data object Scanning : GiornalieraImportState()
    data class Parsed(val result: GiornalieraParseResult) : GiornalieraImportState()
    data class Importing(val progress: String) : GiornalieraImportState()
    data class Success(val summary: String) : GiornalieraImportState()
    data class Error(val message: String) : GiornalieraImportState()
}

@HiltViewModel
class GiornalieraImportViewModel @Inject constructor(
    private val commessaRepository: CommessaRepository,
    private val cantiereRepository: CantiereRepository,
    private val giornalieraRepository: GiornalieraRepository,
    private val personaleRepository: PersonaleRepository,
    private val mezzoRepository: MezzoRepository,
    private val attrezzaturaRepository: AttrezzaturaRepository,
    private val personaleGiornalieraRepository: PersonaleGiornalieraRepository,
    private val mezzoGiornalieraRepository: MezzoGiornalieraRepository,
    private val attrezzaturaGiornalieraRepository: AttrezzaturaGiornalieraRepository,
    preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _state = MutableStateFlow<GiornalieraImportState>(GiornalieraImportState.Idle)
    val state: StateFlow<GiornalieraImportState> = _state.asStateFlow()

    val exportFolderUri: StateFlow<Uri?> = preferencesRepository.exportFolderUri
        .map { uriString -> uriString?.let { Uri.parse(it) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun scanFolder(folderUri: Uri, context: Context) {
        viewModelScope.launch {
            _state.value = GiornalieraImportState.Scanning
            try {
                val folder = DocumentFile.fromTreeUri(context, folderUri)
                    ?: run {
                        _state.value = GiornalieraImportState.Error("Impossibile accedere alla cartella selezionata")
                        return@launch
                    }

                val csvFiles = folder.listFiles().filter {
                    it.isFile && (it.name?.endsWith(".csv", ignoreCase = true) == true)
                }

                if (csvFiles.isEmpty()) {
                    _state.value = GiornalieraImportState.Error("Nessun file CSV trovato nella cartella selezionata")
                    return@launch
                }

                val allRows = mutableListOf<List<String>>()
                val errors = mutableListOf<String>()
                var fileCount = 0

                for (csvFile in csvFiles) {
                    val fileName = csvFile.name ?: "unknown"
                    try {
                        context.contentResolver.openInputStream(csvFile.uri)?.use { inputStream ->
                            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
                            val lines = reader.readLines()
                            if (lines.isEmpty()) {
                                errors.add("$fileName: file vuoto")
                                return@use
                            }

                            var startIndex = 0
                            var firstLine = lines[0]
                            if (firstLine.startsWith("\uFEFF")) {
                                firstLine = firstLine.removePrefix("\uFEFF")
                            }
                            val firstFields = splitCsvLine(firstLine)
                            if (firstFields.isNotEmpty() && firstFields[0].trim().equals("Data", ignoreCase = true)) {
                                startIndex = 1
                            }

                            var rowsParsed = 0
                            for (i in startIndex until lines.size) {
                                var line = lines[i]
                                if (i == 0 && line.startsWith("\uFEFF")) {
                                    line = line.removePrefix("\uFEFF")
                                }
                                line = line.trim()
                                if (line.isEmpty()) continue

                                val fields = splitCsvLine(line)
                                if (fields.size < 6) {
                                    errors.add("$fileName riga ${i + 1}: colonne insufficienti (${fields.size})")
                                    continue
                                }
                                allRows.add(fields)
                                rowsParsed++
                            }
                            if (rowsParsed > 0) fileCount++
                        } ?: errors.add("$fileName: impossibile aprire il file")
                    } catch (e: Exception) {
                        errors.add("$fileName: ${e.message}")
                    }
                }

                if (allRows.isEmpty()) {
                    _state.value = GiornalieraImportState.Error(
                        "Nessun dato valido trovato." +
                                if (errors.isNotEmpty()) "\n\n" + errors.joinToString("\n") else ""
                    )
                    return@launch
                }

                // Group by (Data + Commessa)
                val grouped = mutableMapOf<Pair<Long, Int>, MutableList<List<String>>>()
                for (fields in allRows) {
                    try {
                        val dataTimestamp = parseDate(fields[0].trim())
                        val commessaNomeInt = parseCommessaNome(fields[1].trim())
                        val key = Pair(dataTimestamp, commessaNomeInt)
                        grouped.getOrPut(key) { mutableListOf() }.add(fields)
                    } catch (e: Exception) {
                        errors.add("Errore parsing riga: ${e.message}")
                    }
                }

                // Build ParsedGiornaliera list
                val giornaliere = mutableListOf<ParsedGiornaliera>()
                val newPersonaleNames = mutableSetOf<String>()
                val newMezziTarghe = mutableSetOf<String>()
                val newAttrezzatureNomi = mutableSetOf<String>()

                for ((key, rows) in grouped) {
                    val (data, commessaNomeInt) = key
                    val cliente = rows.firstOrNull()?.getOrNull(2)?.trim()?.removeSurrounding("\"") ?: ""

                    val personaleRows = mutableListOf<ParsedPersonaleRow>()
                    val mezzoRows = mutableListOf<ParsedMezzoRow>()
                    val attrezzatureNomi = mutableListOf<String>()

                    for (fields in rows) {
                        val tipo = fields.getOrNull(4)?.trim()?.removeSurrounding("\"") ?: continue
                        when (tipo) {
                            "Personale" -> {
                                val nome = fields.getOrNull(5)?.trim()?.removeSurrounding("\"") ?: ""
                                val cognome = fields.getOrNull(6)?.trim()?.removeSurrounding("\"") ?: ""
                                if (nome.isBlank() && cognome.isBlank()) continue
                                personaleRows.add(
                                    ParsedPersonaleRow(
                                        nome = nome,
                                        cognome = cognome,
                                        oreLavoro = parseItalianFloat(fields.getOrNull(7)),
                                        oreViaggioAndata = parseItalianFloat(fields.getOrNull(8)),
                                        oreViaggioRitorno = parseItalianFloat(fields.getOrNull(9)),
                                        inizioTurno = parseTimeToMillis(fields.getOrNull(10)),
                                        fineTurno = parseTimeToMillis(fields.getOrNull(11)),
                                        pernotto = fields.getOrNull(12)?.trim()?.removeSurrounding("\"")
                                            ?.equals("Si", ignoreCase = true) == true
                                    )
                                )
                                val existing = personaleRepository.getByNomeECognome(nome, cognome)
                                if (existing == null) {
                                    newPersonaleNames.add("$nome $cognome")
                                }
                            }
                            "Mezzo" -> {
                                val nome = fields.getOrNull(5)?.trim()?.removeSurrounding("\"") ?: ""
                                val targa = fields.getOrNull(13)?.trim()?.removeSurrounding("\"") ?: ""
                                if (targa.isBlank() && nome.isBlank()) continue
                                mezzoRows.add(
                                    ParsedMezzoRow(
                                        nome = nome,
                                        targa = targa,
                                        kmAndata = parseItalianFloat(fields.getOrNull(14)),
                                        kmRitorno = parseItalianFloat(fields.getOrNull(15)),
                                        codiceArticoloKm = fields.getOrNull(18)?.trim()?.removeSurrounding("\"") ?: "",
                                        codiceArticoloGiorni = fields.getOrNull(19)?.trim()?.removeSurrounding("\"") ?: ""
                                    )
                                )
                                if (targa.isNotBlank()) {
                                    val existing = mezzoRepository.getByTarga(targa)
                                    if (existing == null) {
                                        newMezziTarghe.add(if (nome.isNotBlank()) "$nome ($targa)" else targa)
                                    }
                                }
                            }
                            "Attrezzatura" -> {
                                val nome = fields.getOrNull(5)?.trim()?.removeSurrounding("\"") ?: ""
                                if (nome.isBlank()) continue
                                attrezzatureNomi.add(nome)
                                val existing = attrezzaturaRepository.getByNome(nome)
                                if (existing == null) {
                                    newAttrezzatureNomi.add(nome)
                                }
                            }
                        }
                    }

                    giornaliere.add(
                        ParsedGiornaliera(
                            data = data,
                            commessaNomeInt = commessaNomeInt,
                            cliente = cliente,
                            personaleRows = personaleRows,
                            mezzoRows = mezzoRows,
                            attrezzatureNomi = attrezzatureNomi
                        )
                    )
                }

                if (giornaliere.isEmpty()) {
                    _state.value = GiornalieraImportState.Error(
                        "Nessuna giornaliera valida trovata." +
                                if (errors.isNotEmpty()) "\n\n" + errors.joinToString("\n") else ""
                    )
                    return@launch
                }

                _state.value = GiornalieraImportState.Parsed(
                    GiornalieraParseResult(
                        fileCount = fileCount,
                        giornaliere = giornaliere,
                        newPersonale = newPersonaleNames,
                        newMezzi = newMezziTarghe,
                        newAttrezzature = newAttrezzatureNomi,
                        errors = errors
                    )
                )
            } catch (e: Exception) {
                _state.value = GiornalieraImportState.Error("Errore durante la scansione: ${e.message}")
            }
        }
    }

    fun confirmImport() {
        val currentState = _state.value
        if (currentState !is GiornalieraImportState.Parsed) return

        val result = currentState.result
        _state.value = GiornalieraImportState.Importing("Importazione in corso...")

        viewModelScope.launch {
            try {
                var importedCount = 0

                for ((index, parsed) in result.giornaliere.withIndex()) {
                    _state.value = GiornalieraImportState.Importing(
                        "Importazione ${index + 1}/${result.giornaliere.size}..."
                    )

                    // 1. Commessa: find or create
                    var commessa = commessaRepository.getByNome(parsed.commessaNomeInt)
                    if (commessa == null) {
                        val commessaId = commessaRepository.insert(
                            Commessa(
                                nome = parsed.commessaNomeInt,
                                cliente = parsed.cliente,
                                indirizzo = ""
                            )
                        )
                        commessa = commessaRepository.getById(commessaId)!!
                    }

                    // 2. Cantiere: find or create
                    val cantieri = cantiereRepository.getByCommessaIdSync(commessa.id)
                    val cantiere = if (cantieri.isNotEmpty()) {
                        cantieri.first()
                    } else {
                        val cantiereId = cantiereRepository.insert(
                            Cantiere(
                                commessaId = commessa.id,
                                nome = formatCommessaNome(parsed.commessaNomeInt),
                                indirizzo = ""
                            )
                        )
                        cantiereRepository.getById(cantiereId)!!
                    }

                    // 3. Duplicates: delete existing giornaliera for same cantiere+date
                    val endOfDay = parsed.data + 86_399_999L
                    val existing = giornalieraRepository.getByCantiereIdAndDateRange(
                        cantiere.id, parsed.data, endOfDay
                    )
                    for (existingGiornaliera in existing) {
                        giornalieraRepository.deleteById(existingGiornaliera.id)
                    }

                    // 4. Insert giornaliera
                    val giornalieraId = giornalieraRepository.insert(
                        Giornaliera(
                            cantiereId = cantiere.id,
                            data = parsed.data,
                            stato = StatoGiornaliera.INVIATA
                        )
                    )

                    // 5. Personale
                    for (pRow in parsed.personaleRows) {
                        var personale = personaleRepository.getByNomeECognome(pRow.nome, pRow.cognome)
                        if (personale == null) {
                            val pId = personaleRepository.insert(
                                Personale(nome = pRow.nome, cognome = pRow.cognome)
                            )
                            personale = personaleRepository.getById(pId)!!
                        }
                        personaleGiornalieraRepository.insert(
                            PersonaleGiornaliera(
                                giornalieraId = giornalieraId,
                                personaleId = personale.id,
                                oreLavoro = pRow.oreLavoro,
                                oreViaggioAndata = pRow.oreViaggioAndata,
                                oreViaggioRitorno = pRow.oreViaggioRitorno,
                                inizioTurno = pRow.inizioTurno,
                                fineTurno = pRow.fineTurno,
                                pernotto = pRow.pernotto
                            )
                        )
                    }

                    // 6. Mezzi
                    for (mRow in parsed.mezzoRows) {
                        var mezzo = if (mRow.targa.isNotBlank()) {
                            mezzoRepository.getByTarga(mRow.targa)
                        } else null
                        if (mezzo == null) {
                            val mId = mezzoRepository.insert(
                                Mezzo(
                                    nome = mRow.nome,
                                    targa = mRow.targa,
                                    codiceArticoloKm = mRow.codiceArticoloKm,
                                    codiceArticoloGiorni = mRow.codiceArticoloGiorni
                                )
                            )
                            mezzo = mezzoRepository.getById(mId)!!
                        }
                        mezzoGiornalieraRepository.insert(
                            MezzoGiornaliera(
                                giornalieraId = giornalieraId,
                                mezzoId = mezzo.id,
                                kmAndata = mRow.kmAndata,
                                kmRitorno = mRow.kmRitorno
                            )
                        )
                    }

                    // 7. Attrezzature
                    for (attrNome in parsed.attrezzatureNomi) {
                        var attrezzatura = attrezzaturaRepository.getByNome(attrNome)
                        if (attrezzatura == null) {
                            attrezzaturaRepository.insert(Attrezzatura(nome = attrNome))
                            attrezzatura = attrezzaturaRepository.getByNome(attrNome)!!
                        }
                        attrezzaturaGiornalieraRepository.insert(
                            AttrezzaturaGiornaliera(
                                giornalieraId = giornalieraId,
                                attrezzaturaId = attrezzatura.id
                            )
                        )
                    }

                    importedCount++
                }

                _state.value = GiornalieraImportState.Success(
                    "Importate $importedCount giornaliere da ${result.fileCount} file"
                )
            } catch (e: Exception) {
                _state.value = GiornalieraImportState.Error("Errore durante l'importazione: ${e.message}")
            }
        }
    }

    fun resetState() {
        _state.value = GiornalieraImportState.Idle
    }

    companion object {
        fun splitCsvLine(line: String): List<String> {
            val result = mutableListOf<String>()
            val current = StringBuilder()
            var inQuotes = false

            for (char in line) {
                when {
                    char == '"' -> inQuotes = !inQuotes
                    char == ';' && !inQuotes -> {
                        result.add(current.toString())
                        current.clear()
                    }
                    else -> current.append(char)
                }
            }
            result.add(current.toString())
            return result
        }

        fun parseItalianFloat(str: String?): Float {
            val cleaned = str?.trim()?.removeSurrounding("\"")?.trim() ?: return 0f
            if (cleaned.isEmpty()) return 0f
            return try {
                cleaned.replace(',', '.').toFloat()
            } catch (e: NumberFormatException) {
                0f
            }
        }

        fun parseTimeToMillis(str: String?): Long {
            val cleaned = str?.trim()?.removeSurrounding("\"")?.trim() ?: return 0L
            if (cleaned.isEmpty()) return 0L
            return try {
                val parts = cleaned.split(":")
                if (parts.size != 2) return 0L
                val hours = parts[0].toInt()
                val minutes = parts[1].toInt()
                (hours * 60L + minutes) * 60L * 1000L
            } catch (e: Exception) {
                0L
            }
        }

        fun parseDate(str: String): Long {
            val cleaned = str.removeSurrounding("\"").trim()
            val format = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY)
            val date = format.parse(cleaned)
                ?: throw IllegalArgumentException("Data non valida: $cleaned")
            val calendar = Calendar.getInstance().apply {
                time = date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return calendar.timeInMillis
        }

        fun parseCommessaNome(str: String): Int {
            val cleaned = str.removeSurrounding("\"").trim()
            return cleaned.toIntOrNull()
                ?: throw IllegalArgumentException("Numero commessa non valido: $cleaned")
        }
    }
}
