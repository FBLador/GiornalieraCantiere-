package com.idroambiente.giornalieradicantiere.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.idroambiente.giornalieradicantiere.database.AppDatabase
import com.idroambiente.giornalieradicantiere.database.entities.Attrezzatura
import com.idroambiente.giornalieradicantiere.database.entities.AttrezzaturaGiornaliera
import com.idroambiente.giornalieradicantiere.database.entities.Cantiere
import com.idroambiente.giornalieradicantiere.database.entities.Commessa
import com.idroambiente.giornalieradicantiere.database.entities.Giornaliera
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AttrezzaturaGiornalieraDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var attrezzaturaGiornalieraDao: AttrezzaturaGiornalieraDao
    private var giornalieraId: Long = 0
    private var attrezzaturaId: Long = 0

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        attrezzaturaGiornalieraDao = database.attrezzaturaGiornalieraDao()

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
            attrezzaturaId = database.attrezzaturaDao().insert(
                Attrezzatura(nome = "Escavatore")
            )
        }
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetById() = runTest {
        val ag = AttrezzaturaGiornaliera(
            giornalieraId = giornalieraId,
            attrezzaturaId = attrezzaturaId
        )

        attrezzaturaGiornalieraDao.insert(ag)
        val result = attrezzaturaGiornalieraDao.getById(giornalieraId, attrezzaturaId)

        assertNotNull(result)
        assertEquals(giornalieraId, result?.giornalieraId)
        assertEquals(attrezzaturaId, result?.attrezzaturaId)
    }

    @Test
    fun getByGiornalieraIdReturnsCorrectRecords() = runTest {
        val attrezzaturaId2 = database.attrezzaturaDao().insert(
            Attrezzatura(nome = "Martello")
        )

        attrezzaturaGiornalieraDao.insert(
            AttrezzaturaGiornaliera(giornalieraId = giornalieraId, attrezzaturaId = attrezzaturaId)
        )
        attrezzaturaGiornalieraDao.insert(
            AttrezzaturaGiornaliera(giornalieraId = giornalieraId, attrezzaturaId = attrezzaturaId2)
        )

        attrezzaturaGiornalieraDao.getByGiornalieraId(giornalieraId).test {
            val result = awaitItem()
            assertEquals(2, result.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun cascadeDeleteWhenGiornalieraDeleted() = runTest {
        attrezzaturaGiornalieraDao.insert(
            AttrezzaturaGiornaliera(giornalieraId = giornalieraId, attrezzaturaId = attrezzaturaId)
        )

        database.giornalieraDao().deleteById(giornalieraId)

        val result = attrezzaturaGiornalieraDao.getById(giornalieraId, attrezzaturaId)
        assertNull(result)
    }

    @Test
    fun deleteByIdRemovesRecord() = runTest {
        attrezzaturaGiornalieraDao.insert(
            AttrezzaturaGiornaliera(giornalieraId = giornalieraId, attrezzaturaId = attrezzaturaId)
        )

        attrezzaturaGiornalieraDao.deleteById(giornalieraId, attrezzaturaId)

        val result = attrezzaturaGiornalieraDao.getById(giornalieraId, attrezzaturaId)
        assertNull(result)
    }

    @Test
    fun deleteByGiornalieraIdRemovesAllRelated() = runTest {
        val attrezzaturaId2 = database.attrezzaturaDao().insert(
            Attrezzatura(nome = "Martello")
        )

        attrezzaturaGiornalieraDao.insert(
            AttrezzaturaGiornaliera(giornalieraId = giornalieraId, attrezzaturaId = attrezzaturaId)
        )
        attrezzaturaGiornalieraDao.insert(
            AttrezzaturaGiornaliera(giornalieraId = giornalieraId, attrezzaturaId = attrezzaturaId2)
        )

        attrezzaturaGiornalieraDao.deleteByGiornalieraId(giornalieraId)

        attrezzaturaGiornalieraDao.getByGiornalieraId(giornalieraId).test {
            val result = awaitItem()
            assertEquals(0, result.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun insertAllAddsMultipleRecords() = runTest {
        val attrezzaturaId2 = database.attrezzaturaDao().insert(
            Attrezzatura(nome = "Martello")
        )

        val records = listOf(
            AttrezzaturaGiornaliera(giornalieraId = giornalieraId, attrezzaturaId = attrezzaturaId),
            AttrezzaturaGiornaliera(giornalieraId = giornalieraId, attrezzaturaId = attrezzaturaId2)
        )

        attrezzaturaGiornalieraDao.insertAll(records)

        attrezzaturaGiornalieraDao.getByGiornalieraId(giornalieraId).test {
            val result = awaitItem()
            assertEquals(2, result.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
