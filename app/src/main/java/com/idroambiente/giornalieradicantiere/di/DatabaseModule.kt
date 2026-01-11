package com.idroambiente.giornalieradicantiere.di

import android.content.Context
import androidx.room.Room
import com.idroambiente.giornalieradicantiere.database.AppDatabase
import com.idroambiente.giornalieradicantiere.database.ExampleDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "app_database"
        ).build()
    }
    
    @Provides
    fun provideExampleDao(database: AppDatabase): ExampleDao {
        return database.exampleDao()
    }
}
