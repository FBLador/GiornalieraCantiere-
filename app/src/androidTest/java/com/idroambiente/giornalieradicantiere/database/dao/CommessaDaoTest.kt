package com.idroambiente.giornalieradicantiere.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.idroambiente.giornalieradicantiere.database.AppDatabase
import com.idroambiente.giornalieradicantiere.database.entities.Commessa
import com.idroambiente.giornalieradicantiere.database.entities.StatoCommessa
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CommessaDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var commessaDao: CommessaDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        commessaDao = database.commessaDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetById() = runTest {
        val commessa = Commessa(
            nome = "Commessa Test",
            cliente = "Cliente Test",
            indirizzo = "Via Test 123",
            stato = StatoCommessa.APERTA
        )

        val id = commessaDao.insert(commessa)
        val result = commessaDao.getById(id)

        assertNotNull(result)
        assertEquals("Commessa Test", result?.nome)
        assertEquals("Cliente Test", result?.cliente)
        assertEquals(StatoCommessa.APERTA, result?.stato)
    }

    @Test
    fun getAllReturnsAllCommesse() = runTest {
        val commessa1 = Commessa(nome = "Commessa 1", cliente = "Cliente 1", indirizzo = "Via 1")
        val commessa2 = Commessa(nome = "Commessa 2", cliente = "Cliente 2", indirizzo = "Via 2")

        commessaDao.insert(commessa1)
        commessaDao.insert(commessa2)

        commessaDao.getAll().test {
            val result = awaitItem()
            assertEquals(2, result.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getByStatoFiltersCorrectly() = runTest {
        val commessaAperta = Commessa(nome = "Aperta", cliente = "C1", indirizzo = "V1", stato = StatoCommessa.APERTA)
        val commessaChiusa = Commessa(nome = "Chiusa", cliente = "C2", indirizzo = "V2", stato = StatoCommessa.CHIUSA)

        commessaDao.insert(commessaAperta)
        commessaDao.insert(commessaChiusa)

        commessaDao.getByStato(StatoCommessa.APERTA).test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Aperta", result[0].nome)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun searchFindsMatchingCommesse() = runTest {
        val commessa1 = Commessa(nome = "Progetto Alpha", cliente = "Cliente Uno", indirizzo = "Via 1")
        val commessa2 = Commessa(nome = "Progetto Beta", cliente = "Cliente Due", indirizzo = "Via 2")

        commessaDao.insert(commessa1)
        commessaDao.insert(commessa2)

        commessaDao.search("Alpha").test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Progetto Alpha", result[0].nome)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun updateModifiesCommessa() = runTest {
        val commessa = Commessa(nome = "Original", cliente = "Cliente", indirizzo = "Via")
        val id = commessaDao.insert(commessa)

        val updated = commessa.copy(id = id, nome = "Updated")
        commessaDao.update(updated)

        val result = commessaDao.getById(id)
        assertEquals("Updated", result?.nome)
    }

    @Test
    fun deleteRemovesCommessa() = runTest {
        val commessa = Commessa(nome = "ToDelete", cliente = "Cliente", indirizzo = "Via")
        val id = commessaDao.insert(commessa)

        commessaDao.deleteById(id)

        val result = commessaDao.getById(id)
        assertNull(result)
    }

    @Test
    fun deleteAllRemovesAllCommesse() = runTest {
        commessaDao.insert(Commessa(nome = "C1", cliente = "Cl1", indirizzo = "V1"))
        commessaDao.insert(Commessa(nome = "C2", cliente = "Cl2", indirizzo = "V2"))

        commessaDao.deleteAll()

        commessaDao.getAll().test {
            val result = awaitItem()
            assertEquals(0, result.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
