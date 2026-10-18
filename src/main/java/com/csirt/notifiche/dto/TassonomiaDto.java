package com.csirt.notifiche.dto;

import java.util.List;

public record TassonomiaDto(VersioneDto versione, List<VoceDto> voci) {
}