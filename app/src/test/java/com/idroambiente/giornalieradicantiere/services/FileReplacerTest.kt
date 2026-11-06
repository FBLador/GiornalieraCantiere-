package com.idroambiente.giornalieradicantiere.services

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Archivio finto in memoria: nome file per ID, più file "protetti" (altro account). */
private class FakeArchivio(
    val files: MutableMap<String, String> = mutableMapOf(),
    val protetti: Set<String> = emptySet(),
    var reteGiu: Boolean = false
) : RemoteOps {
    var creati = 0
    var aggiornati = 0
    private var next = 100

    override suspend fun update(ref: String, newName: String): OpResult {
        if (reteGiu) return OpResult.Failed("Errore di rete")
        if (ref !in files) return OpResult.NotFound
        if (ref in protetti) return OpResult.Forbidden
        files[ref] = newName
        aggiornati++
        return OpResult.Ok(ref, newName)
    }

    override suspend fun findByName(name: String): FindResult =
        if (reteGiu) FindResult.Failed("Errore di rete")
        else FindResult.Found(files.filterValues { it == name }.keys.toList())

    override suspend fun create(name: String): OpResult {
        if (reteGiu) return OpResult.Failed("Errore di rete")
        val id = "id${next++}"
        files[id] = name
        creati++
        return OpResult.Ok(id, name)
    }
}

class FileReplacerTest {

    @Test
    fun fileConosciuto_vieneSostituitoSenzaDoppioni() = runBlocking {
        val drive = FakeArchivio(mutableMapOf("A" to "0001234_2026-11-04.pdf"))
        val r = FileReplacer.replace("A", "0001234_2026-11-04.pdf", "0001234_2026-11-04.pdf", "x_rev1.pdf", drive)
        r as ReplaceResult.Done
        assertEquals(EsitoSostituzione.SOSTITUITO, r.esito)
        assertEquals("A", r.ref)
        assertEquals(1, drive.files.size)
        assertEquals(0, drive.creati)
    }

    @Test
    fun dataCambiata_stessoFileRinominato() = runBlocking {
        val drive = FakeArchivio(mutableMapOf("A" to "0001234_2026-11-04.pdf"))
        val r = FileReplacer.replace("A", "0001234_2026-11-04.pdf", "0001234_2026-11-02.pdf", "x_rev1.pdf", drive)
        r as ReplaceResult.Done
        assertEquals(EsitoSostituzione.RINOMINATO, r.esito)
        assertEquals("0001234_2026-11-04.pdf", r.previousName)
        assertEquals(mapOf("A" to "0001234_2026-11-02.pdf"), drive.files)
    }

    @Test
    fun giornalieraVecchiaSenzaId_vieneTrovataPerNome() = runBlocking {
        val drive = FakeArchivio(mutableMapOf("B" to "0001234_2026-11-04.pdf", "C" to "altro.pdf"))
        val r = FileReplacer.replace(null, "0001234_2026-11-04.pdf", "0001234_2026-11-04.pdf", "x_rev1.pdf", drive)
        r as ReplaceResult.Done
        assertEquals("B", r.ref)
        assertEquals(0, drive.creati)
        assertEquals(2, drive.files.size)
    }

    @Test
    fun fileCancellatoAMano_vieneRicreato() = runBlocking {
        val drive = FakeArchivio()
        val r = FileReplacer.replace("A", "0001234_2026-11-04.pdf", "0001234_2026-11-04.pdf", "x_rev1.pdf", drive)
        r as ReplaceResult.Done
        assertEquals(EsitoSostituzione.CREATO_NON_TROVATO, r.esito)
        assertEquals("0001234_2026-11-04.pdf", r.name)
        assertEquals(1, drive.creati)
    }

    @Test
    fun fileDiUnAltroAccount_creaNuovoConRevisione() = runBlocking {
        val drive = FakeArchivio(mutableMapOf("A" to "0001234_2026-11-04.pdf"), protetti = setOf("A"))
        val r = FileReplacer.replace("A", "0001234_2026-11-04.pdf", "0001234_2026-11-04.pdf", "0001234_2026-11-04_rev2.pdf", drive)
        r as ReplaceResult.Done
        assertEquals(EsitoSostituzione.CREATO_SENZA_PERMESSI, r.esito)
        assertEquals("0001234_2026-11-04_rev2.pdf", r.name)
        // il file originale non viene toccato
        assertEquals("0001234_2026-11-04.pdf", drive.files["A"])
    }

    @Test
    fun erroreDiRete_nonCreaDoppioni() = runBlocking {
        val drive = FakeArchivio(mutableMapOf("A" to "0001234_2026-11-04.pdf"), reteGiu = true)
        val r = FileReplacer.replace("A", "0001234_2026-11-04.pdf", "0001234_2026-11-02.pdf", "x_rev1.pdf", drive)
        assertTrue(r is ReplaceResult.Failed)
        assertEquals(0, drive.creati)
        assertEquals("0001234_2026-11-04.pdf", drive.files["A"])
    }

    @Test
    fun rinominaGiaFattaInUnTentativoPrecedente_vieneTrovataColNomeNuovo() = runBlocking {
        val drive = FakeArchivio(mutableMapOf("A" to "0001234_2026-11-02.pdf"))
        val r = FileReplacer.replace(null, "0001234_2026-11-04.pdf", "0001234_2026-11-02.pdf", "x_rev1.pdf", drive)
        r as ReplaceResult.Done
        assertEquals("A", r.ref)
        assertEquals(0, drive.creati)
    }
}
