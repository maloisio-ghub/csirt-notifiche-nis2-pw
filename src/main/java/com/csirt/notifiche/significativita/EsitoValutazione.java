package com.csirt.notifiche.significativita;

import java.util.List;

public record EsitoValutazione(List<RegolaScattata> regole) {

    public EsitoValutazione {
        regole = List.copyOf(regole);
    }

    /** Basta una regola soddisfatta perché l'incidente sia significativo. */
    public boolean significativo() {
        return !regole.isEmpty();
    }

    public List<String> codici() {
        return regole.stream().map(RegolaScattata::codice).toList();
    }
}
