package com.idroambiente.giornalieradicantiere.database.repository

import com.idroambiente.giornalieradicantiere.database.dao.AttrezzaturaDao
import com.idroambiente.giornalieradicantiere.database.entities.Attrezzatura
import kotlinx.coroutines.flow.Flow

class AttrezzaturaRepository(private val attrezzaturaDao: AttrezzaturaDao) {
    fun getAll(): Flow<List<Attrezzatura>> = attrezzaturaDao.getAll()

    fun getAllIncludingInactive(): Flow<List<Attrezzatura>> = attrezzaturaDao.getAllIncludingInactive()

    /** "Elimina" dalle liste: l'attrezzatura resta nelle giornaliere già compilate. */
    suspend fun disattiva(id: Long) = attrezzaturaDao.disattiva(id)

    suspend fun disattivaTutti() = attrezzaturaDao.disattivaTutti()

    /** Inserisce le attrezzature; se il nome esiste già (anche eliminata) la aggiorna e la riattiva. */
    suspend fun importa(attrezzature: List<Attrezzatura>) {
        attrezzature.forEach { a ->
            val esistente = attrezzaturaDao.getByNome(a.nome)
            if (esistente != null) {
                attrezzaturaDao.update(a.copy(id = esistente.id, attivo = true))
            } else {
                attrezzaturaDao.insert(a.copy(id = 0, attivo = true))
            }
        }
    }

    suspend fun getById(id: Long): Attrezzatura? = attrezzaturaDao.getById(id)

    suspend fun getByIds(ids: List<Long>): List<Attrezzatura> = attrezzaturaDao.getByIds(ids)

    fun search(query: String): Flow<List<Attrezzatura>> = attrezzaturaDao.search(query)

    suspend fun insert(attrezzatura: Attrezzatura): Long = attrezzaturaDao.insert(attrezzatura)

    suspend fun insertAll(attrezzature: List<Attrezzatura>) = attrezzaturaDao.insertAll(attrezzature)

    suspend fun update(attrezzatura: Attrezzatura) = attrezzaturaDao.update(attrezzatura)

    suspend fun delete(attrezzatura: Attrezzatura) = attrezzaturaDao.delete(attrezzatura)

    suspend fun deleteById(id: Long) = attrezzaturaDao.deleteById(id)

    suspend fun deleteAll() = attrezzaturaDao.deleteAll()

    suspend fun getByNome(nome: String): Attrezzatura? = attrezzaturaDao.getByNome(nome)
}
