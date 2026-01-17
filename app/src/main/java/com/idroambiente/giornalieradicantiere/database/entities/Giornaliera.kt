package com.idroambiente.giornalieradicantiere.database.entities

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
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
