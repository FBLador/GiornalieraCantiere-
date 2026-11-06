package com.idroambiente.giornalieradicantiere.database

import androidx.room.migration.Migration

/**
 * Elenco delle migrazioni del database.
 *
 * Regola: ogni volta che cambia una entità si alza la versione in [AppDatabase]
 * e si aggiunge qui la migrazione corrispondente. Mai usare la migrazione
 * distruttiva: cancellerebbe le giornaliere sui telefoni.
 */
object Migrations {
    val ALL: Array<Migration> = arrayOf()
}
