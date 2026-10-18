package com.csirt.notifiche.dto;

import java.time.Instant;
import java.util.List;

import com.csirt.notifiche.model.StatoIncidente;

public record TracciaDto(Long incidenteId, StatoIncidente statoIncidente, Integer classificazioneAttiva,
                         List<ClassificazioneDto> classificazioni, List<FotoDocumento> documenti, Instant istante) {
}