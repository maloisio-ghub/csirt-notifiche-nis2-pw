package com.csirt.notifiche.scadenze;

import java.time.Instant;

import com.csirt.notifiche.model.TipoDocumento;

/**
 * Situazione di un termine. minutiResidui è negativo se la scadenza è superata
 * (indica allora i minuti di ritardo); è null se il termine non è definito.
 */
public record Scadenza(TipoDocumento tipo, Instant termine, StatoScadenza stato, Long minutiResidui) {
}