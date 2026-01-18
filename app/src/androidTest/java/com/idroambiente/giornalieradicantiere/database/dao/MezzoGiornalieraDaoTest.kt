package com.idroambiente.giornalieradicantiere.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.idroambiente.giornalieradicantiere.database.AppDatabase
import com.idroambiente.giornalieradicantiere.database.entities.Cantiere
import com.idroambiente.giornalieradicantiere.database.entities.Commessa
import com.idroambiente.giornalieradicantiere.database.entities.Giornaliera
import com.idroambiente.giornalieradicantiere.database.entities.Mezzo
import com.idroambiente.giornalieradicantiere.database.entities.MezzoGiornaliera
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MezzoGiornalieraDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var mezzoGiornalieraDao: MezzoGiornalieraDao
    private var giornalieraId: Long = 0
    private var mezzoId: Long = 0

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        mezzoGiornalieraDao = database.mezzoGiornalieraDao()

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
            mezzoId = database.mezzoDao().insert(
                Mezzo(targa = "AB123CD", nome = "Furgone")
            )
        }
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetById() = runTest {
        val mg = MezzoGiornaliera(
            giornalieraId = giornalieraId,
            mezzoId = mezzoId,
            kmAndata = 50.5f,
            kmRitorno = 50.5f
        )

        val id = mezzoGiornalieraDao.insert(mg)
        val result = mezzoGiornalieraDao.getById(id)

        assertNotNull(result)
        assertEquals(50.5f, result?.kmAndata)
        assertEquals(50.5f, result?.kmRitorno)
    }

    @Test
    fun getByGiornalieraIdReturnsCorrectRecords() = runTest {
        val mezzoId2 = database.mezzoDao().insert(
            Mezzo(targa = "XY789WZ", nome = "Camion")
        )

        mezzoGiornalieraDao.insert(
            MezzoGiornaliera(giornalieraId = giornalieraId, mezzoId = mezzoId, kmAndata = 10f, kmRitorno = 10f)
        )
        mezzoGiornalieraDao.insert(
            MezzoGiornaliera(giornalieraId = giornalieraId, mezzoId = mezzoId2, kmAndata = 20f, kmRitorno = 20f)
        )

        mezzoGiornalieraDao.getByGiornalieraId(giornalieraId).test {
            val result = awaitItem()
            assertEquals(2, result.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun cascadeDeleteWhenGiornalieraDeleted() = runTest {
        val mgId = mezzoGiornalieraDao.insert(
            MezzoGiornaliera(giornalieraId = giornalieraId, mezzoId = mezzoId, kmAndata = 10f, kmRitorno = 10f)
        )

        database.giornalieraDao().deleteById(giornalieraId)

        val result = mezzoGiornalieraDao.getById(mgId)
        assertNull(result)
    }

    @Test
    fun deleteByGiornalieraIdRemovesAllRelated() = runTest {
        mezzoGiornalieraDao.insert(
            MezzoGiornaliera(giornalieraId = giornalieraId, mezzoId = mezzoId, kmAndata = 10f, kmRitorno = 10f)
        )

        mezzoGiornalieraDao.deleteByGiornalieraId(giornalieraId)

        mezzoGiornalieraDao.getByGiornalieraId(giornalieraId).test {
            val result = awaitItem()
            assertEquals(0, result.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun updateModifiesRecord() = runTest {
        val mg = MezzoGiornaliera(
            giornalieraId = giornalieraId,
            mezzoId = mezzoId,
            kmAndata = 50f,
            kmRitorno = 50f
        )
        val id = mezzoGiornalieraDao.insert(mg)

        val updated = mg.copy(id = id, kmAndata = 75f)
        mezzoGiornalieraDao.update(updated)

        val result = mezzoGiornalieraDao.getById(id)
        assertEquals(75f, result?.kmAndata)
    }
}
