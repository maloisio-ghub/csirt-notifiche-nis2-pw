package com.csirt.notifiche.tassonomia;

public class TassonomiaNonValidaException extends RuntimeException {

    public TassonomiaNonValidaException(String messaggio) {
        super(messaggio);
    }
}