package com.idroambiente.giornalieradicantiere.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.idroambiente.giornalieradicantiere.database.entities.GiornalieraModifica
import kotlinx.coroutines.flow.Flow

@Dao
interface GiornalieraModificaDao {
    @Insert
    suspend fun insert(modifica: GiornalieraModifica): Long

    @Query("SELECT * FROM giornaliera_modifica WHERE giornalieraId = :giornalieraId ORDER BY revisione ASC")
    fun getByGiornalieraId(giornalieraId: Long): Flow<List<GiornalieraModifica>>

    @Query("SELECT * FROM giornaliera_modifica WHERE giornalieraId = :giornalieraId ORDER BY revisione ASC")
    suspend fun getByGiornalieraIdSync(giornalieraId: Long): List<GiornalieraModifica>
}
