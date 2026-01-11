package com.idroambiente.giornalieradicantiere.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExampleDao {
    @Query("SELECT * FROM example_table ORDER BY createdAt DESC")
    fun getAll(): Flow<List<ExampleEntity>>
    
    @Query("SELECT * FROM example_table WHERE id = :id")
    suspend fun getById(id: Long): ExampleEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ExampleEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ExampleEntity>)
    
    @Update
    suspend fun update(entity: ExampleEntity)
    
    @Delete
    suspend fun delete(entity: ExampleEntity)
    
    @Query("DELETE FROM example_table WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("DELETE FROM example_table")
    suspend fun deleteAll()
}
