package com.idroambiente.giornalieradicantiere.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.idroambiente.giornalieradicantiere.database.entities.Commessa
import com.idroambiente.giornalieradicantiere.database.entities.StatoCommessa
import kotlinx.coroutines.flow.Flow

@Dao
interface CommessaDao {
    @Query("SELECT * FROM commessa ORDER BY id DESC")
    fun getAll(): Flow<List<Commessa>>

    @Query("SELECT * FROM commessa WHERE id = :id")
    suspend fun getById(id: Long): Commessa?

    @Query("SELECT * FROM commessa WHERE stato = :stato ORDER BY id DESC")
    fun getByStato(stato: StatoCommessa): Flow<List<Commessa>>

    @Query("SELECT * FROM commessa WHERE nome LIKE '%' || :query || '%' OR cliente LIKE '%' || :query || '%' ORDER BY id DESC")
    fun search(query: String): Flow<List<Commessa>>

    @Query("SELECT * FROM commessa WHERE nome = :nome")
    suspend fun getByNome(nome: String): Commessa?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(commessa: Commessa): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(commesse: List<Commessa>)

    @Update
    suspend fun update(commessa: Commessa)

    @Delete
    suspend fun delete(commessa: Commessa)

    @Query("DELETE FROM commessa WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM commessa")
    suspend fun deleteAll()
}
