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

import com.csirt.notifiche.dto.ClassificazioneDto;
import com.csirt.notifiche.dto.RichiestaClassificazione;
import com.csirt.notifiche.service.ClassificazioneService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/incidenti/{id}/classificazioni")
public class ClassificazioneController {

    private final ClassificazioneService service;

    public ClassificazioneController(ClassificazioneService service) {
        this.service = service;
    }

    /** Prima classificazione o riclassificazione. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClassificazioneDto classifica(@PathVariable Long id, @Valid @RequestBody RichiestaClassificazione richiesta) {
        return service.classifica(id, richiesta);
    }

    @GetMapping
    public List<ClassificazioneDto> storico(@PathVariable Long id) {
        return service.storico(id);
    }
}