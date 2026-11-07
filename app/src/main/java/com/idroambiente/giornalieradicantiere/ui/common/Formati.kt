package com.idroambiente.giornalieradicantiere.ui.common

import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

/** Formati di date e ore usati nelle schermate (italiano, come nel prototipo). */
object Formati {
    private val GG = listOf("dom", "lun", "mar", "mer", "gio", "ven", "sab")
    private val GIORNI = listOf("Domenica", "Lunedì", "Martedì", "Mercoledì", "Giovedì", "Venerdì", "Sabato")
    private val MM = listOf("gen", "feb", "mar", "apr", "mag", "giu", "lug", "ago", "set", "ott", "nov", "dic")
    private val MESI = listOf(
        "gennaio", "febbraio", "marzo", "aprile", "maggio", "giugno",
        "luglio", "agosto", "settembre", "ottobre", "novembre", "dicembre"
    )

    private fun cal(millis: Long) = Calendar.getInstance().apply { timeInMillis = millis }

    /** "gio 5 nov" */
    fun dataLunga(millis: Long): String {
        val c = cal(millis)
        return "${GG[c.get(Calendar.DAY_OF_WEEK) - 1]} ${c.get(Calendar.DAY_OF_MONTH)} ${MM[c.get(Calendar.MONTH)]}"
    }

    /** "Giovedì 5 novembre" */
    fun oggiEsteso(millis: Long): String {
        val c = cal(millis)
        return "${GIORNI[c.get(Calendar.DAY_OF_WEEK) - 1]} ${c.get(Calendar.DAY_OF_MONTH)} ${MESI[c.get(Calendar.MONTH)]}"
    }

    /** "05/11" */
    fun dataBreve(millis: Long): String {
        val c = cal(millis)
        return "%02d/%02d".format(c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1)
    }

    /** "05/11 18:20" */
    fun dataOra(millis: Long): String {
        val c = cal(millis)
        return "%02d/%02d %02d:%02d".format(
            c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)
        )
    }

    /** "Novembre" */
    fun meseCapitalizzato(millis: Long): String =
        MESI[cal(millis).get(Calendar.MONTH)].replaceFirstChar { it.titlecase(Locale.ITALY) }

    /** Ore con una cifra decimale e virgola: 16.5 → "16,5", 8.0 → "8" */
    fun ore(ore: Float): String {
        val v = (ore * 10).roundToInt() / 10f
        return if (v == v.toInt().toFloat()) v.toInt().toString() else v.toString().replace('.', ',')
    }

    /** Minuti dalla mezzanotte → "07:30" */
    fun hhmm(minuti: Int): String = "%02d:%02d".format((minuti / 60) % 24, minuti % 60)

    /** Km senza decimali inutili */
    fun km(km: Float): String = if (km == km.toInt().toFloat()) km.toInt().toString() else "%.1f".format(Locale.ITALY, km)

    fun inizioGiorno(millis: Long): Long = cal(millis).apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun stessoGiorno(a: Long, b: Long): Boolean = inizioGiorno(a) == inizioGiorno(b)
}
