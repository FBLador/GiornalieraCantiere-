package com.idroambiente.giornalieradicantiere.database.repository

import com.idroambiente.giornalieradicantiere.database.dao.PersonaleDao
import com.idroambiente.giornalieradicantiere.database.entities.Personale
import kotlinx.coroutines.flow.Flow

class PersonaleRepository(private val personaleDao: PersonaleDao) {
    fun getAll(): Flow<List<Personale>> = personaleDao.getAll()

    suspend fun getById(id: Long): Personale? = personaleDao.getById(id)

    fun search(query: String): Flow<List<Personale>> = personaleDao.search(query)

    suspend fun insert(personale: Personale): Long = personaleDao.insert(personale)

    suspend fun insertAll(personale: List<Personale>) = personaleDao.insertAll(personale)

    suspend fun update(personale: Personale) = personaleDao.update(personale)

    suspend fun delete(personale: Personale) = personaleDao.delete(personale)

    suspend fun deleteById(id: Long) = personaleDao.deleteById(id)

    suspend fun deleteAll() = personaleDao.deleteAll()
}
