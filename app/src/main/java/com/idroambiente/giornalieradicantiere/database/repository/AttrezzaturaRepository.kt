package com.idroambiente.giornalieradicantiere.database.repository

import com.idroambiente.giornalieradicantiere.database.dao.AttrezzaturaDao
import com.idroambiente.giornalieradicantiere.database.entities.Attrezzatura
import kotlinx.coroutines.flow.Flow

class AttrezzaturaRepository(private val attrezzaturaDao: AttrezzaturaDao) {
    fun getAll(): Flow<List<Attrezzatura>> = attrezzaturaDao.getAll()

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
