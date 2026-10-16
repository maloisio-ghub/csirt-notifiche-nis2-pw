package com.csirt.notifiche.significativita;

/** Effetti dell'incidente su un singolo servizio critico. */
public record ImpattoServizio(
        String codiceServizio,
        long baseUtenti,
        int minutiIndisponibilitaTotale,
        int minutiDisponibilitaLimitata,
        long utentiDisponibilitaLimitata,
        boolean compromissioneMalevola,
        long utentiCompromissione) {
}
