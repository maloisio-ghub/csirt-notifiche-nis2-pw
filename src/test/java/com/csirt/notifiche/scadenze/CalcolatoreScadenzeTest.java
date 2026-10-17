package com.csirt.notifiche.scadenze;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.csirt.notifiche.model.TipoDocumento;

class CalcolatoreScadenzeTest {

    private static final Instant CONOSCENZA = Instant.parse("2026-10-16T10:00:00Z");

    private final CalcolatoreScadenze calcolatore = new CalcolatoreScadenze();

    private Scadenza preNotifica(Instant adesso) {
        return calcolatore.calcola(CONOSCENZA, null, adesso).get(0);
    }

    @Test
    @DisplayName("Incidente non significativo: nessun termine attivo")
    void nonSignificativo() {
        List<Scadenza> scadenze = calcolatore.calcola(null, null, CONOSCENZA);
        assertEquals(3, scadenze.size());
        assertTrue(scadenze.stream().allMatch(s -> s.stato() == StatoScadenza.NON_ATTIVA));
    }

    @Test
    @DisplayName("24 e 72 ore dalla conoscenza; relazione finale in attesa della notifica")
    void terminiDallaConoscenza() {
        List<Scadenza> scadenze = calcolatore.calcola(CONOSCENZA, null, CONOSCENZA);

        assertEquals(TipoDocumento.PRE_NOTIFICA, scadenze.get(0).tipo());
        assertEquals(Instant.parse("2026-10-17T10:00:00Z"), scadenze.get(0).termine());
        assertEquals(Instant.parse("2026-10-19T10:00:00Z"), scadenze.get(1).termine());
        assertEquals(StatoScadenza.IN_ATTESA, scadenze.get(2).stato());
        assertNull(scadenze.get(2).termine());
    }

    @Test
    @DisplayName("Pre-notifica: aperta a 23h59 e a 24h esatte, scaduta a 24h01")
    void confineDelleVentiquattroOre() {
        Scadenza unMinutoPrima = preNotifica(CONOSCENZA.plus(Duration.ofHours(24).minusMinutes(1)));
        assertEquals(StatoScadenza.IN_CORSO, unMinutoPrima.stato());
        assertEquals(1L, unMinutoPrima.minutiResidui());

        assertEquals(StatoScadenza.IN_CORSO, preNotifica(CONOSCENZA.plus(Duration.ofHours(24))).stato());

        Scadenza unMinutoDopo = preNotifica(CONOSCENZA.plus(Duration.ofHours(24).plusMinutes(1)));
        assertEquals(StatoScadenza.SCADUTA, unMinutoDopo.stato());
        assertEquals(-1L, unMinutoDopo.minutiResidui());
    }

    @Test
    @DisplayName("Relazione finale: un mese di calendario in ora italiana, anche col cambio dell'ora legale")
    void meseDiCalendarioConOraLegale() {
        // 20/10/2026 10:00 ora legale (UTC+2) -> 20/11/2026 10:00 ora solare (UTC+1)
        Instant trasmissioneNotifica = Instant.parse("2026-10-20T08:00:00Z");

        Scadenza relazione = calcolatore.calcola(CONOSCENZA, trasmissioneNotifica, CONOSCENZA).get(2);

        assertEquals(StatoScadenza.IN_CORSO, relazione.stato());
        assertEquals(Instant.parse("2026-11-20T09:00:00Z"), relazione.termine());
    }

    @Test
    @DisplayName("Relazione finale: dal 31 gennaio si arriva all'ultimo giorno di febbraio")
    void fineMese() {
        Instant trasmissioneNotifica = Instant.parse("2027-01-31T10:00:00Z");

        Scadenza relazione = calcolatore.calcola(CONOSCENZA, trasmissioneNotifica, CONOSCENZA).get(2);

        assertEquals(Instant.parse("2027-02-28T10:00:00Z"), relazione.termine());
    }
}