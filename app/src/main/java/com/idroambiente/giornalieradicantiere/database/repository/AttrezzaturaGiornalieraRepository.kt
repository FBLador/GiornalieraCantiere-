package com.idroambiente.giornalieradicantiere.database.repository

import com.idroambiente.giornalieradicantiere.database.dao.AttrezzaturaGiornalieraDao
import com.idroambiente.giornalieradicantiere.database.entities.AttrezzaturaGiornaliera
import kotlinx.coroutines.flow.Flow

class AttrezzaturaGiornalieraRepository(private val attrezzaturaGiornalieraDao: AttrezzaturaGiornalieraDao) {
    fun getAll(): Flow<List<AttrezzaturaGiornaliera>> = attrezzaturaGiornalieraDao.getAll()

    suspend fun getById(giornalieraId: Long, attrezzaturaId: Long): AttrezzaturaGiornaliera? =
        attrezzaturaGiornalieraDao.getById(giornalieraId, attrezzaturaId)

    fun getByGiornalieraId(giornalieraId: Long): Flow<List<AttrezzaturaGiornaliera>> =
        attrezzaturaGiornalieraDao.getByGiornalieraId(giornalieraId)

    fun getByAttrezzaturaId(attrezzaturaId: Long): Flow<List<AttrezzaturaGiornaliera>> =
        attrezzaturaGiornalieraDao.getByAttrezzaturaId(attrezzaturaId)

    suspend fun insert(attrezzaturaGiornaliera: AttrezzaturaGiornaliera) =
        attrezzaturaGiornalieraDao.insert(attrezzaturaGiornaliera)

    suspend fun insertAll(attrezzatureGiornaliere: List<AttrezzaturaGiornaliera>) =
        attrezzaturaGiornalieraDao.insertAll(attrezzatureGiornaliere)

    suspend fun delete(attrezzaturaGiornaliera: AttrezzaturaGiornaliera) =
        attrezzaturaGiornalieraDao.delete(attrezzaturaGiornaliera)

    suspend fun deleteById(giornalieraId: Long, attrezzaturaId: Long) =
        attrezzaturaGiornalieraDao.deleteById(giornalieraId, attrezzaturaId)

    suspend fun deleteByGiornalieraId(giornalieraId: Long) =
        attrezzaturaGiornalieraDao.deleteByGiornalieraId(giornalieraId)

    suspend fun deleteAll() = attrezzaturaGiornalieraDao.deleteAll()
}
