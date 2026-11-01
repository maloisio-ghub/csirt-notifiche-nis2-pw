package com.csirt.notifiche.service;

/** Richiesta non accettabile. Diventa 400. */
public class RichiestaNonValidaException extends RuntimeException {

    public RichiestaNonValidaException(String messaggio) {
        super(messaggio);
    }
}