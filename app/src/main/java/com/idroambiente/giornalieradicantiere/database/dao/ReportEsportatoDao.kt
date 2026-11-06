package com.idroambiente.giornalieradicantiere.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.idroambiente.giornalieradicantiere.database.entities.ReportEsportato
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportEsportatoDao {
    @Insert
    suspend fun insert(report: ReportEsportato): Long

    @Update
    suspend fun update(report: ReportEsportato)

    @Query("SELECT * FROM report_esportato WHERE commessaId = :commessaId AND archivio = :archivio AND pdfFileName = :pdfFileName LIMIT 1")
    suspend fun find(commessaId: Long, archivio: String, pdfFileName: String): ReportEsportato?

    @Query("SELECT * FROM report_esportato WHERE commessaId = :commessaId AND archivio = :archivio ORDER BY exportedAt DESC")
    suspend fun getByCommessa(commessaId: Long, archivio: String): List<ReportEsportato>

    @Query("SELECT * FROM report_esportato WHERE commessaId = :commessaId AND daAggiornare = 1")
    suspend fun getDaAggiornare(commessaId: Long): List<ReportEsportato>

    @Query("SELECT COUNT(*) FROM report_esportato WHERE commessaId = :commessaId AND daAggiornare = 1")
    fun countDaAggiornare(commessaId: Long): Flow<Int>

    @Query("UPDATE report_esportato SET daAggiornare = :valore WHERE id = :id")
    suspend fun setDaAggiornare(id: Long, valore: Boolean)
}
