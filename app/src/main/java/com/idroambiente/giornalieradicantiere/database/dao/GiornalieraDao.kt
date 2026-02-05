package com.idroambiente.giornalieradicantiere.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.idroambiente.giornalieradicantiere.database.entities.Giornaliera
import com.idroambiente.giornalieradicantiere.database.entities.StatoGiornaliera
import kotlinx.coroutines.flow.Flow

@Dao
interface GiornalieraDao {
    @Query("SELECT * FROM giornaliera ORDER BY CASE WHEN stato = 'BOZZA' THEN 0 ELSE 1 END, data DESC")
    fun getAll(): Flow<List<Giornaliera>>

    @Query("SELECT * FROM giornaliera WHERE id = :id")
    suspend fun getById(id: Long): Giornaliera?

    @Query("SELECT * FROM giornaliera WHERE cantiereId = :cantiereId ORDER BY CASE WHEN stato = 'BOZZA' THEN 0 ELSE 1 END, data DESC")
    fun getByCantiereId(cantiereId: Long): Flow<List<Giornaliera>>

    @Query("SELECT * FROM giornaliera WHERE data = :data ORDER BY id DESC")
    fun getByData(data: Long): Flow<List<Giornaliera>>

    @Query("SELECT * FROM giornaliera WHERE data BETWEEN :startDate AND :endDate ORDER BY data DESC")
    fun getByDateRange(startDate: Long, endDate: Long): Flow<List<Giornaliera>>

    @Query("SELECT * FROM giornaliera WHERE stato = :stato ORDER BY data DESC")
    fun getByStato(stato: StatoGiornaliera): Flow<List<Giornaliera>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(giornaliera: Giornaliera): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(giornaliere: List<Giornaliera>)

    @Update
    suspend fun update(giornaliera: Giornaliera)

    @Delete
    suspend fun delete(giornaliera: Giornaliera)

    @Query("DELETE FROM giornaliera WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM giornaliera")
    suspend fun deleteAll()
}
