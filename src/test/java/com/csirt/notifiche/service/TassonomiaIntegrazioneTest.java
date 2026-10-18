package com.csirt.notifiche.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.csirt.notifiche.dto.ConteggioMacrocategoria;
import com.csirt.notifiche.dto.ConteggioTipo;
import com.csirt.notifiche.dto.TassonomiaDto;
import com.csirt.notifiche.dto.VoceDto;

@SpringBootTest
class TassonomiaIntegrazioneTest {

    @Autowired
    private TassonomiaService tassonomia;

    @Test
    @DisplayName("Query di conteggio: 4 macrocategorie, 21 predicati, 219 valori, come dichiarato dal documento ACN")
    void conteggiPerLivello() {
        assertEquals(List.of(new ConteggioTipo("MACROCATEGORIA", 4), new ConteggioTipo("PREDICATO", 21),
                new ConteggioTipo("VALORE", 219)), tassonomia.conteggi("TC-ACN-2.0").perTipo());
    }

    @Test
    @DisplayName("Conteggio per macrocategoria")
    void conteggiPerMacrocategoria() {
        assertEquals(List.of(
                new ConteggioMacrocategoria("BC", 4, 26),
                new ConteggioMacrocategoria("TT", 9, 62),
                new ConteggioMacrocategoria("TA", 2, 10),
                new ConteggioMacrocategoria("AC", 6, 121)),
                tassonomia.conteggi("TC-ACN-2.0").perMacrocategoria());
    }

    @Test
    @DisplayName("Versione attiva con data di acquisizione e impronta; gerarchia dei codici rispettata")
    void versioneAttiva() {
        TassonomiaDto attiva = tassonomia.attiva();

        assertEquals("TC-ACN-2.0", attiva.versione().codice());
        assertNotNull(attiva.versione().dataAcquisizione());
        assertEquals(64, attiva.versione().sha256Documento().length());
        assertEquals(244, attiva.voci().size());
        VoceDto disponibilita = attiva.voci().stream().filter(v -> v.codice().equals("BC:IM_AV")).findFirst().orElseThrow();
        assertEquals("BC:IM", disponibilita.padre());
    }
}