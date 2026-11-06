package com.idroambiente.giornalieradicantiere.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.idroambiente.giornalieradicantiere.database.entities.Attrezzatura
import kotlinx.coroutines.flow.Flow

@Dao
interface AttrezzaturaDao {
    /** Solo le attrezzature attive (quelle eliminate restano nelle giornaliere vecchie). */
    @Query("SELECT * FROM attrezzatura WHERE attivo = 1 ORDER BY nome ASC")
    fun getAll(): Flow<List<Attrezzatura>>

    @Query("SELECT * FROM attrezzatura ORDER BY nome ASC")
    fun getAllIncludingInactive(): Flow<List<Attrezzatura>>

    @Query("UPDATE attrezzatura SET attivo = 0 WHERE id = :id")
    suspend fun disattiva(id: Long)

    @Query("UPDATE attrezzatura SET attivo = 0")
    suspend fun disattivaTutti()

    @Query("SELECT * FROM attrezzatura WHERE id = :id")
    suspend fun getById(id: Long): Attrezzatura?

    @Query("SELECT * FROM attrezzatura WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<Attrezzatura>

    @Query("SELECT * FROM attrezzatura WHERE attivo = 1 AND nome LIKE '%' || :query || '%' ORDER BY nome ASC")
    fun search(query: String): Flow<List<Attrezzatura>>

    @Query("SELECT * FROM attrezzatura WHERE LOWER(TRIM(nome)) = LOWER(TRIM(:nome))")
    suspend fun getByNome(nome: String): Attrezzatura?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(attrezzatura: Attrezzatura): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(attrezzature: List<Attrezzatura>)

    @Update
    suspend fun update(attrezzatura: Attrezzatura)

    @Delete
    suspend fun delete(attrezzatura: Attrezzatura)

    @Query("DELETE FROM attrezzatura WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM attrezzatura")
    suspend fun deleteAll()
}
