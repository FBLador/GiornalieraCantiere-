package com.idroambiente.giornalieradicantiere.database.repository

import com.idroambiente.giornalieradicantiere.database.dao.CommessaDao
import com.idroambiente.giornalieradicantiere.database.entities.Commessa
import com.idroambiente.giornalieradicantiere.database.entities.StatoCommessa
import kotlinx.coroutines.flow.Flow

class CommessaRepository(private val commessaDao: CommessaDao) {
    fun getAll(): Flow<List<Commessa>> = commessaDao.getAll()

    suspend fun getById(id: Long): Commessa? = commessaDao.getById(id)

    fun getByStato(stato: StatoCommessa): Flow<List<Commessa>> = commessaDao.getByStato(stato)

    fun search(query: String): Flow<List<Commessa>> = commessaDao.search(query)

    suspend fun insert(commessa: Commessa): Long = commessaDao.insert(commessa)

    suspend fun insertAll(commesse: List<Commessa>) = commessaDao.insertAll(commesse)

    suspend fun update(commessa: Commessa) = commessaDao.update(commessa)

    suspend fun delete(commessa: Commessa) = commessaDao.delete(commessa)

    suspend fun deleteById(id: Long) = commessaDao.deleteById(id)

    suspend fun deleteAll() = commessaDao.deleteAll()
}
