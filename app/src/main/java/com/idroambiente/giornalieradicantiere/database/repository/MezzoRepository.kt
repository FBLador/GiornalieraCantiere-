package com.idroambiente.giornalieradicantiere.database.repository

import com.idroambiente.giornalieradicantiere.database.dao.MezzoDao
import com.idroambiente.giornalieradicantiere.database.entities.Mezzo
import kotlinx.coroutines.flow.Flow

class MezzoRepository(private val mezzoDao: MezzoDao) {
    fun getAll(): Flow<List<Mezzo>> = mezzoDao.getAll()

    suspend fun getById(id: Long): Mezzo? = mezzoDao.getById(id)

    fun search(query: String): Flow<List<Mezzo>> = mezzoDao.search(query)

    suspend fun getByTarga(targa: String): Mezzo? = mezzoDao.getByTarga(targa)

    suspend fun insert(mezzo: Mezzo): Long = mezzoDao.insert(mezzo)

    suspend fun insertAll(mezzi: List<Mezzo>) = mezzoDao.insertAll(mezzi)

    suspend fun update(mezzo: Mezzo) = mezzoDao.update(mezzo)

    suspend fun delete(mezzo: Mezzo) = mezzoDao.delete(mezzo)

    suspend fun deleteById(id: Long) = mezzoDao.deleteById(id)

    suspend fun deleteAll() = mezzoDao.deleteAll()
}
