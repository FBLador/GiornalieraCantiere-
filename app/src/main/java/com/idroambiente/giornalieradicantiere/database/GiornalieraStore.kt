package com.idroambiente.giornalieradicantiere.database

import androidx.room.withTransaction
import com.idroambiente.giornalieradicantiere.database.entities.AttrezzaturaGiornaliera
import com.idroambiente.giornalieradicantiere.database.entities.Giornaliera
import com.idroambiente.giornalieradicantiere.database.entities.MezzoGiornaliera
import com.idroambiente.giornalieradicantiere.database.entities.PersonaleGiornaliera
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scritture "tutto o niente" di una giornaliera con le sue righe
 * (personale, mezzi, attrezzature).
 *
 * Prima le righe venivano cancellate e reinserite in passaggi separati:
 * se l'app si chiudeva a metà, la giornaliera restava senza persone.
 */
@Singleton
class GiornalieraStore @Inject constructor(
    private val db: AppDatabase
) {
    /** Righe collegate a una giornaliera, senza l'id della giornaliera (lo mette lo store). */
    data class Righe(
        val personale: List<PersonaleGiornaliera>,
        val mezzi: List<MezzoGiornaliera>,
        val attrezzature: List<AttrezzaturaGiornaliera>
    )

    /**
     * Inserisce (id = 0) o aggiorna la giornaliera e sostituisce tutte le sue righe.
     * @return l'id della giornaliera salvata
     */
    suspend fun salva(giornaliera: Giornaliera, righe: Righe): Long = db.withTransaction {
        val id = if (giornaliera.id == 0L) {
            db.giornalieraDao().insert(giornaliera)
        } else {
            db.giornalieraDao().update(giornaliera)
            giornaliera.id
        }
        sostituisciRighe(id, righe)
        id
    }

    private suspend fun sostituisciRighe(giornalieraId: Long, righe: Righe) {
        db.personaleGiornalieraDao().deleteByGiornalieraId(giornalieraId)
        db.mezzoGiornalieraDao().deleteByGiornalieraId(giornalieraId)
        db.attrezzaturaGiornalieraDao().deleteByGiornalieraId(giornalieraId)
        db.personaleGiornalieraDao().insertAll(righe.personale.map { it.copy(id = 0, giornalieraId = giornalieraId) })
        db.mezzoGiornalieraDao().insertAll(righe.mezzi.map { it.copy(id = 0, giornalieraId = giornalieraId) })
        db.attrezzaturaGiornalieraDao().insertAll(righe.attrezzature.map { it.copy(giornalieraId = giornalieraId) })
    }
}
