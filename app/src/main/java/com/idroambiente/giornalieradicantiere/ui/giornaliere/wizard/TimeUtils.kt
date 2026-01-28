package com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard

/**
 * Formatta millisecondi dall'inizio del giorno in formato HH:mm.
 *
 * Nota: Questo NON è un timestamp Unix, ma millisecondi dall'inizio della giornata
 * (es. 08:00 = 28.800.000 ms). Non usare Date/SimpleDateFormat per evitare
 * problemi di timezone.
 */
fun formatTimeFromMillis(millis: Long): String {
    val h = ((millis / 1000 / 60 / 60) % 24).toInt()
    val m = ((millis / 1000 / 60) % 60).toInt()
    return String.format("%02d:%02d", h, m)
}
