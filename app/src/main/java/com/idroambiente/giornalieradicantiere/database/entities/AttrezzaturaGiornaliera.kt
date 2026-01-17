package com.idroambiente.giornalieradicantiere.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "attrezzatura_giornaliera",
    primaryKeys = ["giornalieraId", "attrezzaturaId"],
    foreignKeys = [
        ForeignKey(
            entity = Giornaliera::class,
            parentColumns = ["id"],
            childColumns = ["giornalieraId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Attrezzatura::class,
            parentColumns = ["id"],
            childColumns = ["attrezzaturaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("giornalieraId"), Index("attrezzaturaId")]
)
data class AttrezzaturaGiornaliera(
    val giornalieraId: Long,
    val attrezzaturaId: Long
)
