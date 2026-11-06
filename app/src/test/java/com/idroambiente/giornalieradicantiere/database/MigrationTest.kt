package com.idroambiente.giornalieradicantiere.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Il test più importante dell'aggiornamento: un telefono con il database v6
 * pieno di dati passa alla v7 senza perdere niente.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class MigrationTest {

    private val dbName = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    private fun creaDatabaseV6() {
        helper.createDatabase(dbName, 6).apply {
            execSQL("INSERT INTO commessa (id, nome, cliente, indirizzo, stato) VALUES (1, 1234, 'Comune di Bergamo', 'Piazza Matteotti 27', 'APERTA')")
            execSQL("INSERT INTO cantiere (id, commessaId, nome, indirizzo) VALUES (10, 1, '0001234', 'Piazza Matteotti 27')")
            execSQL("INSERT INTO personale (id, nome, cognome) VALUES (100, 'Stefano', 'Ricci')")
            execSQL("INSERT INTO personale (id, nome, cognome) VALUES (101, 'Paolo', 'Esposito')")
            execSQL("INSERT INTO mezzo (id, targa, nome, codiceArticoloKm, codiceArticoloGiorni) VALUES (200, 'FK302LM', 'Furgone TV', 'KM01', 'GG01')")
            execSQL("INSERT INTO attrezzatura (id, nome, codiceArticolo) VALUES (300, 'Telecamera', 'ATT01')")
            execSQL(
                "INSERT INTO giornaliera (id, cantiereId, data, stato, note, dpiIdonei, presaVisionePOS, createdAt, updatedAt) " +
                    "VALUES (1000, 10, 1762210800000, 'INVIATA', 'Ispezione via Corridoni', 0, 1, 1, 2)"
            )
            execSQL(
                "INSERT INTO personale_giornaliera (id, giornalieraId, personaleId, presente, oreViaggioAndata, oreViaggioRitorno, oreLavoro, inizioTurno, fineTurno, pernotto) " +
                    "VALUES (1, 1000, 100, 1, 0.5, 0.5, 8.0, 27000000, 59400000, 0)"
            )
            execSQL(
                "INSERT INTO personale_giornaliera (id, giornalieraId, personaleId, presente, oreViaggioAndata, oreViaggioRitorno, oreLavoro, inizioTurno, fineTurno, pernotto) " +
                    "VALUES (2, 1000, 101, 1, 0.5, 1.0, 8.0, 27000000, 59400000, 1)"
            )
            execSQL("INSERT INTO mezzo_giornaliera (id, giornalieraId, mezzoId, kmAndata, kmRitorno) VALUES (1, 1000, 200, 22, 22)")
            execSQL("INSERT INTO attrezzatura_giornaliera (giornalieraId, attrezzaturaId) VALUES (1000, 300)")
            close()
        }
    }

    @Test
    fun migrazione6a7_conservaTuttiIDati() {
        creaDatabaseV6()

        // Esegue la migrazione e verifica che lo schema sia identico a quello atteso da Room
        helper.runMigrationsAndValidate(dbName, 7, true, Migrations.MIGRATION_6_7).close()

        // Apre il database come fa l'app e controlla i dati
        val db = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
            dbName
        ).addMigrations(*Migrations.ALL).allowMainThreadQueries().build()

        runBlocking {
            val g = db.giornalieraDao().getById(1000)!!
            assertEquals("INVIATA", g.stato.name)
            assertEquals("Ispezione via Corridoni", g.note)
            assertEquals(1762210800000L, g.data)
            assertEquals(0, g.revisione)
            assertNull(g.pdfFileRef)

            val persone = db.personaleGiornalieraDao().getByGiornalieraId(1000).first()
            assertEquals(2, persone.size)
            assertEquals(16f, persone.sumOf { it.oreLavoro.toDouble() }.toFloat())
            assertTrue(persone.any { it.pernotto })

            assertEquals(1, db.mezzoGiornalieraDao().getByGiornalieraId(1000).first().size)
            assertEquals(1, db.attrezzaturaGiornalieraDao().getByGiornalieraId(1000).first().size)

            // Anagrafiche: tutte attive, codici articolo intatti
            val mezzo = db.mezzoDao().getById(200)!!
            assertTrue(mezzo.attivo)
            assertEquals("KM01", mezzo.codiceArticoloKm)
            assertEquals(2, db.personaleDao().getAll().first().size)
            assertEquals(1234, db.commessaDao().getById(1)!!.nome)
        }
        db.close()
    }

    @Test
    fun eliminarePersona_nonLaTogliedalleGiornaliereVecchie() {
        creaDatabaseV6()
        helper.runMigrationsAndValidate(dbName, 7, true, Migrations.MIGRATION_6_7).close()
        val db = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
            dbName
        ).addMigrations(*Migrations.ALL).allowMainThreadQueries().build()

        runBlocking {
            db.personaleDao().disattiva(101)
            // Sparisce dalle liste...
            assertEquals(listOf(100L), db.personaleDao().getAll().first().map { it.id })
            assertFalse(db.personaleDao().getById(101)!!.attivo)
            // ...ma resta nella giornaliera già inviata
            assertEquals(2, db.personaleGiornalieraDao().getByGiornalieraId(1000).first().size)
        }
        db.close()
    }
}
