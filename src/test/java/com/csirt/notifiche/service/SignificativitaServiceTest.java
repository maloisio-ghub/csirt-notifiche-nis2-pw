package com.csirt.notifiche.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.csirt.notifiche.dto.ImpattoRichiesto;
import com.csirt.notifiche.dto.NuovoIncidente;
import com.csirt.notifiche.dto.RichiestaValutazione;
import com.csirt.notifiche.dto.ValutazioneDto;
import com.csirt.notifiche.model.Incidente;
import com.csirt.notifiche.model.StatoIncidente;

@SpringBootTest
@Transactional
class SignificativitaServiceTest {

    @Autowired
    private IncidenteService incidenti;

    @Autowired
    private SignificativitaService significativita;

    private Long nuovoIncidente() {
        return incidenti.registra(new NuovoIncidente("Prova", null, Instant.parse("2026-10-16T08:00:00Z"))).getId();
    }

    private static RichiestaValutazione indisponibilita(String servizio, int minuti) {
        return new RichiestaValutazione(null, null, null, null, null,
                List.of(new ImpattoRichiesto(servizio, minuti, null, null, null, null)));
    }

    @Test
    @DisplayName("Esito positivo: incidente SIGNIFICATIVO e momento di conoscenza fissato")
    void esitoPositivo() {
        Long id = nuovoIncidente();

        ValutazioneDto valutazione = significativita.valuta(id, indisponibilita("S1", 31));

        assertTrue(valutazione.significativo());
        assertEquals(List.of("R7"), valutazione.regole());
        assertEquals(1, valutazione.versioneParametri());
        assertEquals(12_000, valutazione.impatti().get(0).baseUtenti());
        Incidente incidente = incidenti.trova(id);
        assertEquals(StatoIncidente.SIGNIFICATIVO, incidente.getStato());
        assertEquals(valutazione.dataValutazione(), incidente.getDataConoscenza());
    }

    @Test
    @DisplayName("Esito negativo, poi nuova valutazione positiva: lo storico conserva entrambe")
    void rivalutazione() {
        Long id = nuovoIncidente();

        assertFalse(significativita.valuta(id, indisponibilita("S1", 30)).significativo());
        assertEquals(StatoIncidente.NON_SIGNIFICATIVO, incidenti.trova(id).getStato());
        assertNull(incidenti.trova(id).getDataConoscenza());

        assertTrue(significativita.valuta(id, indisponibilita("S1", 45)).significativo());
        assertEquals(StatoIncidente.SIGNIFICATIVO, incidenti.trova(id).getStato());
        assertEquals(2, significativita.storico(id).size());
    }

    @Test
    @DisplayName("Un incidente già significativo non può essere rivalutato")
    void significativoNonRivalutabile() {
        Long id = nuovoIncidente();
        significativita.valuta(id, indisponibilita("S1", 31));

        assertThrows(OperazioneNonConsentitaException.class,
                () -> significativita.valuta(id, indisponibilita("S1", 10)));
    }

    @Test
    @DisplayName("Servizio critico inesistente: richiesta rifiutata")
    void servizioSconosciuto() {
        Long id = nuovoIncidente();

        assertThrows(RichiestaNonValidaException.class, () -> significativita.valuta(id, indisponibilita("S9", 31)));
    }
}