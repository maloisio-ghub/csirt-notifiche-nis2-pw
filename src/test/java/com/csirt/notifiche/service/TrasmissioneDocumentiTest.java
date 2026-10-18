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
import com.csirt.notifiche.model.GravitaIncidente;
import com.csirt.notifiche.model.StatoDocumento;
import com.csirt.notifiche.model.StatoIncidente;
import com.csirt.notifiche.model.TipoDocumento;
import com.csirt.notifiche.scadenze.CalcolatoreScadenze;
import com.csirt.notifiche.scadenze.Scadenza;
import com.csirt.notifiche.scadenze.StatoScadenza;
import com.csirt.notifiche.dto.RichiestaClassificazione;

@SpringBootTest
@Transactional
class TrasmissioneDocumentiTest {

    private static final ContenutoDto PRE = contenuto(null, null, null, null, null);
    private static final ContenutoDto NOTIFICA = contenuto(GravitaIncidente.ALTA,
            "Clienti senza gestione della rete per 31 minuti", null, null, null);
    private static final ContenutoDto RELAZIONE = contenuto(GravitaIncidente.ALTA,
            "Clienti senza gestione della rete per 31 minuti", "Descrizione dettagliata dell'accaduto",
            "Aggiornamento firmware difettoso", "Rollback del firmware e test in pre-produzione");

    @Autowired
    private IncidenteService incidenti;

    @Autowired
    private SignificativitaService significativita;

    @Autowired
    private DocumentoService documenti;

    @Autowired
    private ClassificazioneService classificazioni;

    private static ContenutoDto contenuto(GravitaIncidente gravita, String impatto, String dettaglio, String causa,
                                          String misure) {
        return new ContenutoDto("Mario Rossi", "soc@alfa.example", "Console SD-WAN irraggiungibile", false, false,
                gravita, impatto, null, dettaglio, causa, misure);
    }

    private Long incidenteSignificativo() {
        Long id = incidenti.registra(new NuovoIncidente("Console SD-WAN irraggiungibile", null,
                Instant.parse("2026-10-17T08:00:00Z"))).getId();
        significativita.valuta(id, new RichiestaValutazione(null, null, null, null, null,
                List.of(new ImpattoRichiesto("S1", 31, null, null, null, null))));
                classificazioni.classifica(id, new RichiestaClassificazione(List.of("BC:IM_AV", "TT:AV_OU"), null));        
        return id;
    }

    private DocumentoDto bozza(Long incidenteId, TipoDocumento tipo, ContenutoDto contenuto) {
        DocumentoDto bozza = documenti.crea(incidenteId, tipo);
        return documenti.aggiorna(bozza.id(), contenuto);
    }

    private DocumentoDto trasmesso(Long incidenteId, TipoDocumento tipo, ContenutoDto contenuto) {
        return documenti.trasmetti(bozza(incidenteId, tipo, contenuto).id());
    }

    @Test
    @DisplayName("Pre-notifica incompleta: rifiutata con l'elenco dei campi mancanti, resta in bozza")
    void preNotificaIncompleta() {
        DocumentoDto bozza = documenti.crea(incidenteSignificativo(), TipoDocumento.PRE_NOTIFICA);

        DocumentoIncompletoException e = assertThrows(DocumentoIncompletoException.class,
                () -> documenti.trasmetti(bozza.id()));

        assertEquals(List.of("Referente - nome", "Referente - e-mail", "Sospetto di atto illegittimo o malevolo",
                "Impatto transfrontaliero"), e.getCampiMancanti());
        assertEquals(StatoDocumento.BOZZA, documenti.dettaglio(bozza.id()).stato());
    }

    @Test
    @DisplayName("Notifica incompleta: mancano gravità e impatto")
    void notificaIncompleta() {
        Long id = incidenteSignificativo();
        trasmesso(id, TipoDocumento.PRE_NOTIFICA, PRE);
        DocumentoDto notifica = documenti.crea(id, TipoDocumento.NOTIFICA);

        DocumentoIncompletoException e = assertThrows(DocumentoIncompletoException.class,
                () -> documenti.trasmetti(notifica.id()));

        assertEquals(List.of("Gravità", "Impatto"), e.getCampiMancanti());
    }

    @Test
    @DisplayName("Relazione finale incompleta: mancano descrizione dettagliata, causa e misure")
    void relazioneFinaleIncompleta() {
        Long id = incidenteSignificativo();
        trasmesso(id, TipoDocumento.PRE_NOTIFICA, PRE);
        trasmesso(id, TipoDocumento.NOTIFICA, NOTIFICA);
        DocumentoDto relazione = documenti.crea(id, TipoDocumento.RELAZIONE_FINALE);

        DocumentoIncompletoException e = assertThrows(DocumentoIncompletoException.class,
                () -> documenti.trasmetti(relazione.id()));

        assertEquals(List.of("Descrizione dettagliata", "Tipo di minaccia o causa principale",
                "Misure di attenuazione"), e.getCampiMancanti());
    }

    @Test
    @DisplayName("La notifica non si trasmette prima della pre-notifica")
    void ordineDegliAdempimenti() {
        Long id = incidenteSignificativo();
        DocumentoDto notifica = bozza(id, TipoDocumento.NOTIFICA, NOTIFICA);

        assertThrows(OperazioneNonConsentitaException.class, () -> documenti.trasmetti(notifica.id()));
    }

    @Test
    @DisplayName("Un documento trasmesso è immutabile e la sua impronta è verificabile")
    void documentoTrasmessoImmutabile() {
        Long id = incidenteSignificativo();
        DocumentoDto pre = trasmesso(id, TipoDocumento.PRE_NOTIFICA, PRE);

        assertEquals(StatoDocumento.TRASMESSO, pre.stato());
        assertEquals(64, pre.hashSha256().length());
        assertTrue(pre.integro());
        assertThrows(OperazioneNonConsentitaException.class, () -> documenti.aggiorna(pre.id(), NOTIFICA));
        assertThrows(OperazioneNonConsentitaException.class, () -> documenti.trasmetti(pre.id()));

        DocumentoDto versione2 = documenti.crea(id, TipoDocumento.PRE_NOTIFICA);
        assertEquals(2, versione2.versione());
    }

    @Test
    @DisplayName("Flusso completo: tre invii nei termini, relazione a un mese dalla notifica, incidente chiuso")
    void flussoCompleto() {
        Long id = incidenteSignificativo();
        trasmesso(id, TipoDocumento.PRE_NOTIFICA, PRE);
        DocumentoDto notifica = trasmesso(id, TipoDocumento.NOTIFICA, NOTIFICA);
        trasmesso(id, TipoDocumento.RELAZIONE_FINALE, RELAZIONE);

        assertEquals(StatoIncidente.CHIUSO, incidenti.trova(id).getStato());

        List<Scadenza> scadenze = incidenti.scadenze(id);
        assertTrue(scadenze.stream().allMatch(s -> s.stato() == StatoScadenza.RISPETTATA));
        Instant termineRelazione = notifica.dataTrasmissione()
                .atZone(CalcolatoreScadenze.FUSO_ORARIO).plusMonths(1).toInstant();
        assertEquals(termineRelazione, scadenze.get(2).termine());

        assertThrows(OperazioneNonConsentitaException.class, () -> documenti.crea(id, TipoDocumento.NOTIFICA));
    }
}