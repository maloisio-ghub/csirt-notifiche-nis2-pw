package com.csirt.notifiche.documenti;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.csirt.notifiche.model.ContenutoNotifica;
import com.csirt.notifiche.model.GravitaIncidente;
import com.csirt.notifiche.model.TipoDocumento;

class RegoleDocumentoTest {

    private static ContenutoNotifica completo() {
        return new ContenutoNotifica("Mario Rossi", "soc@alfa.example", "Console SD-WAN irraggiungibile", false,
                false, GravitaIncidente.ALTA, "Clienti senza gestione della rete per 31 minuti", null,
                "Descrizione dettagliata", "Aggiornamento firmware difettoso", "Rollback del firmware");
    }

    @Test
    @DisplayName("La matrice è ricavata dal codice: 11 campi più la classificazione")
    void matriceDalCodice() {
        List<RegoleDocumento.Campo> matrice = RegoleDocumento.matrice();
        assertEquals(12, matrice.size());
        for (int i = 0; i < matrice.size(); i++) {
            assertEquals(i + 1, matrice.get(i).ordine());
        }
    }

    @Test
    @DisplayName("Contenuto vuoto: campi mancanti diversi per ciascun adempimento")
    void campiMancantiPerAdempimento() {
        ContenutoNotifica vuoto = new ContenutoNotifica();

        assertEquals(List.of("Referente - nome", "Referente - e-mail", "Descrizione sintetica",
                "Sospetto di atto illegittimo o malevolo", "Impatto transfrontaliero"),
                RegoleDocumento.campiMancanti(vuoto, TipoDocumento.PRE_NOTIFICA));
        assertEquals(List.of("Referente - nome", "Referente - e-mail", "Descrizione sintetica",
                "Sospetto di atto illegittimo o malevolo", "Impatto transfrontaliero", "Gravità", "Impatto"),
                RegoleDocumento.campiMancanti(vuoto, TipoDocumento.NOTIFICA));
        assertEquals(List.of("Referente - nome", "Referente - e-mail", "Descrizione sintetica",
                "Impatto transfrontaliero", "Gravità", "Impatto", "Descrizione dettagliata",
                "Tipo di minaccia o causa principale", "Misure di attenuazione"),
                RegoleDocumento.campiMancanti(vuoto, TipoDocumento.RELAZIONE_FINALE));
    }

    @Test
    @DisplayName("Contenuto completo: nessun campo mancante per i tre adempimenti")
    void contenutoCompleto() {
        for (TipoDocumento tipo : TipoDocumento.values()) {
            assertTrue(RegoleDocumento.campiMancanti(completo(), tipo).isEmpty(), tipo.toString());
        }
    }

    @Test
    @DisplayName("Un testo di soli spazi conta come mancante")
    void spaziComeVuoto() {
        ContenutoNotifica c = new ContenutoNotifica("Mario Rossi", "soc@alfa.example", "   ", false, false,
                null, null, null, null, null, null);
        assertEquals(List.of("Descrizione sintetica"), RegoleDocumento.campiMancanti(c, TipoDocumento.PRE_NOTIFICA));
    }

    @Test
    @DisplayName("La matrice documentata in docs/matrice-campi.md coincide con quella generata dal codice")
    void matriceDocumentataUgualeAlCodice() throws IOException {
        String documentata = Files.readString(Path.of("docs", "matrice-campi.md"), StandardCharsets.UTF_8);
        assertEquals(normalizza(RegoleDocumento.matriceMarkdown()), normalizza(documentata),
                "docs/matrice-campi.md non è aggiornato: rigeneralo da /api/documenti/matrice.md");
    }

    private static String normalizza(String testo) {
        return testo.replace("\r\n", "\n").strip();
    }
}