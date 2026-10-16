package com.csirt.notifiche.service;

/** Richiesta formalmente corretta ma non accettabile (es. servizio inesistente). Diventa 400. */
public class RichiestaNonValidaException extends RuntimeException {

    public RichiestaNonValidaException(String messaggio) {
        super(messaggio);
    }
}