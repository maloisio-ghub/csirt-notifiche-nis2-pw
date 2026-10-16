package com.csirt.notifiche.service;

/** Operazione non ammessa nello stato attuale dell'incidente. Diventa 409. */
public class OperazioneNonConsentitaException extends RuntimeException {

    public OperazioneNonConsentitaException(String messaggio) {
        super(messaggio);
    }
}
