package com.csirt.notifiche.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.csirt.notifiche.documenti.RegoleDocumento;
import com.csirt.notifiche.dto.ContenutoDto;
import com.csirt.notifiche.dto.DocumentoDto;
import com.csirt.notifiche.model.ContenutoNotifica;
import com.csirt.notifiche.model.Documento;
import com.csirt.notifiche.model.Incidente;
import com.csirt.notifiche.model.StatoDocumento;
import com.csirt.notifiche.model.StatoIncidente;
import com.csirt.notifiche.model.TipoDocumento;
import com.csirt.notifiche.repository.DocumentoRepository;

@Service
public class DocumentoService {

    private final DocumentoRepository documenti;
    private final IncidenteService incidenteService;
    private final Clock clock;

    public DocumentoService(DocumentoRepository documenti, IncidenteService incidenteService, Clock clock) {
        this.documenti = documenti;
        this.incidenteService = incidenteService;
        this.clock = clock;
    }

    /** Nuova bozza, precompilata con il contenuto del documento più recente dell'incidente. */
    @Transactional
    public DocumentoDto crea(Long incidenteId, TipoDocumento tipo) {
        Incidente incidente = incidenteService.trova(incidenteId);
        if (incidente.getStato() != StatoIncidente.SIGNIFICATIVO) {
            throw new OperazioneNonConsentitaException("L'incidente è " + incidente.getStato()
                    + ": non si possono creare documenti");
        }

        Optional<Documento> ultimoDelTipo = documenti.findTopByIncidenteIdAndTipoOrderByVersioneDesc(incidenteId, tipo);
        if (ultimoDelTipo.map(Documento::isBozza).orElse(false)) {
            throw new OperazioneNonConsentitaException("Esiste già una bozza di " + tipo.getEtichetta()
                    + ": va completata quella");
        }
        int versione = ultimoDelTipo.map(d -> d.getVersione() + 1).orElse(1);

        ContenutoNotifica contenuto = documenti.findTopByIncidenteIdOrderByIdDesc(incidenteId)
                .map(Documento::getContenuto)
                .orElseGet(() -> contenutoIniziale(incidente));

        Documento documento = documenti.save(new Documento(incidente, tipo, versione, contenuto, Instant.now(clock)));
        return DocumentoDto.da(documento);
    }

    /** Salvataggio in bozza: sostituisce il contenuto. */
    @Transactional
    public DocumentoDto aggiorna(Long documentoId, ContenutoDto dati) {
        Documento documento = trova(documentoId);
        try {
            documento.aggiornaContenuto(dati.comeContenuto(), Instant.now(clock));
        } catch (IllegalStateException e) {
            throw new OperazioneNonConsentitaException(e.getMessage());
        }
        return DocumentoDto.da(documento);
    }

    /** Trasmissione al CSIRT (simulata): ordine, completezza, impronta, chiusura del fascicolo. */
    @Transactional
    public DocumentoDto trasmetti(Long documentoId) {
        Documento documento = trova(documentoId);
        Incidente incidente = documento.getIncidente();
        if (!documento.isBozza()) {
            throw new OperazioneNonConsentitaException(documento.getTipo().getEtichetta() + " v"
                    + documento.getVersione() + " è già stata trasmessa");
        }
        if (incidente.getStato() == StatoIncidente.CHIUSO) {
            throw new OperazioneNonConsentitaException("L'incidente è chiuso: nessun ulteriore invio");
        }
        verificaOrdine(documento);

        List<String> mancanti = RegoleDocumento.campiMancanti(documento.getContenuto(), documento.getTipo());
        if (!mancanti.isEmpty()) {
            throw new DocumentoIncompletoException(documento.getTipo(), mancanti);
        }

        // Il database conserva i microsecondi: si tronca qui perché l'impronta resti verificabile dopo la rilettura.
        Instant adesso = Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
        documento.trasmetti(adesso, RegoleDocumento.sha256(RegoleDocumento.testoCanonico(documento, adesso)));

        if (documento.getTipo() == TipoDocumento.RELAZIONE_FINALE) {
            incidente.chiudi();
        }
        return DocumentoDto.da(documento);
    }

    @Transactional(readOnly = true)
    public DocumentoDto dettaglio(Long documentoId) {
        return DocumentoDto.da(trova(documentoId));
    }

    @Transactional(readOnly = true)
    public List<DocumentoDto> elenco(Long incidenteId) {
        incidenteService.trova(incidenteId);
        return documenti.findByIncidenteIdOrderByIdAsc(incidenteId).stream().map(DocumentoDto::da).toList();
    }

    /** La notifica richiede la pre-notifica trasmessa; la relazione finale richiede la notifica trasmessa. */
    private void verificaOrdine(Documento documento) {
        TipoDocumento precedente = switch (documento.getTipo()) {
            case PRE_NOTIFICA -> null;
            case NOTIFICA -> TipoDocumento.PRE_NOTIFICA;
            case RELAZIONE_FINALE -> TipoDocumento.NOTIFICA;
        };
        if (precedente != null && !documenti.existsByIncidenteIdAndTipoAndStato(
                documento.getIncidente().getId(), precedente, StatoDocumento.TRASMESSO)) {
            throw new OperazioneNonConsentitaException("Prima di trasmettere " + documento.getTipo().getEtichetta()
                    + " va trasmessa: " + precedente.getEtichetta());
        }
    }

    private Documento trova(Long documentoId) {
        return documenti.findById(documentoId).orElseThrow(() -> new DocumentoNonTrovatoException(documentoId));
    }

    private static ContenutoNotifica contenutoIniziale(Incidente incidente) {
        String descrizione = incidente.getDescrizione() == null
                ? incidente.getTitolo()
                : incidente.getTitolo() + " - " + incidente.getDescrizione();
        return ContenutoNotifica.conDescrizione(descrizione);
    }
}