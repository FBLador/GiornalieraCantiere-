package com.idroambiente.giornalieradicantiere.database.entities

/** Dove sono stati salvati PDF e CSV di una giornaliera o di un report. */
enum class ArchivioFile {
    /** Google Drive: il riferimento è l'ID del file. */
    DRIVE,
    /** Cartella scelta sul telefono (SAF): il riferimento è l'URI del documento. */
    LOCALE,
    /** Cartella interna dell'app (fallback): il riferimento è il percorso del file. */
    APP,
    /** Solo condiviso (WhatsApp, email...): non c'è un file da sostituire. */
    CONDIVISO
}
