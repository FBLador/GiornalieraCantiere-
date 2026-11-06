package com.idroambiente.giornalieradicantiere.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Storico delle modifiche fatte a una giornaliera dopo l'invio. */
@Entity(
    tableName = "giornaliera_modifica",
    foreignKeys = [
        ForeignKey(
            entity = Giornaliera::class,
            parentColumns = ["id"],
            childColumns = ["giornalieraId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("giornalieraId")]
)
data class GiornalieraModifica(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val giornalieraId: Long,
    /** Revisione raggiunta con questa modifica (1, 2, ...). */
    val revisione: Int,
    val modificataAt: Long,
    val motivo: String,
    /** Data della giornaliera prima della modifica, se è stata spostata. */
    val dataPrecedente: Long? = null,
    val dataNuova: Long
)
