package com.csirt.notifiche.tassonomia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.csirt.notifiche.model.TipoVoce;

class DefinizioneTassonomiaTest {

    private static final String INTESTAZIONE = "tipo;codice;nome;padre;multiplo;gruppo;descrizione\n";

    private static DefinizioneTassonomia definizione(String righe) throws IOException {
        return DefinizioneTassonomia.da(LettoreCsv.leggi(new StringReader(INTESTAZIONE + righe)));
    }

    @Test
    @DisplayName("Definizione valida, con un campo tra virgolette che contiene ; e virgolette")
    void definizioneValida() throws IOException {
        DefinizioneTassonomia d = definizione("MACROCATEGORIA;BC;Baseline Characterization;;N;;\n"
                + "PREDICATO;BC:IM;Impact;BC;S;;\"Testo con ; e \"\"virgolette\"\"\"\n"
                + "VALORE;BC:IM_AV;Availability;BC:IM;N;;\n");

        assertEquals(Map.of(TipoVoce.MACROCATEGORIA, 1L, TipoVoce.PREDICATO, 1L, TipoVoce.VALORE, 1L), d.conteggi());
        assertEquals("Testo con ; e \"virgolette\"", d.righe().get(1).descrizione());
    }

    @Test
    @DisplayName("I codici anomali del documento ufficiale (TT:VU_0-) sono accettati così come sono")
    void codiceUfficialeConTrattino() throws IOException {
        DefinizioneTassonomia d = definizione("MACROCATEGORIA;TT;Threat Type;;N;;\n"
                + "PREDICATO;TT:VU;Vulnerability;TT;S;;\n"
                + "VALORE;TT:VU_0-;0-Day Vulnerability;TT:VU;N;;\n");

        assertEquals("TT:VU_0-", d.righe().get(2).codice());
    }

    @Test
    @DisplayName("Codice duplicato: definizione rifiutata")
    void codiceDuplicato() {
        assertThrows(TassonomiaNonValidaException.class, () -> definizione(
                "MACROCATEGORIA;BC;Baseline Characterization;;N;;\n"
                        + "MACROCATEGORIA;BC;Duplicato;;N;;\n"));
    }

    @Test
    @DisplayName("Valore con codice di un altro predicato: definizione rifiutata")
    void valoreNelPredicatoSbagliato() {
        assertThrows(TassonomiaNonValidaException.class, () -> definizione(
                "MACROCATEGORIA;BC;Baseline Characterization;;N;;\n"
                        + "PREDICATO;BC:IM;Impact;BC;S;;\n"
                        + "VALORE;BC:RO_AV;Availability;BC:IM;N;;\n"));
    }

    @Test
    @DisplayName("Predicato senza la sua macrocategoria: definizione rifiutata")
    void predicatoOrfano() {
        assertThrows(TassonomiaNonValidaException.class, () -> definizione(
                "PREDICATO;TT:VU;Vulnerability;TT;S;;\n"));
    }
}