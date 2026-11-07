package com.idroambiente.giornalieradicantiere.ui.impostazioni

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idroambiente.giornalieradicantiere.data.preferences.PinPolicy
import com.idroambiente.giornalieradicantiere.data.preferences.PinRepository
import com.idroambiente.giornalieradicantiere.data.preferences.PreferencesRepository
import com.idroambiente.giornalieradicantiere.data.preferences.TemaApp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Passi del cambio PIN: prima quello attuale, poi il nuovo due volte. */
enum class PassoPin { ATTUALE, NUOVO, CONFERMA }

data class CambioPinState(
    val passo: PassoPin = PassoPin.ATTUALE,
    val cifre: String = "",
    val errore: String? = null,
    val bloccato: Boolean = false,
    val nuovo: String? = null
)

/** Tema e PIN (le altre impostazioni hanno già i loro ViewModel). */
@HiltViewModel
class ImpostazioniViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val pinRepository: PinRepository
) : ViewModel() {

    val tema: StateFlow<TemaApp> = preferencesRepository.tema
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TemaApp.SISTEMA)

    val pinPredefinito: StateFlow<Boolean> = pinRepository.usaPinPredefinito
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _cambioPin = MutableStateFlow<CambioPinState?>(null)
    val cambioPin: StateFlow<CambioPinState?> = _cambioPin.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    fun setTema(tema: TemaApp) {
        viewModelScope.launch { preferencesRepository.setTema(tema) }
    }

    fun apriCambioPin() {
        viewModelScope.launch {
            val blocco = pinRepository.bloccoRimanente()
            _cambioPin.value = CambioPinState(
                errore = if (blocco > 0) "Troppi tentativi. Riprova tra ${(blocco + 59_999) / 60_000} minuti." else null,
                bloccato = blocco > 0
            )
        }
    }

    fun annullaCambioPin() {
        _cambioPin.value = null
    }

    fun cancella() = _cambioPin.update { it?.copy(cifre = it.cifre.dropLast(1)) }

    fun cifra(c: Char) {
        val s = _cambioPin.value ?: return
        if (s.bloccato || s.cifre.length >= 6) return
        val cifre = s.cifre + c
        _cambioPin.value = s.copy(cifre = cifre, errore = null)
        if (cifre.length < 6) return
        viewModelScope.launch {
            when (s.passo) {
                PassoPin.ATTUALE -> when (val esito = pinRepository.verifica(cifre)) {
                    PinPolicy.Esito.Corretto -> _cambioPin.value = CambioPinState(passo = PassoPin.NUOVO)
                    is PinPolicy.Esito.Errato -> _cambioPin.value = CambioPinState(
                        errore = "PIN errato · ${esito.tentativiRimasti} ${if (esito.tentativiRimasti == 1) "tentativo rimasto" else "tentativi rimasti"}"
                    )
                    is PinPolicy.Esito.Bloccato -> _cambioPin.value = CambioPinState(
                        errore = "Troppi tentativi. Riprova tra ${(esito.millisRimanenti + 59_999) / 60_000} minuti.",
                        bloccato = true
                    )
                }
                PassoPin.NUOVO -> _cambioPin.value = CambioPinState(passo = PassoPin.CONFERMA, nuovo = cifre)
                PassoPin.CONFERMA -> if (cifre == s.nuovo) {
                    pinRepository.impostaNuovoPin(cifre)
                    _cambioPin.value = null
                    _toast.value = "PIN aggiornato"
                } else {
                    _cambioPin.value = CambioPinState(passo = PassoPin.NUOVO, errore = "I due PIN non coincidono. Riprova.")
                }
            }
        }
    }

    fun toastMostrato() {
        _toast.value = null
    }
}
