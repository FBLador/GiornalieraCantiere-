package com.idroambiente.giornalieradicantiere.di

import com.idroambiente.giornalieradicantiere.database.ExampleDao
import com.idroambiente.giornalieradicantiere.database.ExampleRepository
import com.idroambiente.giornalieradicantiere.database.dao.CommessaDao
import com.idroambiente.giornalieradicantiere.database.repository.CommessaRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideExampleRepository(
        exampleDao: ExampleDao
    ): ExampleRepository {
        return ExampleRepository(exampleDao)
    }

    @Provides
    @Singleton
    fun provideCommessaRepository(
        commessaDao: CommessaDao
    ): CommessaRepository {
        return CommessaRepository(commessaDao)
    }
}
