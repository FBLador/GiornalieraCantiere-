package com.idroambiente.giornalieradicantiere.database

import androidx.room.TypeConverter
import com.idroambiente.giornalieradicantiere.database.entities.StatoCommessa
import com.idroambiente.giornalieradicantiere.database.entities.StatoGiornaliera

class Converters {

    @TypeConverter
    fun fromStatoCommessa(stato: StatoCommessa): String {
        return stato.name
    }

    @TypeConverter
    fun toStatoCommessa(value: String): StatoCommessa {
        return StatoCommessa.valueOf(value)
    }

    @TypeConverter
    fun fromStatoGiornaliera(stato: StatoGiornaliera): String {
        return stato.name
    }

    @TypeConverter
    fun toStatoGiornaliera(value: String): StatoGiornaliera {
        return StatoGiornaliera.valueOf(value)
    }
}
