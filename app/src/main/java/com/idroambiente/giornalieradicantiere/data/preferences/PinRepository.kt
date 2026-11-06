package com.idroambiente.giornalieradicantiere.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Impronta del PIN (PBKDF2-SHA256 con sale). Nel codice e nel telefono
 * c'è solo l'impronta, mai il PIN in chiaro.
 *
 * Nota onesta: un PIN di 6 cifre serve a evitare modifiche per sbaglio o "di passaggio",
 * non è una protezione forte. La vera tutela è la traccia: motivo, revisione, registro.
 */
object PinHasher {
    const val ITERAZIONI = 60_000
    private const val LUNGHEZZA_BIT = 256

    fun impronta(pin: String, sale: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), sale, ITERAZIONI, LUNGHEZZA_BIT)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    fun verifica(pin: String, saleB64: String, improntaB64: String): Boolean {
        val attesa = Base64.getDecoder().decode(improntaB64)
        val calcolata = impronta(pin, Base64.getDecoder().decode(saleB64))
        return MessageDigest.isEqual(attesa, calcolata)
    }

    fun nuovoSale(): ByteArray = ByteArray(16).also { SecureRandom().nextBytes(it) }

    fun b64(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
}

/** PIN predefinito aziendale (202900): solo sale e impronta. */
object PinPredefinito {
    const val SALE = "eBU7bDlW6vSE2uMyvX9tjw=="
    const val IMPRONTA = "981tuOHkF9dfjKZKRc1N9UjRecMF8ZEFXfFyLXSip0U="
}

/** Regole dei tentativi: 5 errori → blocco di 5 minuti. */
object PinPolicy {
    const val MAX_TENTATIVI = 5
    const val BLOCCO_MS = 5 * 60 * 1000L

    data class Stato(val tentativiErrati: Int, val bloccatoFinoA: Long)

    sealed interface Esito {
        data object Corretto : Esito
        data class Errato(val tentativiRimasti: Int) : Esito
        data class Bloccato(val millisRimanenti: Long) : Esito
    }

    /** Calcola esito e nuovo stato di un tentativo. */
    fun valuta(stato: Stato, corretto: Boolean, adesso: Long): Pair<Esito, Stato> {
        if (stato.bloccatoFinoA > adesso) {
            return Esito.Bloccato(stato.bloccatoFinoA - adesso) to stato
        }
        if (corretto) return Esito.Corretto to Stato(0, 0)
        val errati = stato.tentativiErrati + 1
        return if (errati >= MAX_TENTATIVI) {
            Esito.Bloccato(BLOCCO_MS) to Stato(0, adesso + BLOCCO_MS)
        } else {
            Esito.Errato(MAX_TENTATIVI - errati) to Stato(errati, 0)
        }
    }
}

/**
 * PIN del responsabile per modificare le giornaliere già inviate.
 * Finché non viene cambiato su questo telefono vale il PIN predefinito aziendale.
 */
@Singleton
class PinRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    /** true se su questo telefono vale ancora il PIN predefinito aziendale. */
    val usaPinPredefinito: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.PIN_HASH] == null }

    /** Millisecondi di blocco rimanenti (0 = non bloccato). */
    suspend fun bloccoRimanente(adesso: Long = System.currentTimeMillis()): Long {
        val fino = dataStore.data.first()[PreferencesKeys.PIN_BLOCCATO_FINO_A] ?: 0L
        return (fino - adesso).coerceAtLeast(0)
    }

    suspend fun verifica(pin: String, adesso: Long = System.currentTimeMillis()): PinPolicy.Esito {
        val prefs = dataStore.data.first()
        val stato = PinPolicy.Stato(
            prefs[PreferencesKeys.PIN_TENTATIVI_ERRATI] ?: 0,
            prefs[PreferencesKeys.PIN_BLOCCATO_FINO_A] ?: 0L
        )
        if (stato.bloccatoFinoA > adesso) return PinPolicy.Esito.Bloccato(stato.bloccatoFinoA - adesso)

        val sale = prefs[PreferencesKeys.PIN_SALT] ?: PinPredefinito.SALE
        val impronta = prefs[PreferencesKeys.PIN_HASH] ?: PinPredefinito.IMPRONTA
        val corretto = withContext(Dispatchers.Default) { PinHasher.verifica(pin, sale, impronta) }

        val (esito, nuovo) = PinPolicy.valuta(stato, corretto, adesso)
        dataStore.edit {
            it[PreferencesKeys.PIN_TENTATIVI_ERRATI] = nuovo.tentativiErrati
            it[PreferencesKeys.PIN_BLOCCATO_FINO_A] = nuovo.bloccatoFinoA
        }
        return esito
    }

    /** Imposta un nuovo PIN su questo telefono (va chiamato dopo aver verificato quello attuale). */
    suspend fun impostaNuovoPin(pin: String) {
        require(pin.length == 6 && pin.all { it.isDigit() }) { "Il PIN deve avere 6 cifre" }
        val sale = PinHasher.nuovoSale()
        val impronta = withContext(Dispatchers.Default) { PinHasher.impronta(pin, sale) }
        dataStore.edit {
            it[PreferencesKeys.PIN_SALT] = PinHasher.b64(sale)
            it[PreferencesKeys.PIN_HASH] = PinHasher.b64(impronta)
            it[PreferencesKeys.PIN_TENTATIVI_ERRATI] = 0
            it[PreferencesKeys.PIN_BLOCCATO_FINO_A] = 0L
        }
    }
}
