package com.csirt.notifiche.dto;

import java.time.Instant;

import com.csirt.notifiche.documenti.RegoleDocumento;
import com.csirt.notifiche.model.Documento;
import com.csirt.notifiche.model.StatoDocumento;
import com.csirt.notifiche.model.TipoDocumento;

/** integro: per un documento trasmesso, vero se l'impronta ricalcolata coincide con quella salvata. */
public record DocumentoDto(
        Long id,
        Long incidenteId,
        TipoDocumento tipo,
        int versione,
        StatoDocumento stato,
        ContenutoDto contenuto,
        Instant dataCreazione,
        Instant dataUltimaModifica,
        Instant dataTrasmissione,
        String hashSha256,
        Boolean integro) {

    public static DocumentoDto da(Documento d) {
        Boolean integro = d.isBozza()
                ? null
                : RegoleDocumento.sha256(RegoleDocumento.testoCanonico(d, d.getDataTrasmissione()))
                        .equals(d.getHashSha256());
        return new DocumentoDto(d.getId(), d.getIncidente().getId(), d.getTipo(), d.getVersione(), d.getStato(),
                ContenutoDto.da(d.getContenuto()), d.getDataCreazione(), d.getDataUltimaModifica(),
                d.getDataTrasmissione(), d.getHashSha256(), integro);
    }
}