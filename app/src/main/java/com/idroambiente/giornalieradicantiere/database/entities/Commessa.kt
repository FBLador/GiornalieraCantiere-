package com.idroambiente.giornalieradicantiere.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "commessa")
data class Commessa(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nome: Int,
    val cliente: String,
    val indirizzo: String,
    val stato: StatoCommessa = StatoCommessa.APERTA
)
