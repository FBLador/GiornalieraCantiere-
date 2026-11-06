package com.idroambiente.giornalieradicantiere.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Report (riepilogo commessa) esportato da QUESTO telefono.
 *
 * Serve a sapere quali report aggiornare quando si modifica una giornaliera:
 * l'app tocca solo i report che ha creato lei, perché un report fatto da un
 * altro telefono può contenere giornaliere che qui non ci sono.
 */
@Entity(
    tableName = "report_esportato",
    foreignKeys = [
        ForeignKey(
            entity = Commessa::class,
            parentColumns = ["id"],
            childColumns = ["commessaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("commessaId"),
        Index(value = ["commessaId", "archivio", "pdfFileName"], unique = true)
    ]
)
data class ReportEsportato(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val commessaId: Long,
    /** Inizio periodo (null = dall'inizio). */
    val startDate: Long? = null,
    /** Fine periodo, giorno incluso (null = fino alla fine). */
    val endDate: Long? = null,
    /** Vedi [ArchivioFile]. */
    val archivio: String,
    val pdfFileName: String,
    val pdfFileRef: String? = null,
    val csvFileName: String? = null,
    val csvFileRef: String? = null,
    val exportedAt: Long,
    /** true = una giornaliera è cambiata ma l'aggiornamento automatico non è riuscito. */
    @ColumnInfo(defaultValue = "0")
    val daAggiornare: Boolean = false
)
