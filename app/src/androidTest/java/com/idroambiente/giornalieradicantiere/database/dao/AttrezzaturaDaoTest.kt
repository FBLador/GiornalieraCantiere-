package com.idroambiente.giornalieradicantiere.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.idroambiente.giornalieradicantiere.database.AppDatabase
import com.idroambiente.giornalieradicantiere.database.entities.Attrezzatura
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AttrezzaturaDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var attrezzaturaDao: AttrezzaturaDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        attrezzaturaDao = database.attrezzaturaDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetById() = runTest {
        val attrezzatura = Attrezzatura(nome = "Escavatore")

        val id = attrezzaturaDao.insert(attrezzatura)
        val result = attrezzaturaDao.getById(id)

        assertNotNull(result)
        assertEquals("Escavatore", result?.nome)
    }

    @Test
    fun getAllReturnsOrderedByNome() = runTest {
        attrezzaturaDao.insert(Attrezzatura(nome = "Martello"))
        attrezzaturaDao.insert(Attrezzatura(nome = "Escavatore"))
        attrezzaturaDao.insert(Attrezzatura(nome = "Trivella"))

        attrezzaturaDao.getAll().test {
            val result = awaitItem()
            assertEquals(3, result.size)
            assertEquals("Escavatore", result[0].nome)
            assertEquals("Martello", result[1].nome)
            assertEquals("Trivella", result[2].nome)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun searchFindsAttrezzatura() = runTest {
        attrezzaturaDao.insert(Attrezzatura(nome = "Escavatore Caterpillar"))
        attrezzaturaDao.insert(Attrezzatura(nome = "Martello pneumatico"))

        attrezzaturaDao.search("Escavatore").test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Escavatore Caterpillar", result[0].nome)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun updateModifiesAttrezzatura() = runTest {
        val attrezzatura = Attrezzatura(nome = "Escavatore")
        val id = attrezzaturaDao.insert(attrezzatura)

        val updated = attrezzatura.copy(id = id, nome = "Escavatore Grande")
        attrezzaturaDao.update(updated)

        val result = attrezzaturaDao.getById(id)
        assertEquals("Escavatore Grande", result?.nome)
    }

    @Test
    fun deleteRemovesAttrezzatura() = runTest {
        val id = attrezzaturaDao.insert(Attrezzatura(nome = "Escavatore"))

        attrezzaturaDao.deleteById(id)

        val result = attrezzaturaDao.getById(id)
        assertNull(result)
    }

    @Test
    fun deleteAllRemovesAllAttrezzature() = runTest {
        attrezzaturaDao.insert(Attrezzatura(nome = "Escavatore"))
        attrezzaturaDao.insert(Attrezzatura(nome = "Martello"))

        attrezzaturaDao.deleteAll()

        attrezzaturaDao.getAll().test {
            val result = awaitItem()
            assertEquals(0, result.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
