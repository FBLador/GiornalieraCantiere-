package com.idroambiente.giornalieradicantiere.database.repository

import com.idroambiente.giornalieradicantiere.database.dao.MezzoDao
import com.idroambiente.giornalieradicantiere.database.entities.Mezzo
import kotlinx.coroutines.flow.Flow

class MezzoRepository(private val mezzoDao: MezzoDao) {
    fun getAll(): Flow<List<Mezzo>> = mezzoDao.getAll()

    fun getAllIncludingInactive(): Flow<List<Mezzo>> = mezzoDao.getAllIncludingInactive()

    /** "Elimina" dalle liste: il mezzo resta nelle giornaliere già compilate. */
    suspend fun disattiva(id: Long) = mezzoDao.disattiva(id)

    suspend fun disattivaTutti() = mezzoDao.disattivaTutti()

    /** Inserisce i mezzi; se la targa esiste già (anche eliminata) lo aggiorna e lo riattiva. */
    suspend fun importa(mezzi: List<Mezzo>) {
        mezzi.forEach { m ->
            val esistente = mezzoDao.getByTarga(m.targa)
            if (esistente != null) {
                mezzoDao.update(m.copy(id = esistente.id, attivo = true))
            } else {
                mezzoDao.insert(m.copy(id = 0, attivo = true))
            }
        }
    }

    suspend fun getById(id: Long): Mezzo? = mezzoDao.getById(id)

    suspend fun getByIds(ids: List<Long>): List<Mezzo> = mezzoDao.getByIds(ids)

    fun search(query: String): Flow<List<Mezzo>> = mezzoDao.search(query)

    suspend fun getByTarga(targa: String): Mezzo? = mezzoDao.getByTarga(targa)

    suspend fun insert(mezzo: Mezzo): Long = mezzoDao.insert(mezzo)

    suspend fun insertAll(mezzi: List<Mezzo>) = mezzoDao.insertAll(mezzi)

    suspend fun update(mezzo: Mezzo) = mezzoDao.update(mezzo)

    suspend fun delete(mezzo: Mezzo) = mezzoDao.delete(mezzo)

    suspend fun deleteById(id: Long) = mezzoDao.deleteById(id)

    suspend fun deleteAll() = mezzoDao.deleteAll()
}
