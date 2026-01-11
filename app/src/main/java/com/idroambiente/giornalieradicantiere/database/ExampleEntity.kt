package com.idroambiente.giornalieradicantiere.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "example_table")
data class ExampleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
