package com.csirt.notifiche.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.csirt.notifiche.dto.ParametriDto;
import com.csirt.notifiche.dto.ServizioDto;
import com.csirt.notifiche.service.ConfigurazioneService;
import com.csirt.notifiche.significativita.ParametriSoglia;

@RestController
@RequestMapping("/api")
public class ConfigurazioneController {

    private final ConfigurazioneService service;

    public ConfigurazioneController(ConfigurazioneService service) {
        this.service = service;
    }

    @GetMapping("/servizi")
    public List<ServizioDto> servizi() {
        ParametriSoglia soglie = service.parametriInVigore().comeSoglia();
        return service.servizi().stream().map(s -> ServizioDto.da(s, soglie)).toList();
    }

    @GetMapping("/parametri")
    public ParametriDto parametri() {
        return ParametriDto.da(service.parametriInVigore());
    }
}