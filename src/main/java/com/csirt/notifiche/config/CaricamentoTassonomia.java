package com.csirt.notifiche.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.csirt.notifiche.service.TassonomiaService;

/**
 * All'avvio carica le versioni della tassonomia elencate nel manifest. Se un file è malformato
 * l'applicazione non parte
 */
@Component
public class CaricamentoTassonomia implements ApplicationRunner {

    private final TassonomiaService tassonomia;

    public CaricamentoTassonomia(TassonomiaService tassonomia) {
        this.tassonomia = tassonomia;
    }

    @Override
    public void run(ApplicationArguments args) {
        tassonomia.caricaDaManifest("tassonomia/versioni.csv");
    }
}