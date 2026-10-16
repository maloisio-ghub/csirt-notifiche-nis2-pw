package com.csirt.notifiche.service;

public class IncidenteNonTrovatoException extends RuntimeException {

    public IncidenteNonTrovatoException(Long id) {
        super("Incidente " + id + " non trovato");
    }
}
