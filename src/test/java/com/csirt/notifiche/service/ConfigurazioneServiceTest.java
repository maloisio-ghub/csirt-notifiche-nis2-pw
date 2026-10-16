package com.csirt.notifiche.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.csirt.notifiche.model.ServizioCritico;
import com.csirt.notifiche.significativita.ParametriSoglia;

@SpringBootTest
class ConfigurazioneServiceTest {

    @Autowired
    private ConfigurazioneService configurazione;

    @Test
    @DisplayName("I parametri caricati nel database producono le soglie documentate")
    void soglieDalDatabase() {
        ParametriSoglia soglie = configurazione.parametriInVigore().comeSoglia();

        assertEquals(0, new BigDecimal("500000").compareTo(soglie.sogliaPerdita()));

        Map<String, Long> calcolate = configurazione.servizi().stream()
                .collect(Collectors.toMap(ServizioCritico::getCodice, s -> soglie.sogliaUtenti(s.getBaseUtenti())));
        assertEquals(Map.of("S1", 600L, "S2", 400L, "S3", 250L), calcolate);
    }
}