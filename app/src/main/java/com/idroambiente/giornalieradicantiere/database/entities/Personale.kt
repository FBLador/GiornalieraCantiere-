package com.idroambiente.giornalieradicantiere.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "personale")
data class Personale(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nome: String,
    val cognome: String,
    /** false = eliminato dalle liste, ma resta nelle giornaliere già compilate. */
    @ColumnInfo(defaultValue = "1")
    val attivo: Boolean = true
)
