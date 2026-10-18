package com.csirt.notifiche.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/** Codici TC-ACN da assegnare; la motivazione è obbligatoria per le riclassificazioni. */
public record RichiestaClassificazione(
        @NotEmpty List<@NotBlank String> codici,
        @Size(max = 2000) String motivazione) {
}