package com.idroambiente.giornalieradicantiere.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "personale_giornaliera",
    foreignKeys = [
        ForeignKey(
            entity = Giornaliera::class,
            parentColumns = ["id"],
            childColumns = ["giornalieraId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Personale::class,
            parentColumns = ["id"],
            childColumns = ["personaleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("giornalieraId"), Index("personaleId")]
)
data class PersonaleGiornaliera(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val giornalieraId: Long,
    val personaleId: Long,
    val oreViaggio: Float = 0f,
    val oreLavoro: Float = 0f,
    val inizioTurno: Long,
    val fineTurno: Long,
    val pernotto: Boolean = false
)
