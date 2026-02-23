package com.idroambiente.giornalieradicantiere.ui.impostazioni

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idroambiente.giornalieradicantiere.database.entities.Attrezzatura
import com.idroambiente.giornalieradicantiere.database.entities.Mezzo
import com.idroambiente.giornalieradicantiere.database.entities.Personale
import com.idroambiente.giornalieradicantiere.database.repository.AttrezzaturaRepository
import com.idroambiente.giornalieradicantiere.database.repository.MezzoRepository
import com.idroambiente.giornalieradicantiere.database.repository.PersonaleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

data class CsvParseResult(
    val personale: List<Personale>,
    val mezzi: List<Mezzo>,
    val attrezzature: List<Attrezzatura>,
    val errors: List<String>
)

sealed class CsvImportState {
    data object Idle : CsvImportState()
    data class Parsed(val result: CsvParseResult) : CsvImportState()
    data object Importing : CsvImportState()
    data object Success : CsvImportState()
    data class Error(val message: String) : CsvImportState()
}

@HiltViewModel
class CsvImportViewModel @Inject constructor(
    private val personaleRepository: PersonaleRepository,
    private val mezzoRepository: MezzoRepository,
    private val attrezzaturaRepository: AttrezzaturaRepository
) : ViewModel() {

    private val _state = MutableStateFlow<CsvImportState>(CsvImportState.Idle)
    val state: StateFlow<CsvImportState> = _state.asStateFlow()

    fun parseCsv(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            try {
                val personaleList = mutableListOf<Personale>()
                val mezziList = mutableListOf<Mezzo>()
                val attrezzatureList = mutableListOf<Attrezzatura>()
                val errors = mutableListOf<String>()

                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val lines = reader.readLines()

                    if (lines.isEmpty()) {
                        _state.value = CsvImportState.Error("Il file CSV è vuoto")
                        return@launch
                    }

                    val startIndex = if (isHeaderRow(lines.first())) 1 else 0

                    for (i in startIndex until lines.size) {
                        val line = lines[i].trim()
                        if (line.isEmpty()) continue

                        val parts = line.split(",").map { it.trim() }
                        if (parts.size < 2) {
                            errors.add("Riga ${i + 1}: formato non valido (servono almeno 2 colonne)")
                            continue
                        }

                        val tipo = parts[0].lowercase()
                        val campo1 = parts[1]
                        val campo2 = if (parts.size > 2) parts[2] else ""

                        when (tipo) {
                            "personale" -> {
                                if (campo1.isBlank() || campo2.isBlank()) {
                                    errors.add("Riga ${i + 1}: nome e cognome sono obbligatori per il personale")
                                } else {
                                    personaleList.add(Personale(nome = campo1, cognome = campo2))
                                }
                            }
                            "mezzo" -> {
                                if (campo1.isBlank()) {
                                    errors.add("Riga ${i + 1}: il nome del mezzo è obbligatorio")
                                } else {
                                    val codiceArticoloKm = if (parts.size > 3) parts[3].uppercase().trim() else ""
                                    val codiceArticoloGiorni = if (parts.size > 4) parts[4].uppercase().trim() else ""
                                    mezziList.add(Mezzo(
                                        nome = campo1,
                                        targa = campo2,
                                        codiceArticoloKm = codiceArticoloKm,
                                        codiceArticoloGiorni = codiceArticoloGiorni
                                    ))
                                }
                            }
                            "attrezzatura" -> {
                                if (campo1.isBlank()) {
                                    errors.add("Riga ${i + 1}: il nome dell'attrezzatura è obbligatorio")
                                } else {
                                    val codiceArticolo = if (parts.size > 2) parts[2].uppercase().trim() else ""
                                    attrezzatureList.add(Attrezzatura(
                                        nome = campo1,
                                        codiceArticolo = codiceArticolo
                                    ))
                                }
                            }
                            else -> {
                                errors.add("Riga ${i + 1}: tipo \"${parts[0]}\" non riconosciuto (usare: personale, mezzo, attrezzatura)")
                            }
                        }
                    }

                    if (personaleList.isEmpty() && mezziList.isEmpty() && attrezzatureList.isEmpty()) {
                        _state.value = CsvImportState.Error(
                            if (errors.isNotEmpty()) {
                                "Nessun dato valido trovato.\n\n" + errors.joinToString("\n")
                            } else {
                                "Il file CSV non contiene dati da importare"
                            }
                        )
                        return@launch
                    }

                    _state.value = CsvImportState.Parsed(
                        CsvParseResult(
                            personale = personaleList,
                            mezzi = mezziList,
                            attrezzature = attrezzatureList,
                            errors = errors
                        )
                    )
                } ?: run {
                    _state.value = CsvImportState.Error("Impossibile aprire il file")
                }
            } catch (e: Exception) {
                _state.value = CsvImportState.Error("Errore nella lettura del file: ${e.message}")
            }
        }
    }

    fun confirmImport(replaceExisting: Boolean) {
        val currentState = _state.value
        if (currentState !is CsvImportState.Parsed) return

        val result = currentState.result
        _state.value = CsvImportState.Importing

        viewModelScope.launch {
            try {
                if (replaceExisting) {
                    personaleRepository.deleteAll()
                    mezzoRepository.deleteAll()
                    attrezzaturaRepository.deleteAll()
                }

                if (result.personale.isNotEmpty()) {
                    personaleRepository.insertAll(result.personale)
                }
                if (result.mezzi.isNotEmpty()) {
                    mezzoRepository.insertAll(result.mezzi)
                }
                if (result.attrezzature.isNotEmpty()) {
                    attrezzaturaRepository.insertAll(result.attrezzature)
                }

                _state.value = CsvImportState.Success
            } catch (e: Exception) {
                _state.value = CsvImportState.Error("Errore durante l'importazione: ${e.message}")
            }
        }
    }

    fun resetState() {
        _state.value = CsvImportState.Idle
    }

    private fun isHeaderRow(line: String): Boolean {
        val parts = line.split(",").map { it.trim().lowercase() }
        return parts.firstOrNull() == "tipo"
    }
}
