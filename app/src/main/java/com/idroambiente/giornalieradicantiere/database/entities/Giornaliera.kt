package com.idroambiente.giornalieradicantiere.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "giornaliera",
    foreignKeys = [
        ForeignKey(
            entity = Cantiere::class,
            parentColumns = ["id"],
            childColumns = ["cantiereId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("cantiereId")]
)
data class Giornaliera(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cantiereId: Long,
    val data: Long,
    val stato: StatoGiornaliera = StatoGiornaliera.BOZZA,
    val note: String? = null,
    val dpiIdonei: Boolean = false,
    val presaVisionePOS: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    /** 0 = mai modificata dopo l'invio; ogni reinvio la aumenta di 1. */
    @ColumnInfo(defaultValue = "0")
    val revisione: Int = 0,
    /** Quando è stata reinviata l'ultima volta (null = mai). */
    val modificataAt: Long? = null,
    /** Dove sono finiti PDF e CSV all'invio: vedi [ArchivioFile]. */
    val fileArchivio: String? = null,
    /** ID del PDF su Drive, URI SAF o percorso locale. */
    val pdfFileRef: String? = null,
    val pdfFileName: String? = null,
    /** ID del CSV su Drive, URI SAF o percorso locale. */
    val csvFileRef: String? = null,
    val csvFileName: String? = null
)
