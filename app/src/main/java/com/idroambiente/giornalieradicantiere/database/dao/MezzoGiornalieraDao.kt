package com.idroambiente.giornalieradicantiere.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.idroambiente.giornalieradicantiere.database.entities.MezzoGiornaliera
import kotlinx.coroutines.flow.Flow

@Dao
interface MezzoGiornalieraDao {
    @Query("SELECT * FROM mezzo_giornaliera ORDER BY id DESC")
    fun getAll(): Flow<List<MezzoGiornaliera>>

    @Query("SELECT * FROM mezzo_giornaliera WHERE id = :id")
    suspend fun getById(id: Long): MezzoGiornaliera?

    @Query("SELECT * FROM mezzo_giornaliera WHERE giornalieraId = :giornalieraId")
    fun getByGiornalieraId(giornalieraId: Long): Flow<List<MezzoGiornaliera>>

    @Query("SELECT * FROM mezzo_giornaliera WHERE mezzoId = :mezzoId")
    fun getByMezzoId(mezzoId: Long): Flow<List<MezzoGiornaliera>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mezzoGiornaliera: MezzoGiornaliera): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(mezziGiornaliere: List<MezzoGiornaliera>)

    @Update
    suspend fun update(mezzoGiornaliera: MezzoGiornaliera)

    @Delete
    suspend fun delete(mezzoGiornaliera: MezzoGiornaliera)

    @Query("DELETE FROM mezzo_giornaliera WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM mezzo_giornaliera WHERE giornalieraId = :giornalieraId")
    suspend fun deleteByGiornalieraId(giornalieraId: Long)

    @Query("DELETE FROM mezzo_giornaliera")
    suspend fun deleteAll()
}
