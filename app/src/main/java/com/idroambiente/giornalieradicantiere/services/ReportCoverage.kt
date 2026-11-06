package com.idroambiente.giornalieradicantiere.services

/** Regole per capire se un report contiene una certa data. */
object ReportCoverage {
    private const val FINE_GIORNO_MS = 86_399_999L

    /**
     * Un report con periodo [start, end] contiene la giornaliera del giorno [data]?
     * - start/end null = periodo aperto (es. report "completo")
     * - end è incluso per tutto il giorno, come nel filtro della schermata Riepilogo
     */
    fun contiene(start: Long?, end: Long?, data: Long): Boolean =
        (start == null || data >= start) && (end == null || data <= end + FINE_GIORNO_MS)

    fun contieneAlmenoUna(start: Long?, end: Long?, date: Collection<Long>): Boolean =
        date.any { contiene(start, end, it) }
}
