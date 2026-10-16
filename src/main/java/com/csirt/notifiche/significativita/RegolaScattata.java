package com.csirt.notifiche.significativita;

/** Una regola soddisfatta, con il riferimento normativo e i numeri che l'hanno fatta scattare. */
public record RegolaScattata(String codice, String riferimento, String motivazione) {
}
