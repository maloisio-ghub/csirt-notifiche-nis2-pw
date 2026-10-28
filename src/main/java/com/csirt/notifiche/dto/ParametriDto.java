package com.csirt.notifiche.dto;

import java.math.BigDecimal;

import com.csirt.notifiche.model.ParametriSignificativita;

public record ParametriDto(
        BigDecimal fatturatoAnnuo,
        BigDecimal sogliaPerdita,
        int minutiIndisponibilitaTotale,
        int minutiDisponibilitaLimitata,
        BigDecimal quotaUtenti,
        long utentiMassimi,
        String nota) {

    public static ParametriDto da(ParametriSignificativita p) {
        return new ParametriDto(p.getFatturatoAnnuo(), p.comeSoglia().sogliaPerdita(),
                p.getMinutiIndisponibilitaTotale(), p.getMinutiDisponibilitaLimitata(),
                p.getQuotaUtenti(), p.getUtentiMassimi(), p.getNota());
    }
}