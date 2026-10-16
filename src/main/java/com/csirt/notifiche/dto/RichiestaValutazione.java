package com.csirt.notifiche.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.PositiveOrZero;

public record RichiestaValutazione(
        @PositiveOrZero @Digits(integer = 13, fraction = 2) BigDecimal perditaEconomica,
        Boolean esfiltrazioneSegretiCommerciali,
        Boolean decesso,
        Boolean dannoConsiderevoleSalute,
        Boolean accessoMalevoloConGravePerturbazione,
        @Valid List<ImpattoRichiesto> impatti) {
}