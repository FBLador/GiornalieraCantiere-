package com.idroambiente.giornalieradicantiere.database.repository

import com.idroambiente.giornalieradicantiere.database.dao.PersonaleDao
import com.idroambiente.giornalieradicantiere.database.entities.Personale
import kotlinx.coroutines.flow.Flow

class PersonaleRepository(private val personaleDao: PersonaleDao) {
    fun getAll(): Flow<List<Personale>> = personaleDao.getAll()

    fun getAllIncludingInactive(): Flow<List<Personale>> = personaleDao.getAllIncludingInactive()

    /** "Elimina" dalle liste: la persona resta nelle giornaliere già compilate. */
    suspend fun disattiva(id: Long) = personaleDao.disattiva(id)

    suspend fun disattivaTutti() = personaleDao.disattivaTutti()

    /**
     * Inserisce le persone; se una esiste già (anche eliminata) la riattiva
     * invece di crearne un doppione.
     */
    suspend fun importa(personale: List<Personale>) {
        personale.forEach { p ->
            val esistente = personaleDao.getByNomeECognome(p.nome, p.cognome)
            if (esistente != null) {
                personaleDao.update(esistente.copy(nome = p.nome.trim(), cognome = p.cognome.trim(), attivo = true))
            } else {
                personaleDao.insert(p.copy(id = 0, attivo = true))
            }
        }
    }

    suspend fun getById(id: Long): Personale? = personaleDao.getById(id)

    suspend fun getByIds(ids: List<Long>): List<Personale> = personaleDao.getByIds(ids)

    fun search(query: String): Flow<List<Personale>> = personaleDao.search(query)

    suspend fun insert(personale: Personale): Long = personaleDao.insert(personale)

    suspend fun insertAll(personale: List<Personale>) = personaleDao.insertAll(personale)

    suspend fun update(personale: Personale) = personaleDao.update(personale)

    suspend fun delete(personale: Personale) = personaleDao.delete(personale)

    suspend fun deleteById(id: Long) = personaleDao.deleteById(id)

    suspend fun deleteAll() = personaleDao.deleteAll()

    suspend fun getByNomeECognome(nome: String, cognome: String): Personale? = personaleDao.getByNomeECognome(nome, cognome)
}
