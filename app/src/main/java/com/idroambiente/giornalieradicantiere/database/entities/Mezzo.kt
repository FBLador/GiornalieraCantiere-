package com.idroambiente.giornalieradicantiere.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mezzo")
data class Mezzo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val targa: String,
    val nome: String,
    val codiceArticoloKm: String = "",
    val codiceArticoloGiorni: String = ""
)
