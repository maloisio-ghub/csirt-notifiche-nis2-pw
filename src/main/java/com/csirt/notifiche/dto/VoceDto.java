package com.csirt.notifiche.dto;

import com.csirt.notifiche.model.TipoVoce;
import com.csirt.notifiche.model.VoceTassonomia;

public record VoceDto(String codice, TipoVoce tipo, String nome, String padre, boolean selezioneMultipla,
                      String gruppo, String descrizione) {

    public static VoceDto da(VoceTassonomia v) {
        return new VoceDto(v.getCodice(), v.getTipo(), v.getNome(),
                v.getPadre() == null ? null : v.getPadre().getCodice(), v.isSelezioneMultipla(), v.getGruppo(),
                v.getDescrizione());
    }
}