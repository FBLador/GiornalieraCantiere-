package com.idroambiente.giornalieradicantiere.di

import com.idroambiente.giornalieradicantiere.database.ExampleDao
import com.idroambiente.giornalieradicantiere.database.ExampleRepository
import com.idroambiente.giornalieradicantiere.database.dao.AttrezzaturaDao
import com.idroambiente.giornalieradicantiere.database.dao.AttrezzaturaGiornalieraDao
import com.idroambiente.giornalieradicantiere.database.dao.CantiereDao
import com.idroambiente.giornalieradicantiere.database.dao.CommessaDao
import com.idroambiente.giornalieradicantiere.database.dao.GiornalieraDao
import com.idroambiente.giornalieradicantiere.database.dao.MezzoDao
import com.idroambiente.giornalieradicantiere.database.dao.MezzoGiornalieraDao
import com.idroambiente.giornalieradicantiere.database.dao.PersonaleDao
import com.idroambiente.giornalieradicantiere.database.dao.PersonaleGiornalieraDao
import com.idroambiente.giornalieradicantiere.database.repository.AttrezzaturaGiornalieraRepository
import com.idroambiente.giornalieradicantiere.database.repository.AttrezzaturaRepository
import com.idroambiente.giornalieradicantiere.database.repository.CantiereRepository
import com.idroambiente.giornalieradicantiere.database.repository.CommessaRepository
import com.idroambiente.giornalieradicantiere.database.repository.GiornalieraRepository
import com.idroambiente.giornalieradicantiere.database.repository.MezzoGiornalieraRepository
import com.idroambiente.giornalieradicantiere.database.repository.MezzoRepository
import com.idroambiente.giornalieradicantiere.database.repository.PersonaleGiornalieraRepository
import com.idroambiente.giornalieradicantiere.database.repository.PersonaleRepository
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

    @Provides
    @Singleton
    fun provideCantiereRepository(
        cantiereDao: CantiereDao
    ): CantiereRepository {
        return CantiereRepository(cantiereDao)
    }

    @Provides
    @Singleton
    fun provideGiornalieraRepository(
        giornalieraDao: GiornalieraDao
    ): GiornalieraRepository {
        return GiornalieraRepository(giornalieraDao)
    }

    @Provides
    @Singleton
    fun providePersonaleRepository(
        personaleDao: PersonaleDao
    ): PersonaleRepository {
        return PersonaleRepository(personaleDao)
    }

    @Provides
    @Singleton
    fun providePersonaleGiornalieraRepository(
        personaleGiornalieraDao: PersonaleGiornalieraDao
    ): PersonaleGiornalieraRepository {
        return PersonaleGiornalieraRepository(personaleGiornalieraDao)
    }

    @Provides
    @Singleton
    fun provideMezzoRepository(
        mezzoDao: MezzoDao
    ): MezzoRepository {
        return MezzoRepository(mezzoDao)
    }

    @Provides
    @Singleton
    fun provideMezzoGiornalieraRepository(
        mezzoGiornalieraDao: MezzoGiornalieraDao
    ): MezzoGiornalieraRepository {
        return MezzoGiornalieraRepository(mezzoGiornalieraDao)
    }

    @Provides
    @Singleton
    fun provideAttrezzaturaRepository(
        attrezzaturaDao: AttrezzaturaDao
    ): AttrezzaturaRepository {
        return AttrezzaturaRepository(attrezzaturaDao)
    }

    @Provides
    @Singleton
    fun provideAttrezzaturaGiornalieraRepository(
        attrezzaturaGiornalieraDao: AttrezzaturaGiornalieraDao
    ): AttrezzaturaGiornalieraRepository {
        return AttrezzaturaGiornalieraRepository(attrezzaturaGiornalieraDao)
    }
}
