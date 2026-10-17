package com.csirt.notifiche.service;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.csirt.notifiche.dto.NuovoIncidente;
import com.csirt.notifiche.model.Incidente;
import com.csirt.notifiche.model.StatoDocumento;
import com.csirt.notifiche.model.TipoDocumento;
import com.csirt.notifiche.repository.DocumentoRepository;
import com.csirt.notifiche.repository.IncidenteRepository;
import com.csirt.notifiche.scadenze.CalcolatoreScadenze;
import com.csirt.notifiche.scadenze.Scadenza;

@Service
public class IncidenteService {

    private final IncidenteRepository repository;
    private final DocumentoRepository documenti;
    private final Clock clock;
    private final CalcolatoreScadenze calcolatore = new CalcolatoreScadenze();

    public IncidenteService(IncidenteRepository repository, DocumentoRepository documenti, Clock clock) {
        this.repository = repository;
        this.documenti = documenti;
        this.clock = clock;
    }

    @Transactional
    public Incidente registra(NuovoIncidente dati) {
        Incidente incidente = new Incidente(dati.titolo().trim(), dati.descrizione(),
                dati.dataRilevamento(), Instant.now(clock));
        return repository.save(incidente);
    }

    @Transactional(readOnly = true)
    public Incidente trova(Long id) {
        return repository.findById(id).orElseThrow(() -> new IncidenteNonTrovatoException(id));
    }

    @Transactional(readOnly = true)
    public List<Incidente> elenco() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "dataRegistrazione"));
    }

    /** Termini dell'incidente, con l'esito per i documenti già trasmessi (prima versione di ogni tipo). */
    @Transactional(readOnly = true)
    public List<Scadenza> scadenze(Long id) {
        Incidente incidente = trova(id);
        Map<TipoDocumento, Instant> trasmissioni = new EnumMap<>(TipoDocumento.class);
        for (TipoDocumento tipo : TipoDocumento.values()) {
            documenti.findFirstByIncidenteIdAndTipoAndStatoOrderByVersioneAsc(id, tipo, StatoDocumento.TRASMESSO)
                    .ifPresent(d -> trasmissioni.put(tipo, d.getDataTrasmissione()));
        }
        return calcolatore.calcolaConTrasmissioni(incidente.getDataConoscenza(), trasmissioni, Instant.now(clock));
    }
}