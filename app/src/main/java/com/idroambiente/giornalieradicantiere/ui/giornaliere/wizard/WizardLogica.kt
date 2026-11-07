package com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard

import kotlin.math.max
import kotlin.math.roundToInt

/** Come si apre il wizard. */
enum class ModalitaWizard {
    /** Giornaliera nuova */
    NUOVA,
    /** Bozza ripresa */
    BOZZA,
    /** Giornaliera completata/inviata: sola lettura */
    DETTAGLIO,
    /** Modifica di una giornaliera completata (non ancora inviata) */
    MODIFICA,
    /** Modifica di una giornaliera INVIATA, dopo il PIN: alla fine si reinvia e si sostituiscono i file */
    MODIFICA_INVIATA
}

/** Ogni passo ha due fasi: prima scegli chi/cosa, poi compili i dati. */
enum class Fase { SELEZIONE, DATI }

/** Valori del modulo orari (minuti), proposti dall'ultima giornaliera. */
data class TurnoForm(
    val inizio: Int = 7 * 60 + 30,
    val fine: Int = 16 * 60 + 30,
    val pausa: Int = 60,
    val andata: Int = 30,
    val ritorno: Int = 30,
    val pernotto: Boolean = false
) {
    val lavoroMinuti: Int get() = max(0, fine - inizio - pausa)

    fun errore(): String? = when {
        fine <= inizio -> "L'orario di fine deve essere dopo l'inizio"
        pausa * 2 >= fine - inizio -> "La pausa deve essere inferiore alle ore di lavoro"
        else -> null
    }

    fun toFormData() = PersonaleFormData(
        presente = true,
        oreViaggioAndata = andata / 60f,
        oreViaggioRitorno = ritorno / 60f,
        oreLavoro = lavoroMinuti / 60f,
        orePausa = pausa / 60f,
        inizioTurno = inizio * 60_000L,
        fineTurno = fine * 60_000L,
        pernotto = pernotto
    )

    companion object {
        fun da(p: PersonaleFormData): TurnoForm {
            val inizio = (p.inizioTurno / 60_000L).toInt()
            val fine = (p.fineTurno / 60_000L).toInt()
            val pausa = if (fine > inizio) max(0, fine - inizio - (p.oreLavoro * 60).roundToInt()) else (p.orePausa * 60).roundToInt()
            return TurnoForm(
                inizio = inizio,
                fine = fine,
                pausa = pausa,
                andata = (p.oreViaggioAndata * 60).roundToInt(),
                ritorno = (p.oreViaggioRitorno * 60).roundToInt(),
                pernotto = p.pernotto
            )
        }
    }
}

fun PersonaleFormData.compilato(): Boolean = presente && oreLavoro > 0f
fun MezzoFormData.compilato(): Boolean = utilizzato && kmAndata != null && kmRitorno != null

/** Primo problema che impedisce l'invio: in quale passo e cosa manca. */
data class ProblemaInvio(val step: Int, val messaggio: String)

fun problemaInvio(
    persone: Map<Long, PersonaleFormData>,
    mezzi: Map<Long, MezzoFormData>,
    nomePersona: (Long) -> String,
    nomeMezzo: (Long) -> String,
    serveMotivo: Boolean,
    motivo: String
): ProblemaInvio? {
    val presenti = persone.filterValues { it.presente }
    val usati = mezzi.filterValues { it.utilizzato }
    val senzaDati = presenti.filterValues { !it.compilato() }.keys
    val senzaKm = usati.filterValues { !it.compilato() }.keys
    return when {
        presenti.isEmpty() -> ProblemaInvio(0, "Seleziona almeno una persona")
        senzaDati.isNotEmpty() -> ProblemaInvio(0, "Mancano i dati di " + senzaDati.joinToString(", ", transform = nomePersona))
        usati.isEmpty() -> ProblemaInvio(1, "Seleziona almeno un mezzo")
        senzaKm.isNotEmpty() -> ProblemaInvio(1, "Mancano i km di " + senzaKm.joinToString(", ", transform = nomeMezzo))
        serveMotivo && motivo.isBlank() -> ProblemaInvio(3, "Scrivi il motivo della modifica")
        else -> null
    }
}

// ───────────────────────── Barra in basso ─────────────────────────

enum class AzioneCta { MODIFICA, VAI_DATI_PERSONALE, AVANTI_DA_PERSONALE, VAI_DATI_MEZZI, AVANTI_DA_MEZZI, AVANTI_A_RIEPILOGO, INVIA }
enum class AzioneSinistra { ESCI, SELEZIONE_PERSONALE, STEP_0, SELEZIONE_MEZZI, STEP_1, STEP_2 }
enum class IconaCta { LUCCHETTO, MATITA, AVANTI, INVIA, SINCRONIZZA }
enum class StileCta { PRIMARIO, FORTE, DISATTIVO }

data class BarraWizard(
    val sinistra: AzioneSinistra?,
    val label: String,
    val icona: IconaCta,
    val azione: AzioneCta,
    val stile: StileCta,
    val suggerimento: String?,
    /** Pulsante giallo "Applica a N persone/mezzi" sopra la barra */
    val applica: String?
)

private fun plurale(n: Int, uno: String, tanti: String) = "$n ${if (n == 1) uno else tanti}"

/** Cosa mostra la barra in basso in ogni momento (regole del prototipo). */
fun barraWizard(s: WizardUiState, nomePersona: (Long) -> String, nomeMezzo: (Long) -> String): BarraWizard {
    if (s.modalita == ModalitaWizard.DETTAGLIO) {
        val inviata = s.giornaliera?.stato?.name == "INVIATA"
        return BarraWizard(
            sinistra = null,
            label = "Modifica giornaliera",
            icona = if (inviata) IconaCta.LUCCHETTO else IconaCta.MATITA,
            azione = AzioneCta.MODIFICA,
            stile = if (inviata) StileCta.FORTE else StileCta.PRIMARIO,
            suggerimento = if (inviata) "Serve il PIN del responsabile" else null,
            applica = null
        )
    }
    val presenti = s.personaleSelections.filterValues { it.presente }
    val usati = s.mezzoSelections.filterValues { it.utilizzato }
    val mancaP = presenti.values.count { !it.compilato() }
    val mancaM = usati.values.count { !it.compilato() }

    var b = when {
        s.currentStep == 0 && s.persPhase == Fase.SELEZIONE -> BarraWizard(
            AzioneSinistra.ESCI,
            if (presenti.isEmpty()) "Avanti: orari" else "Avanti: orari (${presenti.size})",
            IconaCta.AVANTI, AzioneCta.VAI_DATI_PERSONALE, StileCta.PRIMARIO,
            if (presenti.isEmpty()) "Seleziona chi è presente" else null, null
        )
        s.currentStep == 0 -> BarraWizard(
            AzioneSinistra.SELEZIONE_PERSONALE, "Avanti: Mezzi", IconaCta.AVANTI, AzioneCta.AVANTI_DA_PERSONALE, StileCta.PRIMARIO,
            if (mancaP > 0) "Mancano i dati di ${plurale(mancaP, "persona", "persone")}" else null,
            if (s.applica.isNotEmpty()) "Applica a ${plurale(s.applica.size, "persona", "persone")}" else null
        )
        s.currentStep == 1 && s.mezPhase == Fase.SELEZIONE -> BarraWizard(
            AzioneSinistra.STEP_0,
            if (usati.isEmpty()) "Avanti: km" else "Avanti: km (${usati.size})",
            IconaCta.AVANTI, AzioneCta.VAI_DATI_MEZZI, StileCta.PRIMARIO,
            if (usati.isEmpty()) "Seleziona almeno un mezzo" else null, null
        )
        s.currentStep == 1 -> BarraWizard(
            AzioneSinistra.SELEZIONE_MEZZI, "Avanti: Attrezzature", IconaCta.AVANTI, AzioneCta.AVANTI_DA_MEZZI, StileCta.PRIMARIO,
            if (mancaM > 0) "Mancano i km di ${plurale(mancaM, "mezzo", "mezzi")}" else null,
            if (s.applicaMezzi.isNotEmpty()) "Applica km a ${plurale(s.applicaMezzi.size, "mezzo", "mezzi")}" else null
        )
        s.currentStep == 2 -> BarraWizard(
            AzioneSinistra.STEP_1, "Avanti: Riepilogo", IconaCta.AVANTI, AzioneCta.AVANTI_A_RIEPILOGO, StileCta.PRIMARIO, null, null
        )
        else -> {
            val reinvio = s.modalita == ModalitaWizard.MODIFICA_INVIATA
            val problema = problemaInvio(s.personaleSelections, s.mezzoSelections, nomePersona, nomeMezzo, reinvio, s.motivo)
            BarraWizard(
                AzioneSinistra.STEP_2,
                if (reinvio) "Reinvia e sostituisci" else "Invia giornaliera",
                if (reinvio) IconaCta.SINCRONIZZA else IconaCta.INVIA,
                AzioneCta.INVIA, StileCta.PRIMARIO, problema?.messaggio, null
            )
        }
    }
    if (b.suggerimento != null) b = b.copy(stile = StileCta.DISATTIVO)
    return b
}
