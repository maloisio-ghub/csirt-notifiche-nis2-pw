package com.csirt.notifiche.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.csirt.notifiche.dto.RichiestaValutazione;
import com.csirt.notifiche.dto.ValutazioneDto;
import com.csirt.notifiche.service.SignificativitaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/incidenti/{id}/valutazioni")
public class ValutazioneController {

    private final SignificativitaService service;

    public ValutazioneController(SignificativitaService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ValutazioneDto valuta(@PathVariable Long id, @Valid @RequestBody RichiestaValutazione richiesta) {
        return service.valuta(id, richiesta);
    }

    @GetMapping
    public List<ValutazioneDto> storico(@PathVariable Long id) {
        return service.storico(id);
    }
}