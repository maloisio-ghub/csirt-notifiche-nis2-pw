package com.csirt.notifiche.integrazione;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.csirt.notifiche.documenti.Riclassificazioni.Riclassificazione;
import com.csirt.notifiche.dto.ClassificazioneDto;
import com.csirt.notifiche.dto.ContenutoDto;
import com.csirt.notifiche.dto.DocumentoDto;
import com.csirt.notifiche.dto.FotoDocumento;
import com.csirt.notifiche.dto.ImpattoRichiesto;
import com.csirt.notifiche.dto.NuovoIncidente;
import com.csirt.notifiche.dto.RichiestaClassificazione;
import com.csirt.notifiche.dto.RichiestaValutazione;
import com.csirt.notifiche.dto.TracciaDto;
import com.csirt.notifiche.dto.ValoreClassificato;
import com.csirt.notifiche.model.GravitaIncidente;
import com.csirt.notifiche.model.StatoIncidente;
import com.csirt.notifiche.model.TipoDocumento;
import com.csirt.notifiche.scadenze.StatoScadenza;
import com.csirt.notifiche.service.ClassificazioneService;
import com.csirt.notifiche.service.DocumentoService;
import com.csirt.notifiche.service.IncidenteService;
import com.csirt.notifiche.service.SignificativitaService;

/**
 * Requisito 3 della traccia: riclassificazione dopo la notifica delle 72 ore. Percorre lo scenario
 * completo, verifica che i documenti trasmessi restino immutati e scrive la traccia di esecuzione
 * in target/traccia-riclassificazione.md.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:scenario")
class ScenarioRiclassificazioneTardivaTest {

    private static final String MOTIVAZIONE = "Analisi forense del 21/10: l'indisponibilità è stata causata dallo "
            + "sfruttamento di una vulnerabilità nota (n-day) della console di orchestrazione SD-WAN, "
            + "non da un guasto. Gravità rivalutata ad alta.";
    private static final DateTimeFormatter ORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.of("Europe/Rome"));

    @Autowired
    private IncidenteService incidenti;

    @Autowired
    private SignificativitaService significativita;

    @Autowired
    private ClassificazioneService classificazioni;

    @Autowired
    private DocumentoService documenti;

    private static ContenutoDto contenuto(GravitaIncidente gravita, String impatto, String dettaglio, String causa,
                                          String misure) {
        return new ContenutoDto("Mario Rossi", "soc@alfa.example",
                "Console di orchestrazione SD-WAN irraggiungibile per i clienti del servizio S1", false, false,
                gravita, impatto, null, dettaglio, causa, misure);
    }

    private DocumentoDto trasmetti(Long incidenteId, TipoDocumento tipo, ContenutoDto contenuto) {
        DocumentoDto bozza = documenti.crea(incidenteId, tipo);
        documenti.aggiorna(bozza.id(), contenuto);
        return documenti.trasmetti(bozza.id());
    }

    @Test
    @DisplayName("Riclassificazione dopo la notifica delle 72 ore: documenti trasmessi immutati, relazione finale con il cambiamento")
    void riclassificazioneTardiva() throws IOException {
        Long id = incidenti.registra(new NuovoIncidente("Console SD-WAN irraggiungibile",
                "La console di orchestrazione SD-WAN non risponde: i clienti non possono gestire le proprie reti",
                Instant.now().minus(Duration.ofMinutes(15)))).getId();

        significativita.valuta(id, new RichiestaValutazione(null, null, null, null, null,
                List.of(new ImpattoRichiesto("S1", 95, null, null, null, null))));

        classificazioni.classifica(id, new RichiestaClassificazione(
                List.of("BC:IM_AV", "BC:RO_SY", "BC:SE_ME", "TT:AV_OU"), null));

        trasmetti(id, TipoDocumento.PRE_NOTIFICA, contenuto(null, null, null, null, null));
        trasmetti(id, TipoDocumento.NOTIFICA, contenuto(GravitaIncidente.MEDIA,
                "Console SD-WAN indisponibile per 95 minuti per i clienti del servizio S1", null, null, null));

        TracciaDto prima = documenti.traccia(id);

        classificazioni.classifica(id, new RichiestaClassificazione(
                List.of("BC:IM_AV", "BC:IM_SY", "BC:RO_MA", "BC:SE_HI", "TT:VU_N-", "AC:VE_EV"), MOTIVAZIONE));

        TracciaDto dopo = documenti.traccia(id);

        assertEquals(prima.documenti(), dopo.documenti());
        assertTrue(dopo.documenti().stream().allMatch(d -> d.versioneClassificazione() == 1));
        assertEquals(1, prima.classificazioneAttiva());
        assertEquals(2, dopo.classificazioneAttiva());

        DocumentoDto bozza = documenti.crea(id, TipoDocumento.RELAZIONE_FINALE);
        documenti.aggiorna(bozza.id(), contenuto(GravitaIncidente.ALTA,
                "Console SD-WAN indisponibile per 95 minuti; nessuna esfiltrazione di dati rilevata",
                "Un attaccante ha sfruttato una vulnerabilità nota della console di orchestrazione SD-WAN, "
                        + "causandone il blocco. Il ripristino è avvenuto dal backup della configurazione.",
                "Sfruttamento di una vulnerabilità n-day non ancora corretta",
                "Applicazione della patch del fornitore, restrizione dell'accesso alla console, nuove regole di rilevamento"));
        DocumentoDto relazione = documenti.trasmetti(bozza.id());

        assertEquals(2, relazione.versioneClassificazione());
        assertEquals(1, relazione.riclassificazioni().size());
        Riclassificazione cambiamento = relazione.riclassificazioni().get(0);
        ClassificazioneDto nuova = dopo.classificazioni().stream()
                .filter(c -> c.versione() == 2).findFirst().orElseThrow();
        assertEquals(1, cambiamento.versionePrecedente());
        assertEquals(2, cambiamento.versioneNuova());
        assertEquals(nuova.dataClassificazione(), cambiamento.momento());
        assertEquals(MOTIVAZIONE, cambiamento.motivazione());
        assertEquals(StatoIncidente.CHIUSO, incidenti.trova(id).getStato());
        assertTrue(incidenti.scadenze(id).stream().allMatch(s -> s.stato() == StatoScadenza.RISPETTATA));

        Files.createDirectories(Path.of("target"));
        Files.writeString(Path.of("target", "traccia-riclassificazione.md"), markdown(prima, dopo, relazione), StandardCharsets.UTF_8);
        Files.write(Path.of("target", "relazione-finale-scenario.pdf"), documenti.pdf(relazione.id()).contenuto());
    }

    private static String markdown(TracciaDto prima, TracciaDto dopo, DocumentoDto relazione) {
        StringBuilder md = new StringBuilder("# Traccia di esecuzione – riclassificazione tardiva\n\n")
                .append("Generata dal test `ScenarioRiclassificazioneTardivaTest`; ")
                .append("orari in ora italiana. Incidente ").append(dopo.incidenteId())
                .append(": console di orchestrazione SD-WAN irraggiungibile (servizio S1).\n\n")
                .append("## Classificazioni\n\n")
                .append("| Versione | Valori TC-ACN | Momento | Motivazione | Attiva |\n| --- | --- | --- | --- | --- |\n");
        for (ClassificazioneDto c : dopo.classificazioni()) {
            md.append("| v").append(c.versione()).append(" | ")
                    .append(c.valori().stream().map(ValoreClassificato::codice).collect(Collectors.joining(", ")))
                    .append(" | ").append(ORA.format(c.dataClassificazione())).append(" | ")
                    .append(c.motivazione() == null ? "–" : c.motivazione()).append(" | ")
                    .append(c.attiva() ? "sì" : "no").append(" |\n");
        }
        md.append(tabellaDocumenti("Prima della riclassificazione", prima));
        md.append(tabellaDocumenti("Dopo la riclassificazione", dopo));
        md.append("\nVerifica: l'elenco dei documenti prima e dopo è identico (stessi id, versioni, stato ")
                .append("e classificazione fissata).\n\n## Relazione finale\n\n");
        for (Riclassificazione r : relazione.riclassificazioni()) {
            md.append("- **Classificazione precedente** (v").append(r.versionePrecedente()).append("): ")
                    .append(r.classificazionePrecedente()).append('\n')
                    .append("- **Classificazione nuova** (v").append(r.versioneNuova()).append("): ")
                    .append(r.classificazioneNuova()).append('\n')
                    .append("- **Momento del cambiamento**: ").append(ORA.format(r.momento())).append('\n')
                    .append("- **Motivazione**: ").append(r.motivazione()).append('\n');
        }
        md.append("\nRelazione finale (documento ").append(relazione.id()).append(", versione ")
                .append(relazione.versione()).append(") trasmessa il ").append(ORA.format(relazione.dataTrasmissione()))
                .append(" con classificazione v").append(relazione.versioneClassificazione()).append(".\n");
        return md.toString();
    }

    private static String tabellaDocumenti(String titolo, TracciaDto traccia) {
        StringBuilder md = new StringBuilder("\n## ").append(titolo).append(" (").append(ORA.format(traccia.istante()))
                .append(")\n\nStato dell'incidente: ").append(traccia.statoIncidente())
                .append(" – classificazione attiva: v").append(traccia.classificazioneAttiva()).append("\n\n")
                .append("| Id | Tipo | Versione | Stato | Classificazione |\n")
                .append("| --- | --- | --- | --- | --- |\n");
        for (FotoDocumento d : traccia.documenti()) {
            md.append("| ").append(d.id()).append(" | ").append(d.tipo()).append(" | ").append(d.versione())
                    .append(" | ").append(d.stato()).append(" | v").append(d.versioneClassificazione())
                    .append(" |\n");
        }
        return md.toString();
    }
}