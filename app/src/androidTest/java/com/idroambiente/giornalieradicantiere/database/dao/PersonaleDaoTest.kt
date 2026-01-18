package com.idroambiente.giornalieradicantiere.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.idroambiente.giornalieradicantiere.database.AppDatabase
import com.idroambiente.giornalieradicantiere.database.entities.Personale
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PersonaleDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var personaleDao: PersonaleDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        personaleDao = database.personaleDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetById() = runTest {
        val personale = Personale(nome = "Mario", cognome = "Rossi")

        val id = personaleDao.insert(personale)
        val result = personaleDao.getById(id)

        assertNotNull(result)
        assertEquals("Mario", result?.nome)
        assertEquals("Rossi", result?.cognome)
    }

    @Test
    fun getAllReturnsOrderedByCognomeNome() = runTest {
        personaleDao.insert(Personale(nome = "Mario", cognome = "Rossi"))
        personaleDao.insert(Personale(nome = "Luigi", cognome = "Bianchi"))
        personaleDao.insert(Personale(nome = "Anna", cognome = "Rossi"))

        personaleDao.getAll().test {
            val result = awaitItem()
            assertEquals(3, result.size)
            assertEquals("Bianchi", result[0].cognome) // First alphabetically
            assertEquals("Anna", result[1].nome) // Anna Rossi before Mario Rossi
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun searchFindsByNome() = runTest {
        personaleDao.insert(Personale(nome = "Mario", cognome = "Rossi"))
        personaleDao.insert(Personale(nome = "Luigi", cognome = "Verdi"))

        personaleDao.search("Mario").test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Mario", result[0].nome)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun searchFindsByCognome() = runTest {
        personaleDao.insert(Personale(nome = "Mario", cognome = "Rossi"))
        personaleDao.insert(Personale(nome = "Luigi", cognome = "Verdi"))

        personaleDao.search("Rossi").test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Rossi", result[0].cognome)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun updateModifiesPersonale() = runTest {
        val personale = Personale(nome = "Mario", cognome = "Rossi")
        val id = personaleDao.insert(personale)

        val updated = personale.copy(id = id, nome = "Giovanni")
        personaleDao.update(updated)

        val result = personaleDao.getById(id)
        assertEquals("Giovanni", result?.nome)
    }

    @Test
    fun deleteRemovesPersonale() = runTest {
        val id = personaleDao.insert(Personale(nome = "Mario", cognome = "Rossi"))

        personaleDao.deleteById(id)

        val result = personaleDao.getById(id)
        assertNull(result)
    }
}
