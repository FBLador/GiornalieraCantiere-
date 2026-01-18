package com.idroambiente.giornalieradicantiere.ui.navigation

object Routes {
    const val COMMESSE_LIST = "commesse_list"
    const val GIORNALIERE_LIST = "giornaliere_list/{commessaId}"
    const val IMPOSTAZIONI = "impostazioni"
    const val PERSONALE_LIST = "personale_list"

    fun giornaliereList(commessaId: Long) = "giornaliere_list/$commessaId"
}
