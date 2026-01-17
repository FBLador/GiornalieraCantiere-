package com.idroambiente.giornalieradicantiere.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cantiere",
    foreignKeys = [
        ForeignKey(
            entity = Commessa::class,
            parentColumns = ["id"],
            childColumns = ["commessaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("commessaId")]
)
data class Cantiere(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val commessaId: Long,
    val nome: String,
    val indirizzo: String
)
