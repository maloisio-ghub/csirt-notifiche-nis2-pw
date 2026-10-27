package com.csirt.notifiche.significativita;

import java.math.BigDecimal;
import java.util.List;

/** Quello che serve per decidere se un incidente è significativo. */
public record DatiValutazione(
        BigDecimal perditaEconomica,
        boolean esfiltrazioneSegretiCommerciali,
        boolean decesso,
        boolean dannoConsiderevoleSalute,
        boolean accessoMalevoloConGravePerturbazione,
        List<ImpattoServizio> impatti) {

    public DatiValutazione {
        perditaEconomica = perditaEconomica == null ? BigDecimal.ZERO : perditaEconomica;
        impatti = impatti == null ? List.of() : List.copyOf(impatti);
    }
}
