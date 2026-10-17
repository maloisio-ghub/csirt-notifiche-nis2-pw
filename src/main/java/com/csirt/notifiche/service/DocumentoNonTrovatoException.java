package com.csirt.notifiche.service;

public class DocumentoNonTrovatoException extends RuntimeException {

    public DocumentoNonTrovatoException(Long id) {
        super("Documento " + id + " non trovato");
    }
}