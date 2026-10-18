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

import com.csirt.notifiche.dto.ClassificazioneDto;
import com.csirt.notifiche.dto.ContenutoDto;
import com.csirt.notifiche.dto.DocumentoDto;
import com.csirt.notifiche.dto.ImpattoRichiesto;
import com.csirt.notifiche.dto.NuovoIncidente;
import com.csirt.notifiche.dto.RichiestaClassificazione;
import com.csirt.notifiche.dto.RichiestaValutazione;
import com.csirt.notifiche.model.GravitaIncidente;
import com.csirt.notifiche.model.TipoDocumento;

@SpringBootTest
@Transactional
class ClassificazioneServiceTest {

    @Autowired
    private IncidenteService incidenti;

    @Autowired
    private SignificativitaService significativita;

    @Autowired
    private ClassificazioneService classificazioni;

    @Autowired
    private DocumentoService documenti;

    private Long incidente() {
        return incidenti.registra(new NuovoIncidente("Console SD-WAN irraggiungibile", null,
                Instant.parse("2026-10-17T08:00:00Z"))).getId();
    }

    private Long incidenteSignificativo() {
        Long id = incidente();
        significativita.valuta(id, new RichiestaValutazione(null, null, null, null, null,
                List.of(new ImpattoRichiesto("S1", 31, null, null, null, null))));
        return id;
    }

    private static RichiestaClassificazione codici(String motivazione, String... codici) {
        return new RichiestaClassificazione(List.of(codici), motivazione);
    }

    @Test
    @DisplayName("Prima classificazione: versione 1 attiva, valori della tassonomia con il loro predicato")
    void primaClassificazione() {
        ClassificazioneDto c = classificazioni.classifica(incidenteSignificativo(), codici(null, "BC:IM_AV", "TT:AV_OU"));

        assertEquals(1, c.versione());
        assertTrue(c.attiva());
        assertEquals("TC-ACN-2.0", c.versioneTassonomia());
        assertEquals("BC:IM", c.valori().get(0).predicato());
    }

    @Test
    @DisplayName("Codice inesistente nella tassonomia: rifiutato")
    void codiceInesistente() {
        Long id = incidenteSignificativo();
        assertThrows(RichiestaNonValidaException.class, () -> classificazioni.classifica(id, codici(null, "BC:IM_XX")));
    }

    @Test
    @DisplayName("Un predicato non è un valore assegnabile: rifiutato")
    void predicatoNonAssegnabile() {
        Long id = incidenteSignificativo();
        assertThrows(RichiestaNonValidaException.class, () -> classificazioni.classifica(id, codici(null, "BC:IM")));
    }

    @Test
    @DisplayName("Due valori di Severity (selezione singola): rifiutato")
    void selezioneSingola() {
        Long id = incidenteSignificativo();
        assertThrows(RichiestaNonValidaException.class,
                () -> classificazioni.classifica(id, codici(null, "BC:SE_HI", "BC:SE_LO")));
    }

    @Test
    @DisplayName("Riclassificazione senza motivazione: rifiutata; con motivazione: versione 2, la 1 non è più attiva")
    void riclassificazioneConMotivazione() {
        Long id = incidenteSignificativo();
        classificazioni.classifica(id, codici(null, "BC:IM_AV", "BC:RO_SY"));

        assertThrows(RichiestaNonValidaException.class,
                () -> classificazioni.classifica(id, codici(null, "BC:IM_AV", "BC:RO_MA")));

        ClassificazioneDto v2 = classificazioni.classifica(id, codici("Analisi forense: azione malevola",
                "BC:IM_AV", "BC:RO_MA"));
        assertEquals(2, v2.versione());
        assertEquals(1, v2.versionePrecedente());
        List<ClassificazioneDto> storico = classificazioni.storico(id);
        assertEquals(2, storico.size());
        assertTrue(!storico.get(0).attiva() && storico.get(1).attiva());
    }

    @Test
    @DisplayName("Notifica senza classificazione: rifiutata con 'Classificazione TC-ACN' tra i campi mancanti")
    void notificaSenzaClassificazione() {
        Long id = incidenteSignificativo();
        ContenutoDto completo = new ContenutoDto("Mario Rossi", "soc@alfa.example", "Console SD-WAN irraggiungibile",
                false, false, GravitaIncidente.MEDIA, "Clienti senza gestione della rete", null, null, null, null);
        DocumentoDto pre = documenti.crea(id, TipoDocumento.PRE_NOTIFICA);
        documenti.aggiorna(pre.id(), completo);
        documenti.trasmetti(pre.id());
        DocumentoDto notifica = documenti.crea(id, TipoDocumento.NOTIFICA);

        DocumentoIncompletoException e = assertThrows(DocumentoIncompletoException.class,
                () -> documenti.trasmetti(notifica.id()));
        assertEquals(List.of("Classificazione TC-ACN"), e.getCampiMancanti());
    }

    @Test
    @DisplayName("Un incidente non significativo non si classifica")
    void incidenteNonSignificativo() {
        Long id = incidente();
        assertThrows(OperazioneNonConsentitaException.class, () -> classificazioni.classifica(id, codici(null, "BC:IM_AV")));
    }
}