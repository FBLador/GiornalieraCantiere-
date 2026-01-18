package com.idroambiente.giornalieradicantiere.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.idroambiente.giornalieradicantiere.database.AppDatabase
import com.idroambiente.giornalieradicantiere.database.entities.Mezzo
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MezzoDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var mezzoDao: MezzoDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        mezzoDao = database.mezzoDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetById() = runTest {
        val mezzo = Mezzo(targa = "AB123CD", nome = "Furgone")

        val id = mezzoDao.insert(mezzo)
        val result = mezzoDao.getById(id)

        assertNotNull(result)
        assertEquals("AB123CD", result?.targa)
        assertEquals("Furgone", result?.nome)
    }

    @Test
    fun getAllReturnsOrderedByNome() = runTest {
        mezzoDao.insert(Mezzo(targa = "ZZ999ZZ", nome = "Camion"))
        mezzoDao.insert(Mezzo(targa = "AA111AA", nome = "Auto"))
        mezzoDao.insert(Mezzo(targa = "BB222BB", nome = "Furgone"))

        mezzoDao.getAll().test {
            val result = awaitItem()
            assertEquals(3, result.size)
            assertEquals("Auto", result[0].nome)
            assertEquals("Camion", result[1].nome)
            assertEquals("Furgone", result[2].nome)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getByTargaFindsMezzo() = runTest {
        mezzoDao.insert(Mezzo(targa = "AB123CD", nome = "Furgone"))

        val result = mezzoDao.getByTarga("AB123CD")

        assertNotNull(result)
        assertEquals("Furgone", result?.nome)
    }

    @Test
    fun searchFindsByNome() = runTest {
        mezzoDao.insert(Mezzo(targa = "AA111AA", nome = "Furgone Iveco"))
        mezzoDao.insert(Mezzo(targa = "BB222BB", nome = "Camion Mercedes"))

        mezzoDao.search("Furgone").test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Furgone Iveco", result[0].nome)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun searchFindsByTarga() = runTest {
        mezzoDao.insert(Mezzo(targa = "AB123CD", nome = "Furgone"))
        mezzoDao.insert(Mezzo(targa = "XY789WZ", nome = "Camion"))

        mezzoDao.search("AB123").test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("AB123CD", result[0].targa)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun updateModifiesMezzo() = runTest {
        val mezzo = Mezzo(targa = "AB123CD", nome = "Furgone")
        val id = mezzoDao.insert(mezzo)

        val updated = mezzo.copy(id = id, nome = "Furgone Nuovo")
        mezzoDao.update(updated)

        val result = mezzoDao.getById(id)
        assertEquals("Furgone Nuovo", result?.nome)
    }

    @Test
    fun deleteRemovesMezzo() = runTest {
        val id = mezzoDao.insert(Mezzo(targa = "AB123CD", nome = "Furgone"))

        mezzoDao.deleteById(id)

        val result = mezzoDao.getById(id)
        assertNull(result)
    }
}
