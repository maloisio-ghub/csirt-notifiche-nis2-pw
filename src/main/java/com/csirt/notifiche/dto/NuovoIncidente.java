package com.csirt.notifiche.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

public record NuovoIncidente(
        @NotBlank @Size(max = 200) String titolo,
        @Size(max = 4000) String descrizione,
        @NotNull @PastOrPresent Instant dataRilevamento) {
}