package com.csirt.notifiche.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.csirt.notifiche.model.ParametriSignificativita;

public record ParametriDto(
        int versione,
        LocalDate validoDal,
        BigDecimal fatturatoAnnuo,
        BigDecimal sogliaPerdita,
        int minutiIndisponibilitaTotale,
        int minutiDisponibilitaLimitata,
        BigDecimal quotaUtenti,
        long utentiMassimi,
        String nota) {

    public static ParametriDto da(ParametriSignificativita p) {
        return new ParametriDto(p.getVersione(), p.getValidoDal(), p.getFatturatoAnnuo(),
                p.comeSoglia().sogliaPerdita(), p.getMinutiIndisponibilitaTotale(),
                p.getMinutiDisponibilitaLimitata(), p.getQuotaUtenti(), p.getUtentiMassimi(), p.getNota());
    }
}