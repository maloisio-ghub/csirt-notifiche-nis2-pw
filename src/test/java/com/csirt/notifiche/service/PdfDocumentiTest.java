package com.csirt.notifiche.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.csirt.notifiche.dto.ContenutoDto;
import com.csirt.notifiche.dto.DocumentoDto;
import com.csirt.notifiche.dto.FileScaricabile;
import com.csirt.notifiche.dto.ImpattoRichiesto;
import com.csirt.notifiche.dto.NuovoIncidente;
import com.csirt.notifiche.dto.RichiestaClassificazione;
import com.csirt.notifiche.dto.RichiestaValutazione;
import com.csirt.notifiche.model.TipoDocumento;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;

@SpringBootTest
@Transactional
class PdfDocumentiTest {

    private static final ContenutoDto PRE = new ContenutoDto("Mario Rossi", "soc@alfa.example",
            "Console SD-WAN irraggiungibile", false, false, null, null, null, null, null, null);

    @Autowired
    private IncidenteService incidenti;

    @Autowired
    private SignificativitaService significativita;

    @Autowired
    private ClassificazioneService classificazioni;

    @Autowired
    private DocumentoService documenti;

    private Long incidenteSignificativo() {
        Long id = incidenti.registra(new NuovoIncidente("Console SD-WAN irraggiungibile", null,
                Instant.parse("2026-10-17T08:00:00Z"))).getId();
        significativita.valuta(id, new RichiestaValutazione(null, null, null, null, null,
                List.of(new ImpattoRichiesto("S1", 31, null, null, null, null))));
        classificazioni.classifica(id, new RichiestaClassificazione(List.of("BC:IM_AV", "TT:AV_OU"), null));
        return id;
    }

    private static String testo(byte[] pdf) throws IOException {
        PdfReader lettore = new PdfReader(pdf);
        try {
            return new PdfTextExtractor(lettore).getTextFromPage(1);
        } finally {
            lettore.close();
        }
    }

    @Test
    @DisplayName("PDF di un documento trasmesso: nome del file, formato PDF e stato")
    void pdfDocumentoTrasmesso() throws IOException {
        Long id = incidenteSignificativo();
        DocumentoDto bozza = documenti.crea(id, TipoDocumento.PRE_NOTIFICA);
        documenti.aggiorna(bozza.id(), PRE);
        DocumentoDto pre = documenti.trasmetti(bozza.id());

        FileScaricabile file = documenti.pdf(pre.id());

        assertEquals("pre-notifica-incidente-" + id + "-v1.pdf", file.nome());
        assertEquals("%PDF-", new String(file.contenuto(), 0, 5, StandardCharsets.US_ASCII));
        String testo = testo(file.contenuto());
        assertTrue(testo.contains("CSIRT Italia"));
        assertTrue(testo.contains("TRASMESSO"));
    }

    @Test
    @DisplayName("PDF di una bozza: generato e marcato come non trasmesso")
    void pdfBozza() throws IOException {
        DocumentoDto bozza = documenti.crea(incidenteSignificativo(), TipoDocumento.PRE_NOTIFICA);

        String testo = testo(documenti.pdf(bozza.id()).contenuto());

        assertTrue(testo.contains("BOZZA"));
    }
}