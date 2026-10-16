package com.csirt.notifiche.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.csirt.notifiche.dto.IncidenteDto;
import com.csirt.notifiche.dto.NuovoIncidente;
import com.csirt.notifiche.model.Incidente;
import com.csirt.notifiche.service.IncidenteService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/incidenti")
public class IncidenteController {

    private final IncidenteService service;

    public IncidenteController(IncidenteService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<IncidenteDto> registra(@Valid @RequestBody NuovoIncidente dati) {
        Incidente incidente = service.registra(dati);
        URI posizione = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(incidente.getId()).toUri();
        return ResponseEntity.created(posizione).body(IncidenteDto.da(incidente));
    }

    @GetMapping
    public List<IncidenteDto> elenco() {
        return service.elenco().stream().map(IncidenteDto::da).toList();
    }

    @GetMapping("/{id}")
    public IncidenteDto dettaglio(@PathVariable Long id) {
        return IncidenteDto.da(service.trova(id));
    }
}
