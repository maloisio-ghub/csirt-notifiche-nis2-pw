package com.csirt.notifiche.scadenze;

import java.time.Instant;

import com.csirt.notifiche.model.TipoDocumento;

/**
 * Situazione di un termine
 * negativo se in ritardo, null se il termine non è definito.
 */
public record Scadenza(TipoDocumento tipo, Instant termine, StatoScadenza stato, Long minutiResidui,
                       Instant trasmissione) {
}