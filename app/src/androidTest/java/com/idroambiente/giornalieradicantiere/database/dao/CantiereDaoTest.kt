package com.idroambiente.giornalieradicantiere.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.idroambiente.giornalieradicantiere.database.AppDatabase
import com.idroambiente.giornalieradicantiere.database.entities.Cantiere
import com.idroambiente.giornalieradicantiere.database.entities.Commessa
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CantiereDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var cantiereDao: CantiereDao
    private lateinit var commessaDao: CommessaDao
    private var commessaId: Long = 0

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        cantiereDao = database.cantiereDao()
        commessaDao = database.commessaDao()

        // Create a commessa for FK reference
        runTest {
            commessaId = commessaDao.insert(
                Commessa(nome = "Commessa Test", cliente = "Cliente", indirizzo = "Via")
            )
        }
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetById() = runTest {
        val cantiere = Cantiere(
            commessaId = commessaId,
            nome = "Cantiere Test",
            indirizzo = "Via Cantiere 123"
        )

        val id = cantiereDao.insert(cantiere)
        val result = cantiereDao.getById(id)

        assertNotNull(result)
        assertEquals("Cantiere Test", result?.nome)
        assertEquals(commessaId, result?.commessaId)
    }

    @Test
    fun getByCommessaIdReturnsCorrectCantieri() = runTest {
        val commessaId2 = commessaDao.insert(
            Commessa(nome = "Commessa 2", cliente = "Cliente 2", indirizzo = "Via 2")
        )

        cantiereDao.insert(Cantiere(commessaId = commessaId, nome = "Cantiere 1", indirizzo = "Via 1"))
        cantiereDao.insert(Cantiere(commessaId = commessaId, nome = "Cantiere 2", indirizzo = "Via 2"))
        cantiereDao.insert(Cantiere(commessaId = commessaId2, nome = "Cantiere 3", indirizzo = "Via 3"))

        cantiereDao.getByCommessaId(commessaId).test {
            val result = awaitItem()
            assertEquals(2, result.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun cascadeDeleteRemovesCantieriWhenCommessaDeleted() = runTest {
        val cantiereId = cantiereDao.insert(
            Cantiere(commessaId = commessaId, nome = "Cantiere", indirizzo = "Via")
        )

        // Delete the parent commessa
        commessaDao.deleteById(commessaId)

        // Cantiere should be deleted due to CASCADE
        val result = cantiereDao.getById(cantiereId)
        assertNull(result)
    }

    @Test
    fun searchFindsCantieri() = runTest {
        cantiereDao.insert(Cantiere(commessaId = commessaId, nome = "Cantiere Roma", indirizzo = "Via Roma"))
        cantiereDao.insert(Cantiere(commessaId = commessaId, nome = "Cantiere Milano", indirizzo = "Via Milano"))

        cantiereDao.search("Roma").test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Cantiere Roma", result[0].nome)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun updateModifiesCantiere() = runTest {
        val cantiere = Cantiere(commessaId = commessaId, nome = "Original", indirizzo = "Via")
        val id = cantiereDao.insert(cantiere)

        val updated = cantiere.copy(id = id, nome = "Updated")
        cantiereDao.update(updated)

        val result = cantiereDao.getById(id)
        assertEquals("Updated", result?.nome)
    }

    @Test
    fun deleteRemovesCantiere() = runTest {
        val cantiere = Cantiere(commessaId = commessaId, nome = "ToDelete", indirizzo = "Via")
        val id = cantiereDao.insert(cantiere)

        cantiereDao.deleteById(id)

        val result = cantiereDao.getById(id)
        assertNull(result)
    }
}
