package com.csirt.notifiche.dto;

import com.csirt.notifiche.model.StatoDocumento;
import com.csirt.notifiche.model.TipoDocumento;

/** Stato sintetico di un documento, per la traccia di esecuzione. */
public record FotoDocumento(Long id, TipoDocumento tipo, int versione, StatoDocumento stato,
                            Integer versioneClassificazione, String hashSha256, Boolean integro) {

    public static FotoDocumento da(DocumentoDto d) {
        return new FotoDocumento(d.id(), d.tipo(), d.versione(), d.stato(), d.versioneClassificazione(),
                d.hashSha256(), d.integro());
    }
}
