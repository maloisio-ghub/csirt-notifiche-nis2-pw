package com.csirt.notifiche.service;

/** Richiesta formalmente corretta ma non accettabile. Diventa 400. */
public class RichiestaNonValidaException extends RuntimeException {

    public RichiestaNonValidaException(String messaggio) {
        super(messaggio);
    }
}