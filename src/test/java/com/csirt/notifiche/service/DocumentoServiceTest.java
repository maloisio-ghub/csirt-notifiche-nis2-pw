package com.csirt.notifiche.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.csirt.notifiche.dto.ContenutoDto;
import com.csirt.notifiche.dto.DocumentoDto;
import com.csirt.notifiche.dto.ImpattoRichiesto;
import com.csirt.notifiche.dto.NuovoIncidente;
import com.csirt.notifiche.dto.RichiestaValutazione;
import com.csirt.notifiche.model.StatoDocumento;
import com.csirt.notifiche.model.TipoDocumento;

@SpringBootTest
@Transactional
class DocumentoServiceTest {

    @Autowired
    private IncidenteService incidenti;

    @Autowired
    private SignificativitaService significativita;

    @Autowired
    private DocumentoService documenti;

    private Long incidente() {
        return incidenti.registra(new NuovoIncidente("Console SD-WAN irraggiungibile",
                "Il portale di gestione non risponde", Instant.parse("2026-10-17T08:00:00Z"))).getId();
    }

    private Long incidenteSignificativo() {
        Long id = incidente();
        significativita.valuta(id, new RichiestaValutazione(null, null, null, null, null,
                List.of(new ImpattoRichiesto("S1", 31, null, null, null, null))));
        return id;
    }

    @Test
    @DisplayName("La prima bozza è precompilata con titolo e descrizione dell'incidente")
    void primaBozzaPrecompilata() {
        DocumentoDto pre = documenti.crea(incidenteSignificativo(), TipoDocumento.PRE_NOTIFICA);

        assertEquals(1, pre.versione());
        assertEquals(StatoDocumento.BOZZA, pre.stato());
        assertEquals("Console SD-WAN irraggiungibile - Il portale di gestione non risponde",
                pre.contenuto().descrizioneSintetica());
    }

    @Test
    @DisplayName("La notifica riusa i dati salvati nella pre-notifica")
    void riusoDeiDati() {
        Long id = incidenteSignificativo();
        DocumentoDto pre = documenti.crea(id, TipoDocumento.PRE_NOTIFICA);
        documenti.aggiorna(pre.id(), new ContenutoDto("Mario Rossi", "soc@alfa.example", "Console SD-WAN giù",
                true, false, null, null, null, null, null, null));

        DocumentoDto notifica = documenti.crea(id, TipoDocumento.NOTIFICA);

        assertEquals("Mario Rossi", notifica.contenuto().referenteNome());
        assertEquals("Console SD-WAN giù", notifica.contenuto().descrizioneSintetica());
        assertTrue(notifica.contenuto().sospettoMalevolo());
        assertEquals(2, documenti.elenco(id).size());
    }

    @Test
    @DisplayName("Soglia: 30 minuti (appena sotto) non attiva il flusso di notifica")
    void sottoSogliaNessunaNotifica() {
        Long id = incidente();
        significativita.valuta(id, new RichiestaValutazione(null, null, null, null, null,
                List.of(new ImpattoRichiesto("S1", 30, null, null, null, null))));

        assertThrows(OperazioneNonConsentitaException.class, () -> documenti.crea(id, TipoDocumento.PRE_NOTIFICA));
    }

    @Test
    @DisplayName("Soglia: 31 minuti (appena sopra) attiva il flusso di notifica")
    void sopraSogliaPreNotificaDisponibile() {
        Long id = incidenteSignificativo();

        DocumentoDto bozza = documenti.crea(id, TipoDocumento.PRE_NOTIFICA);

        assertEquals(StatoDocumento.BOZZA, bozza.stato());
    }

    @Test
    @DisplayName("Non si apre una seconda bozza dello stesso tipo")
    void bozzaDuplicata() {
        Long id = incidenteSignificativo();
        documenti.crea(id, TipoDocumento.PRE_NOTIFICA);

        assertThrows(OperazioneNonConsentitaException.class, () -> documenti.crea(id, TipoDocumento.PRE_NOTIFICA));
    }
}