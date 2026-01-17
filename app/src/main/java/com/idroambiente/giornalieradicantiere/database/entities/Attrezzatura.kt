package com.idroambiente.giornalieradicantiere.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attrezzatura")
data class Attrezzatura(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nome: String
)
