package com.csirt.notifiche.service;

import java.util.List;

import com.csirt.notifiche.model.TipoDocumento;


public class DocumentoIncompletoException extends RuntimeException {

    private final List<String> campiMancanti;

    public DocumentoIncompletoException(TipoDocumento tipo, List<String> campiMancanti) {
        super(tipo.getEtichetta() + " incompleta: mancano " + String.join(", ", campiMancanti));
        this.campiMancanti = List.copyOf(campiMancanti);
    }

    public List<String> getCampiMancanti() {
        return campiMancanti;
    }
}