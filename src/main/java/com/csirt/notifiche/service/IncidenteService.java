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

@Service
public class IncidenteService {

    private final IncidenteRepository repository;
    private final Clock clock;

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
}
