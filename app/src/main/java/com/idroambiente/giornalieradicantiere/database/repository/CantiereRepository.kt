package com.idroambiente.giornalieradicantiere.database.repository

import com.idroambiente.giornalieradicantiere.database.dao.CantiereDao
import com.idroambiente.giornalieradicantiere.database.entities.Cantiere
import kotlinx.coroutines.flow.Flow

class CantiereRepository(private val cantiereDao: CantiereDao) {
    fun getAll(): Flow<List<Cantiere>> = cantiereDao.getAll()

    suspend fun getById(id: Long): Cantiere? = cantiereDao.getById(id)

    fun getByCommessaId(commessaId: Long): Flow<List<Cantiere>> = cantiereDao.getByCommessaId(commessaId)

    suspend fun getByCommessaIdSync(commessaId: Long): List<Cantiere> = cantiereDao.getByCommessaIdSync(commessaId)

    fun search(query: String): Flow<List<Cantiere>> = cantiereDao.search(query)

    suspend fun insert(cantiere: Cantiere): Long = cantiereDao.insert(cantiere)

    suspend fun insertAll(cantieri: List<Cantiere>) = cantiereDao.insertAll(cantieri)

    suspend fun update(cantiere: Cantiere) = cantiereDao.update(cantiere)

    suspend fun delete(cantiere: Cantiere) = cantiereDao.delete(cantiere)

    suspend fun deleteById(id: Long) = cantiereDao.deleteById(id)

    suspend fun deleteAll() = cantiereDao.deleteAll()
}
