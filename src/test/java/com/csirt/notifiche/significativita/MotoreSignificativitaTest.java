package com.csirt.notifiche.significativita;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MotoreSignificativitaTest {

    private static final long BASE_S1 = 12_000;

    private final MotoreSignificativita motore = new MotoreSignificativita();
    private final ParametriSoglia parametri = ParametriSoglia.alfaManagedServices();

    private EsitoValutazione valuta(BigDecimal perdita, ImpattoServizio... impatti) {
        return motore.valuta(new DatiValutazione(perdita, false, false, false, false, List.of(impatti)), parametri);
    }

    private static ImpattoServizio indisponibilitaTotale(int minuti) {
        return new ImpattoServizio("S1", BASE_S1, minuti, 0, 0, false, 0);
    }

    private static ImpattoServizio disponibilitaLimitata(long utenti, int minuti) {
        return new ImpattoServizio("S1", BASE_S1, 0, minuti, utenti, false, 0);
    }

    @Test
    @DisplayName("Soglie calcolate per Alfa Managed Services")
    void soglie() {
        assertEquals(0, new BigDecimal("500000").compareTo(parametri.sogliaPerdita()));
        assertEquals(600, parametri.sogliaUtenti(12_000));
        assertEquals(400, parametri.sogliaUtenti(8_000));
        assertEquals(250, parametri.sogliaUtenti(5_000));
    }

    @Test
    @DisplayName("R7: 31 minuti di indisponibilità totale (appena sopra soglia) è significativo")
    void indisponibilitaAppenaSopra() {
        EsitoValutazione esito = valuta(BigDecimal.ZERO, indisponibilitaTotale(31));
        assertTrue(esito.significativo());
        assertEquals(List.of("R7"), esito.codici());
    }

    @Test
    @DisplayName("R7: 30 minuti di indisponibilità totale (sulla soglia) non è significativo")
    void indisponibilitaSullaSoglia() {
        assertFalse(valuta(BigDecimal.ZERO, indisponibilitaTotale(30)).significativo());
    }

    @Test
    @DisplayName("R8: 601 utenti per 61 minuti è significativo")
    void disponibilitaLimitataSopra() {
        assertEquals(List.of("R8"), valuta(BigDecimal.ZERO, disponibilitaLimitata(601, 61)).codici());
    }

    @Test
    @DisplayName("R8: 600 utenti (sulla soglia) non è significativo")
    void disponibilitaLimitataUtentiSullaSoglia() {
        assertFalse(valuta(BigDecimal.ZERO, disponibilitaLimitata(600, 61)).significativo());
    }

    @Test
    @DisplayName("R8: 60 minuti (sulla soglia) non è significativo")
    void disponibilitaLimitataDurataSullaSoglia() {
        assertFalse(valuta(BigDecimal.ZERO, disponibilitaLimitata(601, 60)).significativo());
    }

    @Test
    @DisplayName("R1: perdita di 500.000,01 € è significativa, 500.000 € no")
    void perditaEconomica() {
        assertEquals(List.of("R1"), valuta(new BigDecimal("500000.01")).codici());
        assertFalse(valuta(new BigDecimal("500000")).significativo());
    }

    @Test
    @DisplayName("Nessun criterio soddisfatto: non significativo")
    void nessunCriterio() {
        assertFalse(valuta(BigDecimal.ZERO).significativo());
    }
}
