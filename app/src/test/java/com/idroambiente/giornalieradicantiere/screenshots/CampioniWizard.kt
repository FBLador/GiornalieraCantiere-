package com.idroambiente.giornalieradicantiere.screenshots

import com.idroambiente.giornalieradicantiere.database.entities.ArchivioFile
import com.idroambiente.giornalieradicantiere.database.entities.Attrezzatura
import com.idroambiente.giornalieradicantiere.database.entities.Cantiere
import com.idroambiente.giornalieradicantiere.database.entities.Giornaliera
import com.idroambiente.giornalieradicantiere.database.entities.GiornalieraModifica
import com.idroambiente.giornalieradicantiere.database.entities.Mezzo
import com.idroambiente.giornalieradicantiere.database.entities.Personale
import com.idroambiente.giornalieradicantiere.database.entities.StatoGiornaliera
import com.idroambiente.giornalieradicantiere.services.EsitoSostituzione
import com.idroambiente.giornalieradicantiere.services.FaseInvio
import com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard.AttrezzaturaFormData
import com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard.EsitoAggiornamento
import com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard.Fase
import com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard.MezzoFormData
import com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard.ModalitaWizard
import com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard.PinUiState
import com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard.RiepilogoData
import com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard.TurnoForm
import com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard.WizardDialog
import com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard.WizardUiState

object CampioniWizard {
    val persone = listOf(
        Personale(1, "Marco", "Rossi"), Personale(2, "Luca", "Bianchi"), Personale(3, "Giuseppe", "Ferrari"),
        Personale(4, "Andrea", "Colombo"), Personale(5, "Stefano", "Ricci"), Personale(6, "Paolo", "Esposito"),
        Personale(7, "Davide", "Greco")
    )
    val mezzi = listOf(
        Mezzo(1, "FG 123 AB", "Iveco Daily"), Mezzo(2, "EZ 456 CD", "Autospurgo Scania"),
        Mezzo(3, "FK 302 LM", "Furgone TV ispezioni"), Mezzo(4, "GH 789 EF", "Ford Transit")
    )
    val attrezzature = listOf(
        Attrezzatura(1, "Telecamera ispezione"), Attrezzatura(2, "Pompa alta pressione"), Attrezzatura(3, "Generatore 5 kW"),
        Attrezzatura(4, "Kit manicotto"), Attrezzatura(5, "Aspiratore fanghi"), Attrezzatura(6, "Martello demolitore")
    )

    fun nome(id: Long) = persone.first { it.id == id }.let { "${it.nome} ${it.cognome}" }
    fun nomeMezzo(id: Long) = mezzi.first { it.id == id }.nome

    private val compilato = TurnoForm().toFormData()
    private val commessa = Campioni.commesse[1]
    private val cantiere = Cantiere(2, 2, "0001234", "")

    val nuova = WizardUiState(
        modalita = ModalitaWizard.NUOVA, isLoading = false, commessa = Campioni.commesse[0], cantiere = Cantiere(1, 1, "0001221", ""),
        selectedDate = Campioni.OGGI, squadraPrecedente = listOf(1, 2, 3), formDa = Campioni.giorno(4)
    )

    val personeSelezione = nuova.copy(personaleSelections = mapOf(1L to compilato.copy(oreLavoro = 0f), 2L to compilato.copy(oreLavoro = 0f), 3L to compilato.copy(oreLavoro = 0f)))

    val personeDati = nuova.copy(
        persPhase = Fase.DATI,
        personaleSelections = mapOf(1L to compilato, 2L to compilato.copy(oreLavoro = 0f), 3L to compilato.copy(oreLavoro = 0f)),
        applica = setOf(2L, 3L)
    )

    private val persCompilate = mapOf(1L to compilato, 2L to compilato, 3L to compilato.copy(pernotto = true))

    val mezziSelezione = nuova.copy(currentStep = 1, maxStep = 1, personaleSelections = persCompilate, mezzoSelections = mapOf(1L to MezzoFormData(true), 2L to MezzoFormData(true)))

    val mezziDati = mezziSelezione.copy(
        mezPhase = Fase.DATI, kmAndata = "38", kmRitorno = "38",
        mezzoSelections = mapOf(1L to MezzoFormData(true, 19f, 19f), 2L to MezzoFormData(true)), applicaMezzi = setOf(2L)
    )

    val attrezzatureStep = mezziDati.copy(
        currentStep = 2, maxStep = 2, mezzoSelections = mapOf(1L to MezzoFormData(true, 19f, 19f), 2L to MezzoFormData(true, 19f, 19f)),
        applicaMezzi = emptySet(), attrezzaturaSelections = mapOf(1L to AttrezzaturaFormData(true), 2L to AttrezzaturaFormData(true))
    )

    val riepilogoNuova = attrezzatureStep.copy(
        currentStep = 3, maxStep = 3,
        riepilogo = RiepilogoData(3, 24f, 3f, 1, 2, 76f, 2)
    )

    // ── Giornaliera del 04/11 di 0001234, inviata e già modificata una volta (come nel prototipo) ──
    private val g4 = Giornaliera(
        id = 4, cantiereId = 2, data = Campioni.giorno(4), stato = StatoGiornaliera.INVIATA,
        revisione = 1, modificataAt = Campioni.giorno(4, h = 18), fileArchivio = "DRIVE",
        pdfFileRef = "drive-id", pdfFileName = "0001234_2026-11-04.pdf"
    )
    private val storico = listOf(
        GiornalieraModifica(1, 4, 1, Campioni.giorno(4, h = 18) + 20 * 60_000, "Corretto il viaggio di ritorno di Paolo Esposito", null, Campioni.giorno(4))
    )
    private val persDettaglio = mapOf(5L to compilato, 6L to compilato)

    val dettaglio = WizardUiState(
        modalita = ModalitaWizard.DETTAGLIO, isLoading = false, giornaliera = g4, commessa = commessa, cantiere = cantiere,
        currentStep = 3, maxStep = 3, personaleSelections = persDettaglio,
        mezzoSelections = mapOf(3L to MezzoFormData(true, 22f, 22f)), attrezzaturaSelections = mapOf(1L to AttrezzaturaFormData(true)),
        selectedDate = g4.data, modifiche = storico, riepilogo = RiepilogoData(2, 16f, 2f, 0, 1, 44f, 1)
    )

    val pin = dettaglio.copy(pin = PinUiState(cifre = "202"))
    val pinErrato = dettaglio.copy(pin = PinUiState(errore = "PIN errato · 4 tentativi rimasti"))

    val modifica = dettaglio.copy(
        modalita = ModalitaWizard.MODIFICA_INVIATA, dataOriginale = g4.data,
        motivo = "Data sbagliata: il lavoro era di lunedì 2", selectedDate = Campioni.giorno(2)
    )

    val modificaSenzaMotivo = modifica.copy(motivo = "", selectedDate = g4.data)

    val invio = modifica.copy(
        fasiInvio = listOf(
            FaseInvio.GENERAZIONE_PDF, FaseInvio.GENERAZIONE_CSV, FaseInvio.SOSTITUZIONE_PDF,
            FaseInvio.SOSTITUZIONE_CSV, FaseInvio.AGGIORNAMENTO_REPORT, FaseInvio.REGISTRO
        ),
        faseCorrente = FaseInvio.SOSTITUZIONE_PDF
    )

    val aggiornata = dettaglio.copy(
        giornaliera = g4.copy(revisione = 2, data = Campioni.giorno(2)),
        selectedDate = Campioni.giorno(2),
        modifiche = storico + GiornalieraModifica(2, 4, 2, Campioni.OGGI, "Data sbagliata: il lavoro era di lunedì 2", Campioni.giorno(4), Campioni.giorno(2)),
        dialog = WizardDialog.Aggiornata(
            EsitoAggiornamento(
                revisione = 2, fileName = "0001234_2026-11-02.pdf", archivio = ArchivioFile.DRIVE,
                esitoPdf = EsitoSostituzione.RINOMINATO, nomePrecedente = "0001234_2026-11-04.pdf", webViewLink = "https://drive.google.com/x",
                csvErrore = null, reportAggiornati = listOf("Riepilogo_0001234_completo.pdf"), reportFalliti = emptyList(),
                registro = "Google Drive › Modifiche", registroErrore = null
            )
        )
    )
}
