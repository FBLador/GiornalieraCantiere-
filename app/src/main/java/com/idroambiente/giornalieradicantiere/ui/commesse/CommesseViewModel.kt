package com.idroambiente.giornalieradicantiere.ui.commesse

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idroambiente.giornalieradicantiere.database.entities.Cantiere
import com.idroambiente.giornalieradicantiere.database.entities.Commessa
import com.idroambiente.giornalieradicantiere.database.entities.Giornaliera
import com.idroambiente.giornalieradicantiere.database.entities.StatoCommessa
import com.idroambiente.giornalieradicantiere.database.entities.StatoGiornaliera
import com.idroambiente.giornalieradicantiere.database.repository.CantiereRepository
import com.idroambiente.giornalieradicantiere.database.repository.CommessaRepository
import com.idroambiente.giornalieradicantiere.database.repository.GiornalieraRepository
import com.idroambiente.giornalieradicantiere.ui.common.Formati
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Cosa propone il pulsante grande di una commessa. */
sealed interface AzioneCommessa {
    /** Giornaliera di oggi già inviata/completata: apre il dettaglio */
    data class OggiFatta(val giornaliera: Giornaliera) : AzioneCommessa
    data class RiprendiBozza(val giornaliera: Giornaliera, val oggi: Boolean) : AzioneCommessa
    data object VediGiornaliere : AzioneCommessa
    data object CompilaOggi : AzioneCommessa
}

data class CommessaCard(
    val commessa: Commessa,
    val cantiereId: Long?,
    val numeroGiornaliere: Int,
    val azione: AzioneCommessa
)

/** Regola del prototipo per scegliere l'azione principale. */
fun azionePer(commessa: Commessa, giornaliere: List<Giornaliera>, adesso: Long): AzioneCommessa {
    val oggi = giornaliere.firstOrNull { Formati.stessoGiorno(it.data, adesso) && it.stato != StatoGiornaliera.BOZZA }
    val bozza = giornaliere.filter { it.stato == StatoGiornaliera.BOZZA }.maxByOrNull { it.data }
    return when {
        oggi != null -> AzioneCommessa.OggiFatta(oggi)
        bozza != null -> AzioneCommessa.RiprendiBozza(bozza, Formati.stessoGiorno(bozza.data, adesso))
        commessa.stato == StatoCommessa.CHIUSA -> AzioneCommessa.VediGiornaliere
        else -> AzioneCommessa.CompilaOggi
    }
}

@HiltViewModel
class CommesseViewModel @Inject constructor(
    private val commessaRepository: CommessaRepository,
    private val cantiereRepository: CantiereRepository,
    giornalieraRepository: GiornalieraRepository
) : ViewModel() {

    companion object {
        private const val TAG = "CommesseViewModel"
    }

    val cards: StateFlow<List<CommessaCard>> = combine(
        commessaRepository.getAll(),
        cantiereRepository.getAll(),
        giornalieraRepository.getAll()
    ) { commesse, cantieri, giornaliere ->
        val adesso = System.currentTimeMillis()
        val cantierePerCommessa = cantieri.groupBy { it.commessaId }
        commesse.map { c ->
            val ids = cantierePerCommessa[c.id].orEmpty().map { it.id }.toSet()
            val gs = giornaliere.filter { it.cantiereId in ids }
            CommessaCard(
                commessa = c,
                cantiereId = cantierePerCommessa[c.id]?.firstOrNull()?.id,
                numeroGiornaliere = gs.size,
                azione = azionePer(c, gs, adesso)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** Messaggio breve da mostrare (es. "Commessa 0001234 creata"). */
    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    fun showCreateDialog() {
        _errorMessage.value = null
        _showCreateDialog.value = true
    }

    fun hideCreateDialog() {
        _showCreateDialog.value = false
    }

    fun toastShown() {
        _toast.value = null
    }

    fun createCommessa(nome: Int, cliente: String, indirizzo: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                if (commessaRepository.getByNome(nome) != null) {
                    _errorMessage.value = "Esiste già una commessa con questo codice"
                    return@launch
                }
                val commessaId = commessaRepository.insert(
                    Commessa(nome = nome, cliente = cliente.trim(), indirizzo = indirizzo, stato = StatoCommessa.APERTA)
                )
                // Ogni commessa ha un cantiere "nascosto" a cui sono legate le giornaliere
                cantiereRepository.insert(
                    Cantiere(commessaId = commessaId, nome = nome.toString().padStart(7, '0'), indirizzo = indirizzo)
                )
                hideCreateDialog()
                _toast.value = "Commessa ${nome.toString().padStart(7, '0')} creata"
            } catch (e: Exception) {
                Log.e(TAG, "Errore creazione commessa", e)
                _errorMessage.value = "Errore durante la creazione della commessa: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Restituisce il cantiere della commessa, creandolo se manca (commesse vecchie). */
    fun conCantiere(commessaId: Long, azione: (Long) -> Unit) {
        viewModelScope.launch {
            val esistente = cantiereRepository.getByCommessaIdSync(commessaId).firstOrNull()
            val id = esistente?.id ?: run {
                val c = commessaRepository.getById(commessaId) ?: return@launch
                cantiereRepository.insert(Cantiere(commessaId = c.id, nome = c.nome.toString().padStart(7, '0'), indirizzo = c.indirizzo))
            }
            azione(id)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
