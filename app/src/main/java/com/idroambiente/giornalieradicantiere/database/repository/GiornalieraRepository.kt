package com.idroambiente.giornalieradicantiere.database.repository

import com.idroambiente.giornalieradicantiere.database.dao.GiornalieraDao
import com.idroambiente.giornalieradicantiere.database.entities.Giornaliera
import com.idroambiente.giornalieradicantiere.database.entities.StatoGiornaliera
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class GiornalieraRepository(private val giornalieraDao: GiornalieraDao) {
    fun getAll(): Flow<List<Giornaliera>> = giornalieraDao.getAll()

    suspend fun getById(id: Long): Giornaliera? = giornalieraDao.getById(id)

    fun getByCantiereId(cantiereId: Long): Flow<List<Giornaliera>> = giornalieraDao.getByCantiereId(cantiereId)

    fun getByData(data: Long): Flow<List<Giornaliera>> = giornalieraDao.getByData(data)

    fun getByDateRange(startDate: Long, endDate: Long): Flow<List<Giornaliera>> = giornalieraDao.getByDateRange(startDate, endDate)

    fun getByStato(stato: StatoGiornaliera): Flow<List<Giornaliera>> = giornalieraDao.getByStato(stato)

    suspend fun insert(giornaliera: Giornaliera): Long = giornalieraDao.insert(giornaliera)

    suspend fun insertAll(giornaliere: List<Giornaliera>) = giornalieraDao.insertAll(giornaliere)

    suspend fun update(giornaliera: Giornaliera) = giornalieraDao.update(giornaliera)

    suspend fun delete(giornaliera: Giornaliera) = giornalieraDao.delete(giornaliera)

    suspend fun deleteById(id: Long) = giornalieraDao.deleteById(id)

    suspend fun deleteAll() = giornalieraDao.deleteAll()

    suspend fun existsForCantiereAndDay(cantiereId: Long, date: Long): Boolean {
        val normalizedDate = normalizeToStartOfDay(date)
        val endOfDay = normalizedDate + 86_399_999L
        val existing = giornalieraDao.getByCantiereIdAndDateRange(cantiereId, normalizedDate, endOfDay)
        return existing.isNotEmpty()
    }

    private fun normalizeToStartOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
}
