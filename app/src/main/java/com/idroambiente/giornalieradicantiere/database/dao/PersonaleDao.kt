package com.idroambiente.giornalieradicantiere.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.idroambiente.giornalieradicantiere.database.entities.Personale
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonaleDao {
    @Query("SELECT * FROM personale ORDER BY cognome ASC, nome ASC")
    fun getAll(): Flow<List<Personale>>

    @Query("SELECT * FROM personale WHERE id = :id")
    suspend fun getById(id: Long): Personale?

    @Query("SELECT * FROM personale WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<Personale>

    @Query("SELECT * FROM personale WHERE nome LIKE '%' || :query || '%' OR cognome LIKE '%' || :query || '%' ORDER BY cognome ASC, nome ASC")
    fun search(query: String): Flow<List<Personale>>

    @Query("SELECT * FROM personale WHERE LOWER(TRIM(nome)) = LOWER(TRIM(:nome)) AND LOWER(TRIM(cognome)) = LOWER(TRIM(:cognome))")
    suspend fun getByNomeECognome(nome: String, cognome: String): Personale?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(personale: Personale): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(personale: List<Personale>)

    @Update
    suspend fun update(personale: Personale)

    @Delete
    suspend fun delete(personale: Personale)

    @Query("DELETE FROM personale WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM personale")
    suspend fun deleteAll()
}
