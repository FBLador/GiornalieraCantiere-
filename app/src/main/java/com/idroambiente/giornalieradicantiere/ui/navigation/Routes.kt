package com.idroambiente.giornalieradicantiere.ui.navigation

object Routes {
    const val COMMESSE_LIST = "commesse_list"
    const val GIORNALIERE_LIST = "giornaliere_list/{commessaId}"
    const val GIORNALIERA_WIZARD = "giornaliera_wizard/{cantiereId}"
    const val IMPOSTAZIONI = "impostazioni"
    const val PERSONALE_LIST = "personale_list"
    const val MEZZO_LIST = "mezzo_list"
    const val ATTREZZATURA_LIST = "attrezzatura_list"

    fun giornaliereList(commessaId: Long) = "giornaliere_list/$commessaId"
    fun giornalieraWizard(cantiereId: Long) = "giornaliera_wizard/$cantiereId"
}
