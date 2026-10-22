package com.csirt.notifiche.dto;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

import com.csirt.notifiche.documenti.RegoleDocumento;
import com.csirt.notifiche.documenti.Riclassificazioni.Riclassificazione;
import com.csirt.notifiche.model.Documento;
import com.csirt.notifiche.model.StatoDocumento;
import com.csirt.notifiche.model.TipoDocumento;

/**
 riclassificazioni: solo per la relazione finale, i cambi avvenuti dopo la notifica.
 */
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
        Integer versioneClassificazione,
        String classificazione,
        List<Riclassificazione> riclassificazioni,
        String hashSha256,
        Boolean integro) {

    public static DocumentoDto da(Documento d, List<Riclassificazione> riclassificazioni) {
        Boolean integro = d.isBozza()
                ? null
                : RegoleDocumento.sha256(RegoleDocumento.testoCanonico(d, d.getDataTrasmissione(),
                        d.getClassificazione(), riclassificazioni)).equals(d.getHashSha256());
        return new DocumentoDto(d.getId(), d.getIncidente().getId(), d.getTipo(), d.getVersione(), d.getStato(),
                ContenutoDto.da(d.getContenuto()), d.getDataCreazione(), d.getDataUltimaModifica(),
                d.getDataTrasmissione(),
                d.getClassificazione() == null ? null : d.getClassificazione().getVersione(),
                d.getClassificazione() == null ? null : d.getClassificazione().codiciOrdinati(),
                riclassificazioni, d.getHashSha256(), integro);
    }

        /** Nome del file da scaricare, es. notifica-incidente-1-v2.pdf */
    public String nomeFile(String estensione) {
        return tipo.name().toLowerCase(Locale.ROOT).replace('_', '-') + "-incidente-" + incidenteId
                + "-v" + versione + "." + estensione;
    }
}