package com.csirt.notifiche.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.csirt.notifiche.documenti.RegoleDocumento;
import com.csirt.notifiche.documenti.Riclassificazioni;
import com.csirt.notifiche.documenti.Riclassificazioni.Riclassificazione;
import com.csirt.notifiche.dto.ClassificazioneDto;
import com.csirt.notifiche.dto.ContenutoDto;
import com.csirt.notifiche.dto.DocumentoDto;
import com.csirt.notifiche.dto.FotoDocumento;
import com.csirt.notifiche.dto.TracciaDto;
import com.csirt.notifiche.model.Classificazione;
import com.csirt.notifiche.model.ContenutoNotifica;
import com.csirt.notifiche.model.Documento;
import com.csirt.notifiche.model.Incidente;
import com.csirt.notifiche.model.StatoDocumento;
import com.csirt.notifiche.model.StatoIncidente;
import com.csirt.notifiche.model.TipoDocumento;
import com.csirt.notifiche.repository.ClassificazioneRepository;
import com.csirt.notifiche.repository.DocumentoRepository;
import com.csirt.notifiche.dto.FileScaricabile;
import com.csirt.notifiche.scadenze.Scadenza;

@Service
public class DocumentoService {

    private final DocumentoRepository documenti;
    private final ClassificazioneRepository classificazioni;
    private final IncidenteService incidenteService;
    private final PdfGeneratorService pdf;
    private final Clock clock;

    public DocumentoService(DocumentoRepository documenti, ClassificazioneRepository classificazioni,
                            IncidenteService incidenteService, PdfGeneratorService pdf, Clock clock) {
        this.documenti = documenti;
        this.classificazioni = classificazioni;
        this.incidenteService = incidenteService;
        this.pdf = pdf;
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
        return dto(documento);
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
        return dto(documento);
    }

    /** Trasmissione al CSIRT: ordine, completezza, classificazione, chiusura. */
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

        Classificazione classificazione = classificazioni.findByIncidenteIdAndAttivaTrue(incidente.getId()).orElse(null);
        List<String> mancanti = RegoleDocumento.campiMancanti(documento.getContenuto(), classificazione != null,
                documento.getTipo());
        if (!mancanti.isEmpty()) {
            throw new DocumentoIncompletoException(documento.getTipo(), mancanti);
        }

        documento.trasmetti(Instant.now(clock), classificazione);

        if (documento.getTipo() == TipoDocumento.RELAZIONE_FINALE) {
            incidente.chiudi();
        }
        return dto(documento);
    }

    @Transactional(readOnly = true)
    public DocumentoDto dettaglio(Long documentoId) {
        return dto(trova(documentoId));
    }

    /** Versione stampabile del documento, con il termine e il suo stato. */
    @Transactional(readOnly = true)
    public FileScaricabile pdf(Long documentoId) {
        Documento documento = trova(documentoId);
        DocumentoDto dto = dto(documento);
        Incidente incidente = documento.getIncidente();
        Scadenza scadenza = incidenteService.scadenze(incidente.getId()).stream()
                .filter(s -> s.tipo() == documento.getTipo())
                .findFirst()
                .orElse(null);
        return new FileScaricabile(dto.nomeFile("pdf"), pdf.documento(dto, incidente, scadenza));
    }

    @Transactional(readOnly = true)
    public List<DocumentoDto> elenco(Long incidenteId) {
        incidenteService.trova(incidenteId);
        return documenti.findByIncidenteIdOrderByIdAsc(incidenteId).stream().map(this::dto).toList();
    }

    /** Traccia di esecuzione: classificazioni e documenti con id, versione, stato e impronta. */
    @Transactional(readOnly = true)
    public TracciaDto traccia(Long incidenteId) {
        Incidente incidente = incidenteService.trova(incidenteId);
        List<ClassificazioneDto> storico = classificazioni.findByIncidenteIdOrderByVersioneAsc(incidenteId).stream()
                .map(ClassificazioneDto::da).toList();
        Integer attiva = classificazioni.findByIncidenteIdAndAttivaTrue(incidenteId)
                .map(Classificazione::getVersione).orElse(null);
        List<FotoDocumento> foto = documenti.findByIncidenteIdOrderByIdAsc(incidenteId).stream()
                .map(d -> FotoDocumento.da(dto(d))).toList();
        return new TracciaDto(incidenteId, incidente.getStato(), attiva, storico, foto, Instant.now(clock));
    }

    private DocumentoDto dto(Documento documento) {
        Classificazione finale = documento.isTrasmesso()
                ? documento.getClassificazione()
                : classificazioni.findByIncidenteIdAndAttivaTrue(documento.getIncidente().getId()).orElse(null);
        return DocumentoDto.da(documento, riclassificazioniDopoLaNotifica(documento, finale));
    }

    /** Solo per la relazione finale: i cambi di classificazione successivi alla notifica delle 72 ore. */
    private List<Riclassificazione> riclassificazioniDopoLaNotifica(Documento documento, Classificazione finale) {
        if (documento.getTipo() != TipoDocumento.RELAZIONE_FINALE) {
            return List.of();
        }
        Classificazione allaNotifica = documenti.findFirstByIncidenteIdAndTipoAndStatoOrderByVersioneAsc(
                        documento.getIncidente().getId(), TipoDocumento.NOTIFICA, StatoDocumento.TRASMESSO)
                .map(Documento::getClassificazione)
                .orElse(null);
        return Riclassificazioni.tra(allaNotifica, finale);
    }

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