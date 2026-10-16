package com.csirt.notifiche.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.csirt.notifiche.model.ImpattoValutato;
import com.csirt.notifiche.model.ValutazioneSignificativita;
import com.csirt.notifiche.significativita.ImpattoServizio;

public record ValutazioneDto(
        Long id,
        Instant dataValutazione,
        int versioneParametri,
        boolean significativo,
        List<String> regole,
        List<String> motivazioni,
        BigDecimal perditaEconomica,
        List<ImpattoServizio> impatti) {

    public static ValutazioneDto da(ValutazioneSignificativita v) {
        return new ValutazioneDto(v.getId(), v.getDataValutazione(), v.getParametri().getVersione(),
                v.isEsitoSignificativo(), v.elencoRegole(), v.elencoMotivazioni(), v.getPerditaEconomica(),
                v.getImpatti().stream().map(ImpattoValutato::comeImpatto).toList());
    }
}