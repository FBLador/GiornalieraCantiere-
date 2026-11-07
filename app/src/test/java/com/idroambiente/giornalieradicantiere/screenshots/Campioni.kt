package com.idroambiente.giornalieradicantiere.screenshots

import com.idroambiente.giornalieradicantiere.database.entities.Commessa
import com.idroambiente.giornalieradicantiere.database.entities.Giornaliera
import com.idroambiente.giornalieradicantiere.database.entities.StatoCommessa
import com.idroambiente.giornalieradicantiere.database.entities.StatoGiornaliera
import com.idroambiente.giornalieradicantiere.ui.commesse.CommessaCard
import com.idroambiente.giornalieradicantiere.ui.commesse.azionePer
import java.util.Calendar

/** Dati di esempio uguali a quelli del prototipo (giovedì 5 novembre 2026). */
object Campioni {
    fun giorno(d: Int, m: Int = 11, h: Int = 12): Long = Calendar.getInstance().apply {
        clear(); set(2026, m - 1, d, h, 0)
    }.timeInMillis

    val OGGI = giorno(5, h = 18)

    val commesse = listOf(
        Commessa(1, 1221, "Uniacque Bergamo", "Via Novelli 11, Bergamo", StatoCommessa.APERTA),
        Commessa(2, 1234, "Comune di Bergamo", "Piazza Matteotti 27, Bergamo", StatoCommessa.APERTA),
        Commessa(3, 1198, "Acque Bresciane S.p.A.", "Via Cefalonia 70, Brescia", StatoCommessa.SOSPESA),
        Commessa(4, 1150, "Consorzio Bonifica Est", "Via Roma 4, Crema", StatoCommessa.CHIUSA)
    )

    val giornaliere = listOf(
        Giornaliera(1, 1, giorno(4), StatoGiornaliera.INVIATA),
        Giornaliera(2, 1, giorno(3), StatoGiornaliera.INVIATA),
        Giornaliera(3, 2, giorno(5), StatoGiornaliera.INVIATA, note = "Ispezione tratto via Corridoni completata."),
        Giornaliera(4, 2, giorno(4), StatoGiornaliera.INVIATA, revisione = 1, modificataAt = giorno(4, h = 18)),
        Giornaliera(5, 2, giorno(31, 10), StatoGiornaliera.COMPLETATA),
        Giornaliera(6, 3, giorno(4), StatoGiornaliera.BOZZA),
        Giornaliera(7, 3, giorno(28, 10), StatoGiornaliera.INVIATA),
        Giornaliera(8, 4, giorno(12, 9), StatoGiornaliera.INVIATA)
    )

    /** cantiereId = commessaId negli esempi */
    val cards: List<CommessaCard> = commesse.map { c ->
        val gs = giornaliere.filter { it.cantiereId == c.id }
        CommessaCard(c, c.id, gs.size, azionePer(c, gs, OGGI))
    }
}
