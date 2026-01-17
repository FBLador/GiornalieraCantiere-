package com.idroambiente.giornalieradicantiere.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "mezzo_giornaliera",
    foreignKeys = [
        ForeignKey(
            entity = Giornaliera::class,
            parentColumns = ["id"],
            childColumns = ["giornalieraId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Mezzo::class,
            parentColumns = ["id"],
            childColumns = ["mezzoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("giornalieraId"), Index("mezzoId")]
)
data class MezzoGiornaliera(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val giornalieraId: Long,
    val mezzoId: Long,
    val kmAndata: Float = 0f,
    val kmRitorno: Float = 0f
)
