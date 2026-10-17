package com.csirt.notifiche.dto;

import com.csirt.notifiche.model.TipoDocumento;

import jakarta.validation.constraints.NotNull;

public record NuovoDocumento(@NotNull TipoDocumento tipo) {
}