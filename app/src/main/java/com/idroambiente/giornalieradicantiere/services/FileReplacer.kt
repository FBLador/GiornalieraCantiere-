package com.idroambiente.giornalieradicantiere.services

/** Risultato di una singola operazione su un archivio (Drive, cartella del telefono, cartella dell'app). */
sealed interface OpResult {
    data class Ok(val ref: String, val name: String, val webViewLink: String? = null) : OpResult
    /** Il file non esiste più (cancellato o spostato). */
    data object NotFound : OpResult
    /** Il file esiste ma l'app non può modificarlo (per esempio creato da un altro account Google). */
    data object Forbidden : OpResult
    /** Errore temporaneo (rete, spazio...): NON si deve creare un doppione. */
    data class Failed(val message: String) : OpResult
}

sealed interface FindResult {
    /** Riferimenti trovati, il più recente per primo. */
    data class Found(val refs: List<String>) : FindResult
    data class Failed(val message: String) : FindResult
}

/** Le tre operazioni che servono per sostituire un file in un archivio. */
interface RemoteOps {
    /** Sostituisce il contenuto del file e, se serve, lo rinomina. */
    suspend fun update(ref: String, newName: String): OpResult
    suspend fun findByName(name: String): FindResult
    suspend fun create(name: String): OpResult
}

enum class EsitoSostituzione {
    /** Stesso file, contenuto nuovo. */
    SOSTITUITO,
    /** Stesso file, contenuto e nome nuovi (data cambiata). */
    RINOMINATO,
    /** Il file originale non c'era più: ne è stato creato uno nuovo. */
    CREATO_NON_TROVATO,
    /** Il file originale non si poteva modificare: ne è stato creato uno nuovo con "_revN". */
    CREATO_SENZA_PERMESSI
}

sealed interface ReplaceResult {
    data class Done(
        val ref: String,
        val name: String,
        val webViewLink: String?,
        val esito: EsitoSostituzione,
        /** Nome che aveva il file prima (se è stato rinominato). */
        val previousName: String?
    ) : ReplaceResult

    data class Failed(val message: String) : ReplaceResult
}

/**
 * Decide come sostituire un file senza creare doppioni.
 *
 * 1. Se conosciamo il file (ID salvato all'invio) lo aggiorniamo.
 * 2. Altrimenti lo cerchiamo per nome (giornaliere inviate prima di questa versione).
 * 3. Se non c'è, lo creiamo.
 * Se non abbiamo i permessi, creiamo un file nuovo con [fallbackName] (es. "_rev2").
 * Se c'è un errore temporaneo ci fermiamo: meglio riprovare che duplicare.
 */
object FileReplacer {

    suspend fun replace(
        knownRef: String?,
        oldName: String,
        newName: String,
        fallbackName: String,
        ops: RemoteOps
    ): ReplaceResult {
        if (knownRef != null) {
            when (val r = ops.update(knownRef, newName)) {
                is OpResult.Ok -> return done(r, oldName, newName)
                OpResult.Forbidden -> return createNew(fallbackName, EsitoSostituzione.CREATO_SENZA_PERMESSI, ops)
                is OpResult.Failed -> return ReplaceResult.Failed(r.message)
                OpResult.NotFound -> Unit // proviamo a cercarlo per nome
            }
        }

        val nomiDaCercare = listOf(oldName, newName).distinct()
        for (nome in nomiDaCercare) {
            when (val found = ops.findByName(nome)) {
                is FindResult.Failed -> return ReplaceResult.Failed(found.message)
                is FindResult.Found -> {
                    val ref = found.refs.firstOrNull() ?: continue
                    return when (val r = ops.update(ref, newName)) {
                        is OpResult.Ok -> done(r, nome, newName)
                        OpResult.Forbidden -> createNew(fallbackName, EsitoSostituzione.CREATO_SENZA_PERMESSI, ops)
                        OpResult.NotFound -> createNew(newName, EsitoSostituzione.CREATO_NON_TROVATO, ops)
                        is OpResult.Failed -> ReplaceResult.Failed(r.message)
                    }
                }
            }
        }

        return createNew(newName, EsitoSostituzione.CREATO_NON_TROVATO, ops)
    }

    private fun done(r: OpResult.Ok, oldName: String, newName: String): ReplaceResult.Done {
        val rinominato = oldName != newName
        return ReplaceResult.Done(
            ref = r.ref,
            name = r.name,
            webViewLink = r.webViewLink,
            esito = if (rinominato) EsitoSostituzione.RINOMINATO else EsitoSostituzione.SOSTITUITO,
            previousName = if (rinominato) oldName else null
        )
    }

    private suspend fun createNew(name: String, esito: EsitoSostituzione, ops: RemoteOps): ReplaceResult =
        when (val r = ops.create(name)) {
            is OpResult.Ok -> ReplaceResult.Done(r.ref, r.name, r.webViewLink, esito, previousName = null)
            is OpResult.Failed -> ReplaceResult.Failed(r.message)
            OpResult.NotFound -> ReplaceResult.Failed("Cartella di destinazione non trovata")
            OpResult.Forbidden -> ReplaceResult.Failed("Permessi insufficienti per creare il file")
        }
}
