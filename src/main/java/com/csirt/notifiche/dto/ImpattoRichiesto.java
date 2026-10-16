package com.csirt.notifiche.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/** Impatto su un servizio dichiarato dall'operatore. La base utenti la prende il server dal database. */
public record ImpattoRichiesto(
        @NotBlank String codiceServizio,
        @PositiveOrZero Integer minutiIndisponibilitaTotale,
        @PositiveOrZero Integer minutiDisponibilitaLimitata,
        @PositiveOrZero Long utentiDisponibilitaLimitata,
        Boolean compromissioneMalevola,
        @PositiveOrZero Long utentiCompromissione) {
}