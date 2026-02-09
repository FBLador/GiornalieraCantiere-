package com.idroambiente.giornalieradicantiere.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.idroambiente.giornalieradicantiere.database.AppDatabase
import com.idroambiente.giornalieradicantiere.database.entities.Cantiere
import com.idroambiente.giornalieradicantiere.database.entities.Commessa
import com.idroambiente.giornalieradicantiere.database.entities.Giornaliera
import com.idroambiente.giornalieradicantiere.database.entities.Personale
import com.idroambiente.giornalieradicantiere.database.entities.PersonaleGiornaliera
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PersonaleGiornalieraDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var personaleGiornalieraDao: PersonaleGiornalieraDao
    private var giornalieraId: Long = 0
    private var personaleId: Long = 0

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        personaleGiornalieraDao = database.personaleGiornalieraDao()

        runTest {
            val commessaId = database.commessaDao().insert(
                Commessa(nome = "Commessa", cliente = "Cliente", indirizzo = "Via")
            )
            val cantiereId = database.cantiereDao().insert(
                Cantiere(commessaId = commessaId, nome = "Cantiere", indirizzo = "Via")
            )
            giornalieraId = database.giornalieraDao().insert(
                Giornaliera(cantiereId = cantiereId, data = System.currentTimeMillis())
            )
            personaleId = database.personaleDao().insert(
                Personale(nome = "Mario", cognome = "Rossi")
            )
        }
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetById() = runTest {
        val pg = PersonaleGiornaliera(
            giornalieraId = giornalieraId,
            personaleId = personaleId,
            oreViaggioAndata = 1.5f,
            oreLavoro = 8.0f,
            inizioTurno = 28800000L, // 8:00
            fineTurno = 61200000L,   // 17:00
            pernotto = false
        )

        val id = personaleGiornalieraDao.insert(pg)
        val result = personaleGiornalieraDao.getById(id)

        assertNotNull(result)
        assertEquals(1.5f, result?.oreViaggioAndata)
        assertEquals(8.0f, result?.oreLavoro)
    }

    @Test
    fun getByGiornalieraIdReturnsCorrectRecords() = runTest {
        val personaleId2 = database.personaleDao().insert(
            Personale(nome = "Luigi", cognome = "Verdi")
        )

        personaleGiornalieraDao.insert(
            PersonaleGiornaliera(
                giornalieraId = giornalieraId,
                personaleId = personaleId,
                inizioTurno = 0L,
                fineTurno = 0L
            )
        )
        personaleGiornalieraDao.insert(
            PersonaleGiornaliera(
                giornalieraId = giornalieraId,
                personaleId = personaleId2,
                inizioTurno = 0L,
                fineTurno = 0L
            )
        )

        personaleGiornalieraDao.getByGiornalieraId(giornalieraId).test {
            val result = awaitItem()
            assertEquals(2, result.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun cascadeDeleteWhenGiornalieraDeleted() = runTest {
        val pgId = personaleGiornalieraDao.insert(
            PersonaleGiornaliera(
                giornalieraId = giornalieraId,
                personaleId = personaleId,
                inizioTurno = 0L,
                fineTurno = 0L
            )
        )

        database.giornalieraDao().deleteById(giornalieraId)

        val result = personaleGiornalieraDao.getById(pgId)
        assertNull(result)
    }

    @Test
    fun deleteByGiornalieraIdRemovesAllRelated() = runTest {
        personaleGiornalieraDao.insert(
            PersonaleGiornaliera(
                giornalieraId = giornalieraId,
                personaleId = personaleId,
                inizioTurno = 0L,
                fineTurno = 0L
            )
        )

        personaleGiornalieraDao.deleteByGiornalieraId(giornalieraId)

        personaleGiornalieraDao.getByGiornalieraId(giornalieraId).test {
            val result = awaitItem()
            assertEquals(0, result.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun updateModifiesRecord() = runTest {
        val pg = PersonaleGiornaliera(
            giornalieraId = giornalieraId,
            personaleId = personaleId,
            oreViaggioAndata = 1.0f,
            oreLavoro = 8.0f,
            inizioTurno = 0L,
            fineTurno = 0L
        )
        val id = personaleGiornalieraDao.insert(pg)

        val updated = pg.copy(id = id, oreLavoro = 10.0f)
        personaleGiornalieraDao.update(updated)

        val result = personaleGiornalieraDao.getById(id)
        assertEquals(10.0f, result?.oreLavoro)
    }
}
