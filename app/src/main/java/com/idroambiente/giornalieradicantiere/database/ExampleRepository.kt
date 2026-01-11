package com.idroambiente.giornalieradicantiere.database

import kotlinx.coroutines.flow.Flow

class ExampleRepository(private val exampleDao: ExampleDao) {
    fun getAll(): Flow<List<ExampleEntity>> = exampleDao.getAll()
    
    suspend fun getById(id: Long): ExampleEntity? = exampleDao.getById(id)
    
    suspend fun insert(entity: ExampleEntity): Long = exampleDao.insert(entity)
    
    suspend fun insertAll(entities: List<ExampleEntity>) = exampleDao.insertAll(entities)
    
    suspend fun update(entity: ExampleEntity) = exampleDao.update(entity)
    
    suspend fun delete(entity: ExampleEntity) = exampleDao.delete(entity)
    
    suspend fun deleteById(id: Long) = exampleDao.deleteById(id)
    
    suspend fun deleteAll() = exampleDao.deleteAll()
}
