package com.idroambiente.giornalieradicantiere.database.repository

import com.idroambiente.giornalieradicantiere.database.dao.MezzoGiornalieraDao
import com.idroambiente.giornalieradicantiere.database.entities.MezzoGiornaliera
import kotlinx.coroutines.flow.Flow

class MezzoGiornalieraRepository(private val mezzoGiornalieraDao: MezzoGiornalieraDao) {
    fun getAll(): Flow<List<MezzoGiornaliera>> = mezzoGiornalieraDao.getAll()

    suspend fun getById(id: Long): MezzoGiornaliera? = mezzoGiornalieraDao.getById(id)

    fun getByGiornalieraId(giornalieraId: Long): Flow<List<MezzoGiornaliera>> = mezzoGiornalieraDao.getByGiornalieraId(giornalieraId)

    fun getByMezzoId(mezzoId: Long): Flow<List<MezzoGiornaliera>> = mezzoGiornalieraDao.getByMezzoId(mezzoId)

    suspend fun insert(mezzoGiornaliera: MezzoGiornaliera): Long = mezzoGiornalieraDao.insert(mezzoGiornaliera)

    suspend fun insertAll(mezziGiornaliere: List<MezzoGiornaliera>) = mezzoGiornalieraDao.insertAll(mezziGiornaliere)

    suspend fun update(mezzoGiornaliera: MezzoGiornaliera) = mezzoGiornalieraDao.update(mezzoGiornaliera)

    suspend fun delete(mezzoGiornaliera: MezzoGiornaliera) = mezzoGiornalieraDao.delete(mezzoGiornaliera)

    suspend fun deleteById(id: Long) = mezzoGiornalieraDao.deleteById(id)

    suspend fun deleteByGiornalieraId(giornalieraId: Long) = mezzoGiornalieraDao.deleteByGiornalieraId(giornalieraId)

    suspend fun deleteAll() = mezzoGiornalieraDao.deleteAll()
}
