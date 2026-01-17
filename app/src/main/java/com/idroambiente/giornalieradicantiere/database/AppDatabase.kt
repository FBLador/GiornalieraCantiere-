package com.idroambiente.giornalieradicantiere.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.idroambiente.giornalieradicantiere.database.entities.Attrezzatura
import com.idroambiente.giornalieradicantiere.database.entities.AttrezzaturaGiornaliera
import com.idroambiente.giornalieradicantiere.database.entities.Cantiere
import com.idroambiente.giornalieradicantiere.database.entities.Commessa
import com.idroambiente.giornalieradicantiere.database.entities.Giornaliera
import com.idroambiente.giornalieradicantiere.database.entities.Mezzo
import com.idroambiente.giornalieradicantiere.database.entities.MezzoGiornaliera
import com.idroambiente.giornalieradicantiere.database.entities.Personale
import com.idroambiente.giornalieradicantiere.database.entities.PersonaleGiornaliera

@Database(
    entities = [
        ExampleEntity::class,
        Commessa::class,
        Cantiere::class,
        Giornaliera::class,
        Personale::class,
        PersonaleGiornaliera::class,
        Mezzo::class,
        MezzoGiornaliera::class,
        Attrezzatura::class,
        AttrezzaturaGiornaliera::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exampleDao(): ExampleDao
}
