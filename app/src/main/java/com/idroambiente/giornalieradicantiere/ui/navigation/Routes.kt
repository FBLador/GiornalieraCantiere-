package com.idroambiente.giornalieradicantiere.ui.navigation

object Routes {
    const val SETUP = "setup"
    const val COMMESSE_LIST = "commesse_list"
    const val GIORNALIERE_LIST = "giornaliere_list/{commessaId}"
    const val GIORNALIERA_WIZARD = "giornaliera_wizard/{cantiereId}?giornalieraId={giornalieraId}"
    const val IMPOSTAZIONI = "impostazioni"
    const val PERSONALE_LIST = "personale_list"
    const val MEZZO_LIST = "mezzo_list"
    const val ATTREZZATURA_LIST = "attrezzatura_list"
    const val LISTE_GESTIONE = "liste_gestione"
    const val RIEPILOGO_COMMESSA = "riepilogo_commessa/{commessaId}"

    fun giornaliereList(commessaId: Long) = "giornaliere_list/$commessaId"

    fun riepilogoCommessa(commessaId: Long) = "riepilogo_commessa/$commessaId"

    fun giornalieraWizard(cantiereId: Long, giornalieraId: Long? = null): String {
        return if (giornalieraId != null) {
            "giornaliera_wizard/$cantiereId?giornalieraId=$giornalieraId"
        } else {
            "giornaliera_wizard/$cantiereId"
        }
    }
}
