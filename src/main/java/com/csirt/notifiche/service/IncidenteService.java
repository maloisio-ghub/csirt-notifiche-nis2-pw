package com.csirt.notifiche.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.csirt.notifiche.dto.NuovoIncidente;
import com.csirt.notifiche.model.Incidente;
import com.csirt.notifiche.repository.IncidenteRepository;
import com.csirt.notifiche.scadenze.CalcolatoreScadenze;
import com.csirt.notifiche.scadenze.Scadenza;

@Service
public class IncidenteService {

    private final IncidenteRepository repository;
    private final Clock clock;
    private final CalcolatoreScadenze calcolatore = new CalcolatoreScadenze();

    public IncidenteService(IncidenteRepository repository, Clock clock) {
        this.repository = repository;
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

        @Transactional(readOnly = true)
    public List<Scadenza> scadenze(Long id) {
        Incidente incidente = trova(id);
        // La data di trasmissione della notifica sarà disponibile dal Passo 8, con i documenti.
        return calcolatore.calcola(incidente.getDataConoscenza(), null, Instant.now(clock));
    }
}
