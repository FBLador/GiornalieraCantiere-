package com.idroambiente.giornalieradicantiere.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Elenco delle migrazioni del database.
 *
 * Regola: ogni volta che cambia una entità si alza la versione in [AppDatabase]
 * e si aggiunge qui la migrazione corrispondente. Mai usare la migrazione
 * distruttiva: cancellerebbe le giornaliere sui telefoni.
 */
object Migrations {

    /**
     * v6 → v7: modifica delle giornaliere inviate.
     * - giornaliera: revisione, data ultima modifica, riferimenti ai file PDF/CSV
     * - personale/mezzo/attrezzatura: flag "attivo" (eliminazione senza perdere lo storico)
     * - nuove tabelle: storico modifiche, report esportati
     * Nessun dato esistente viene toccato.
     */
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `giornaliera` ADD COLUMN `revisione` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `giornaliera` ADD COLUMN `modificataAt` INTEGER")
            db.execSQL("ALTER TABLE `giornaliera` ADD COLUMN `fileArchivio` TEXT")
            db.execSQL("ALTER TABLE `giornaliera` ADD COLUMN `pdfFileRef` TEXT")
            db.execSQL("ALTER TABLE `giornaliera` ADD COLUMN `pdfFileName` TEXT")
            db.execSQL("ALTER TABLE `giornaliera` ADD COLUMN `csvFileRef` TEXT")
            db.execSQL("ALTER TABLE `giornaliera` ADD COLUMN `csvFileName` TEXT")

            db.execSQL("ALTER TABLE `personale` ADD COLUMN `attivo` INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE `mezzo` ADD COLUMN `attivo` INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE `attrezzatura` ADD COLUMN `attivo` INTEGER NOT NULL DEFAULT 1")

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `giornaliera_modifica` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`giornalieraId` INTEGER NOT NULL, `revisione` INTEGER NOT NULL, `modificataAt` INTEGER NOT NULL, " +
                    "`motivo` TEXT NOT NULL, `dataPrecedente` INTEGER, `dataNuova` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`giornalieraId`) REFERENCES `giornaliera`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_giornaliera_modifica_giornalieraId` ON `giornaliera_modifica` (`giornalieraId`)")

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `report_esportato` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`commessaId` INTEGER NOT NULL, `startDate` INTEGER, `endDate` INTEGER, `archivio` TEXT NOT NULL, " +
                    "`pdfFileName` TEXT NOT NULL, `pdfFileRef` TEXT, `csvFileName` TEXT, `csvFileRef` TEXT, " +
                    "`exportedAt` INTEGER NOT NULL, `daAggiornare` INTEGER NOT NULL DEFAULT 0, " +
                    "FOREIGN KEY(`commessaId`) REFERENCES `commessa`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_report_esportato_commessaId` ON `report_esportato` (`commessaId`)")
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_report_esportato_commessaId_archivio_pdfFileName` " +
                    "ON `report_esportato` (`commessaId`, `archivio`, `pdfFileName`)"
            )
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_6_7)
}
