package com.idroambiente.giornalieradicantiere.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.idroambiente.giornalieradicantiere.database.entities.Cantiere
import kotlinx.coroutines.flow.Flow

@Dao
interface CantiereDao {
    @Query("SELECT * FROM cantiere ORDER BY id DESC")
    fun getAll(): Flow<List<Cantiere>>

    @Query("SELECT * FROM cantiere WHERE id = :id")
    suspend fun getById(id: Long): Cantiere?

    @Query("SELECT * FROM cantiere WHERE commessaId = :commessaId ORDER BY id DESC")
    fun getByCommessaId(commessaId: Long): Flow<List<Cantiere>>

    @Query("SELECT * FROM cantiere WHERE commessaId = :commessaId ORDER BY id DESC")
    suspend fun getByCommessaIdSync(commessaId: Long): List<Cantiere>

    @Query("SELECT * FROM cantiere WHERE nome LIKE '%' || :query || '%' ORDER BY id DESC")
    fun search(query: String): Flow<List<Cantiere>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(cantiere: Cantiere): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(cantieri: List<Cantiere>)

    @Update
    suspend fun update(cantiere: Cantiere)

    @Delete
    suspend fun delete(cantiere: Cantiere)

    @Query("DELETE FROM cantiere WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM cantiere")
    suspend fun deleteAll()
}
