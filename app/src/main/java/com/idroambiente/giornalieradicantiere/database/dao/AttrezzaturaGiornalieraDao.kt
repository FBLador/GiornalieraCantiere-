package com.idroambiente.giornalieradicantiere.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.idroambiente.giornalieradicantiere.database.entities.AttrezzaturaGiornaliera
import kotlinx.coroutines.flow.Flow

@Dao
interface AttrezzaturaGiornalieraDao {
    @Query("SELECT * FROM attrezzatura_giornaliera")
    fun getAll(): Flow<List<AttrezzaturaGiornaliera>>

    @Query("SELECT * FROM attrezzatura_giornaliera WHERE giornalieraId = :giornalieraId AND attrezzaturaId = :attrezzaturaId")
    suspend fun getById(giornalieraId: Long, attrezzaturaId: Long): AttrezzaturaGiornaliera?

    @Query("SELECT * FROM attrezzatura_giornaliera WHERE giornalieraId = :giornalieraId")
    fun getByGiornalieraId(giornalieraId: Long): Flow<List<AttrezzaturaGiornaliera>>

    @Query("SELECT * FROM attrezzatura_giornaliera WHERE attrezzaturaId = :attrezzaturaId")
    fun getByAttrezzaturaId(attrezzaturaId: Long): Flow<List<AttrezzaturaGiornaliera>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attrezzaturaGiornaliera: AttrezzaturaGiornaliera)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(attrezzatureGiornaliere: List<AttrezzaturaGiornaliera>)

    @Delete
    suspend fun delete(attrezzaturaGiornaliera: AttrezzaturaGiornaliera)

    @Query("DELETE FROM attrezzatura_giornaliera WHERE giornalieraId = :giornalieraId AND attrezzaturaId = :attrezzaturaId")
    suspend fun deleteById(giornalieraId: Long, attrezzaturaId: Long)

    @Query("DELETE FROM attrezzatura_giornaliera WHERE giornalieraId = :giornalieraId")
    suspend fun deleteByGiornalieraId(giornalieraId: Long)

    @Query("DELETE FROM attrezzatura_giornaliera")
    suspend fun deleteAll()
}
