package com.csirt.notifiche.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.csirt.notifiche.model.ParametriSignificativita;
import com.csirt.notifiche.model.ServizioCritico;
import com.csirt.notifiche.repository.ParametriSignificativitaRepository;
import com.csirt.notifiche.repository.ServizioCriticoRepository;

@Service
public class ConfigurazioneService {

    private final ServizioCriticoRepository servizi;
    private final ParametriSignificativitaRepository parametri;

    public ConfigurazioneService(ServizioCriticoRepository servizi, ParametriSignificativitaRepository parametri) {
        this.servizi = servizi;
        this.parametri = parametri;
    }

    @Transactional(readOnly = true)
    public ParametriSignificativita parametriInVigore() {
        return parametri.findTopByOrderByVersioneDesc()
                .orElseThrow(() -> new IllegalStateException("Nessun parametro di significatività configurato"));
    }

    @Transactional(readOnly = true)
    public List<ServizioCritico> servizi() {
        return servizi.findAll(Sort.by("codice"));
    }
}
