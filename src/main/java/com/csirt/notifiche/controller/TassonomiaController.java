package com.csirt.notifiche.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.csirt.notifiche.dto.ConteggiDto;
import com.csirt.notifiche.dto.TassonomiaDto;
import com.csirt.notifiche.service.TassonomiaService;

@RestController
@RequestMapping("/api/tassonomia")
public class TassonomiaController {

    private final TassonomiaService service;

    public TassonomiaController(TassonomiaService service) {
        this.service = service;
    }

    @GetMapping
    public TassonomiaDto attiva() {
        return service.attiva();
    }

    @GetMapping("/versioni/{codice}/conteggi")
    public ConteggiDto conteggi(@PathVariable String codice) {
        return service.conteggi(codice);
    }
}