package com.idroambiente.giornalieradicantiere.database.repository

import com.idroambiente.giornalieradicantiere.database.dao.PersonaleGiornalieraDao
import com.idroambiente.giornalieradicantiere.database.entities.PersonaleGiornaliera
import kotlinx.coroutines.flow.Flow

class PersonaleGiornalieraRepository(private val personaleGiornalieraDao: PersonaleGiornalieraDao) {
    fun getAll(): Flow<List<PersonaleGiornaliera>> = personaleGiornalieraDao.getAll()

    suspend fun getById(id: Long): PersonaleGiornaliera? = personaleGiornalieraDao.getById(id)

    fun getByGiornalieraId(giornalieraId: Long): Flow<List<PersonaleGiornaliera>> = personaleGiornalieraDao.getByGiornalieraId(giornalieraId)

    fun getByPersonaleId(personaleId: Long): Flow<List<PersonaleGiornaliera>> = personaleGiornalieraDao.getByPersonaleId(personaleId)

    suspend fun insert(personaleGiornaliera: PersonaleGiornaliera): Long = personaleGiornalieraDao.insert(personaleGiornaliera)

    suspend fun insertAll(personaleGiornaliere: List<PersonaleGiornaliera>) = personaleGiornalieraDao.insertAll(personaleGiornaliere)

    suspend fun update(personaleGiornaliera: PersonaleGiornaliera) = personaleGiornalieraDao.update(personaleGiornaliera)

    suspend fun delete(personaleGiornaliera: PersonaleGiornaliera) = personaleGiornalieraDao.delete(personaleGiornaliera)

    suspend fun deleteById(id: Long) = personaleGiornalieraDao.deleteById(id)

    suspend fun deleteByGiornalieraId(giornalieraId: Long) = personaleGiornalieraDao.deleteByGiornalieraId(giornalieraId)

    suspend fun deleteAll() = personaleGiornalieraDao.deleteAll()
}
