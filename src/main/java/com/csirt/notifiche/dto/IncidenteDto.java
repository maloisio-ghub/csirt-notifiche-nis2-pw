package com.csirt.notifiche.dto;

import java.time.Instant;

import com.csirt.notifiche.model.Incidente;
import com.csirt.notifiche.model.StatoIncidente;

public record IncidenteDto(
        Long id,
        String titolo,
        String descrizione,
        Instant dataRilevamento,
        Instant dataConoscenza,
        StatoIncidente stato,
        Instant dataRegistrazione) {

    public static IncidenteDto da(Incidente i) {
        return new IncidenteDto(i.getId(), i.getTitolo(), i.getDescrizione(), i.getDataRilevamento(),
                i.getDataConoscenza(), i.getStato(), i.getDataRegistrazione());
    }
}
