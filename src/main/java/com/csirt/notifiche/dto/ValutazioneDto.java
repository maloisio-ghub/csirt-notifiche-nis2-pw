package com.csirt.notifiche.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.csirt.notifiche.model.ImpattoValutato;
import com.csirt.notifiche.model.ValutazioneSignificativita;

public record ValutazioneDto(
        Long id,
        Instant dataValutazione,
        boolean significativo,
        List<String> regole,
        List<String> motivazioni,
        BigDecimal perditaEconomica,
        List<ImpattoValutato> impatti) {

    public static ValutazioneDto da(ValutazioneSignificativita v) {
        return new ValutazioneDto(v.getId(), v.getDataValutazione(), v.isEsitoSignificativo(),
                v.elencoRegole(), v.elencoMotivazioni(), v.getPerditaEconomica(), List.copyOf(v.getImpatti()));
    }
}