package com.idroambiente.giornalieradicantiere.di

import android.content.Context
import androidx.room.Room
import com.idroambiente.giornalieradicantiere.database.AppDatabase
import com.idroambiente.giornalieradicantiere.database.ExampleDao
import com.idroambiente.giornalieradicantiere.database.dao.AttrezzaturaDao
import com.idroambiente.giornalieradicantiere.database.dao.AttrezzaturaGiornalieraDao
import com.idroambiente.giornalieradicantiere.database.dao.CantiereDao
import com.idroambiente.giornalieradicantiere.database.dao.CommessaDao
import com.idroambiente.giornalieradicantiere.database.dao.GiornalieraDao
import com.idroambiente.giornalieradicantiere.database.dao.MezzoDao
import com.idroambiente.giornalieradicantiere.database.dao.MezzoGiornalieraDao
import com.idroambiente.giornalieradicantiere.database.dao.PersonaleDao
import com.idroambiente.giornalieradicantiere.database.dao.PersonaleGiornalieraDao
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
        ).fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideExampleDao(database: AppDatabase): ExampleDao {
        return database.exampleDao()
    }

    @Provides
    fun provideCommessaDao(database: AppDatabase): CommessaDao {
        return database.commessaDao()
    }

    @Provides
    fun provideCantiereDao(database: AppDatabase): CantiereDao {
        return database.cantiereDao()
    }

    @Provides
    fun provideGiornalieraDao(database: AppDatabase): GiornalieraDao {
        return database.giornalieraDao()
    }

    @Provides
    fun providePersonaleDao(database: AppDatabase): PersonaleDao {
        return database.personaleDao()
    }

    @Provides
    fun providePersonaleGiornalieraDao(database: AppDatabase): PersonaleGiornalieraDao {
        return database.personaleGiornalieraDao()
    }

    @Provides
    fun provideMezzoDao(database: AppDatabase): MezzoDao {
        return database.mezzoDao()
    }

    @Provides
    fun provideMezzoGiornalieraDao(database: AppDatabase): MezzoGiornalieraDao {
        return database.mezzoGiornalieraDao()
    }

    @Provides
    fun provideAttrezzaturaDao(database: AppDatabase): AttrezzaturaDao {
        return database.attrezzaturaDao()
    }

    @Provides
    fun provideAttrezzaturaGiornalieraDao(database: AppDatabase): AttrezzaturaGiornalieraDao {
        return database.attrezzaturaGiornalieraDao()
    }
}
