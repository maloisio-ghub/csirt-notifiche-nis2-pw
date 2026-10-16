package com.csirt.notifiche.significativita;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Soglie numeriche del Reg. 2024/2690 applicate al contesto aziendale. */
public record ParametriSoglia(
        BigDecimal fatturatoAnnuo,
        BigDecimal perditaMassima,
        BigDecimal quotaFatturato,
        int minutiIndisponibilitaTotale,
        int minutiDisponibilitaLimitata,
        BigDecimal quotaUtenti,
        long utentiMassimi) {

    /** Valori per Alfa Managed Services S.r.l. */
    public static ParametriSoglia alfaManagedServices() {
        return new ParametriSoglia(new BigDecimal("24000000"), new BigDecimal("500000"),
                new BigDecimal("0.05"), 30, 60, new BigDecimal("0.05"), 1_000_000);
    }

    /** Il minore tra 500.000 € e il 5% del fatturato (art. 3.1.a). */
    public BigDecimal sogliaPerdita() {
        return fatturatoAnnuo.multiply(quotaFatturato).min(perditaMassima);
    }

    /** Il minore tra il 5% della base utenti e 1 milione (art. 10). */
    public long sogliaUtenti(long baseUtenti) {
        long quota = BigDecimal.valueOf(baseUtenti).multiply(quotaUtenti)
                .setScale(0, RoundingMode.FLOOR).longValueExact();
        return Math.min(quota, utentiMassimi);
    }
}
