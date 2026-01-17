package com.idroambiente.giornalieradicantiere.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.idroambiente.giornalieradicantiere.database.entities.PersonaleGiornaliera
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonaleGiornalieraDao {
    @Query("SELECT * FROM personale_giornaliera ORDER BY id DESC")
    fun getAll(): Flow<List<PersonaleGiornaliera>>

    @Query("SELECT * FROM personale_giornaliera WHERE id = :id")
    suspend fun getById(id: Long): PersonaleGiornaliera?

    @Query("SELECT * FROM personale_giornaliera WHERE giornalieraId = :giornalieraId")
    fun getByGiornalieraId(giornalieraId: Long): Flow<List<PersonaleGiornaliera>>

    @Query("SELECT * FROM personale_giornaliera WHERE personaleId = :personaleId")
    fun getByPersonaleId(personaleId: Long): Flow<List<PersonaleGiornaliera>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(personaleGiornaliera: PersonaleGiornaliera): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(personaleGiornaliere: List<PersonaleGiornaliera>)

    @Update
    suspend fun update(personaleGiornaliera: PersonaleGiornaliera)

    @Delete
    suspend fun delete(personaleGiornaliera: PersonaleGiornaliera)

    @Query("DELETE FROM personale_giornaliera WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM personale_giornaliera WHERE giornalieraId = :giornalieraId")
    suspend fun deleteByGiornalieraId(giornalieraId: Long)

    @Query("DELETE FROM personale_giornaliera")
    suspend fun deleteAll()
}
