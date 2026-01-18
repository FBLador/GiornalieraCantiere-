package com.idroambiente.giornalieradicantiere.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.idroambiente.giornalieradicantiere.database.AppDatabase
import com.idroambiente.giornalieradicantiere.database.entities.Cantiere
import com.idroambiente.giornalieradicantiere.database.entities.Commessa
import com.idroambiente.giornalieradicantiere.database.entities.Giornaliera
import com.idroambiente.giornalieradicantiere.database.entities.StatoGiornaliera
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GiornalieraDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var giornalieraDao: GiornalieraDao
    private lateinit var cantiereDao: CantiereDao
    private lateinit var commessaDao: CommessaDao
    private var cantiereId: Long = 0

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        giornalieraDao = database.giornalieraDao()
        cantiereDao = database.cantiereDao()
        commessaDao = database.commessaDao()

        runTest {
            val commessaId = commessaDao.insert(
                Commessa(nome = "Commessa", cliente = "Cliente", indirizzo = "Via")
            )
            cantiereId = cantiereDao.insert(
                Cantiere(commessaId = commessaId, nome = "Cantiere", indirizzo = "Via")
            )
        }
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetById() = runTest {
        val giornaliera = Giornaliera(
            cantiereId = cantiereId,
            data = System.currentTimeMillis(),
            stato = StatoGiornaliera.BOZZA,
            note = "Note test"
        )

        val id = giornalieraDao.insert(giornaliera)
        val result = giornalieraDao.getById(id)

        assertNotNull(result)
        assertEquals(StatoGiornaliera.BOZZA, result?.stato)
        assertEquals("Note test", result?.note)
    }

    @Test
    fun getByCantiereIdReturnsCorrectGiornaliere() = runTest {
        giornalieraDao.insert(Giornaliera(cantiereId = cantiereId, data = 1000L))
        giornalieraDao.insert(Giornaliera(cantiereId = cantiereId, data = 2000L))

        giornalieraDao.getByCantiereId(cantiereId).test {
            val result = awaitItem()
            assertEquals(2, result.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getByStatoFiltersCorrectly() = runTest {
        giornalieraDao.insert(Giornaliera(cantiereId = cantiereId, data = 1000L, stato = StatoGiornaliera.BOZZA))
        giornalieraDao.insert(Giornaliera(cantiereId = cantiereId, data = 2000L, stato = StatoGiornaliera.COMPLETATA))
        giornalieraDao.insert(Giornaliera(cantiereId = cantiereId, data = 3000L, stato = StatoGiornaliera.INVIATA))

        giornalieraDao.getByStato(StatoGiornaliera.BOZZA).test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals(StatoGiornaliera.BOZZA, result[0].stato)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getByDateRangeReturnsCorrectGiornaliere() = runTest {
        giornalieraDao.insert(Giornaliera(cantiereId = cantiereId, data = 1000L))
        giornalieraDao.insert(Giornaliera(cantiereId = cantiereId, data = 2000L))
        giornalieraDao.insert(Giornaliera(cantiereId = cantiereId, data = 3000L))

        giornalieraDao.getByDateRange(1500L, 2500L).test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals(2000L, result[0].data)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun cascadeDeleteRemovesGiornaliereWhenCantiereDeleted() = runTest {
        val giornalieraId = giornalieraDao.insert(
            Giornaliera(cantiereId = cantiereId, data = 1000L)
        )

        cantiereDao.deleteById(cantiereId)

        val result = giornalieraDao.getById(giornalieraId)
        assertNull(result)
    }

    @Test
    fun updateModifiesGiornaliera() = runTest {
        val giornaliera = Giornaliera(cantiereId = cantiereId, data = 1000L, stato = StatoGiornaliera.BOZZA)
        val id = giornalieraDao.insert(giornaliera)

        val updated = giornaliera.copy(id = id, stato = StatoGiornaliera.COMPLETATA)
        giornalieraDao.update(updated)

        val result = giornalieraDao.getById(id)
        assertEquals(StatoGiornaliera.COMPLETATA, result?.stato)
    }

    @Test
    fun deleteRemovesGiornaliera() = runTest {
        val id = giornalieraDao.insert(Giornaliera(cantiereId = cantiereId, data = 1000L))

        giornalieraDao.deleteById(id)

        val result = giornalieraDao.getById(id)
        assertNull(result)
    }
}
