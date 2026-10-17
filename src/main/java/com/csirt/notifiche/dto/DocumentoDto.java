package com.csirt.notifiche.dto;

import java.time.Instant;

import com.csirt.notifiche.model.Documento;
import com.csirt.notifiche.model.StatoDocumento;
import com.csirt.notifiche.model.TipoDocumento;

public record DocumentoDto(
        Long id,
        Long incidenteId,
        TipoDocumento tipo,
        int versione,
        StatoDocumento stato,
        ContenutoDto contenuto,
        Instant dataCreazione,
        Instant dataUltimaModifica,
        Instant dataTrasmissione) {

    public static DocumentoDto da(Documento d) {
        return new DocumentoDto(d.getId(), d.getIncidente().getId(), d.getTipo(), d.getVersione(), d.getStato(),
                ContenutoDto.da(d.getContenuto()), d.getDataCreazione(), d.getDataUltimaModifica(),
                d.getDataTrasmissione());
    }
}