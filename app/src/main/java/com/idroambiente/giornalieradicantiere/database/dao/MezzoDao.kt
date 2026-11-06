package com.idroambiente.giornalieradicantiere.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.idroambiente.giornalieradicantiere.database.entities.Mezzo
import kotlinx.coroutines.flow.Flow

@Dao
interface MezzoDao {
    /** Solo i mezzi attivi (quelli eliminati restano nelle giornaliere vecchie). */
    @Query("SELECT * FROM mezzo WHERE attivo = 1 ORDER BY nome ASC")
    fun getAll(): Flow<List<Mezzo>>

    @Query("SELECT * FROM mezzo ORDER BY nome ASC")
    fun getAllIncludingInactive(): Flow<List<Mezzo>>

    @Query("UPDATE mezzo SET attivo = 0 WHERE id = :id")
    suspend fun disattiva(id: Long)

    @Query("UPDATE mezzo SET attivo = 0")
    suspend fun disattivaTutti()

    @Query("SELECT * FROM mezzo WHERE id = :id")
    suspend fun getById(id: Long): Mezzo?

    @Query("SELECT * FROM mezzo WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<Mezzo>

    @Query("SELECT * FROM mezzo WHERE attivo = 1 AND (nome LIKE '%' || :query || '%' OR targa LIKE '%' || :query || '%') ORDER BY nome ASC")
    fun search(query: String): Flow<List<Mezzo>>

    @Query("SELECT * FROM mezzo WHERE targa = :targa")
    suspend fun getByTarga(targa: String): Mezzo?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(mezzo: Mezzo): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(mezzi: List<Mezzo>)

    @Update
    suspend fun update(mezzo: Mezzo)

    @Delete
    suspend fun delete(mezzo: Mezzo)

    @Query("DELETE FROM mezzo WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM mezzo")
    suspend fun deleteAll()
}
