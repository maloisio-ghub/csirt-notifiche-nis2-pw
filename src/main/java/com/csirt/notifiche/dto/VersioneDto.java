package com.csirt.notifiche.dto;

import java.time.LocalDate;

import com.csirt.notifiche.model.VersioneTassonomia;

public record VersioneDto(String codice, String titolo, String fonteUrl, LocalDate dataPubblicazione,
                          LocalDate dataAcquisizione, String sha256Documento, boolean attiva) {

    public static VersioneDto da(VersioneTassonomia v) {
        return new VersioneDto(v.getCodice(), v.getTitolo(), v.getFonteUrl(), v.getDataPubblicazione(),
                v.getDataAcquisizione(), v.getSha256Documento(), v.isAttiva());
    }
}