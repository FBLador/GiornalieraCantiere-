package com.idroambiente.giornalieradicantiere.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mezzo")
data class Mezzo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val targa: String,
    val nome: String,
    val codiceArticoloKm: String = "",
    val codiceArticoloGiorni: String = "",
    /** false = eliminato dalle liste, ma resta nelle giornaliere già compilate. */
    @ColumnInfo(defaultValue = "1")
    val attivo: Boolean = true
)
