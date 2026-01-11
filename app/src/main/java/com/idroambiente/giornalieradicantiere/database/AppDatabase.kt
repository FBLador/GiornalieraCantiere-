package com.idroambiente.giornalieradicantiere.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ExampleEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exampleDao(): ExampleDao
}
  