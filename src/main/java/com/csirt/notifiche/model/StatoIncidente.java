package com.csirt.notifiche.model;

public enum StatoIncidente {
    REGISTRATO,          // inserito, significatività non ancora valutata
    SIGNIFICATIVO,       // fa partire gli obblighi di notifica
    NON_SIGNIFICATIVO,   // valutato: nessun obbligo
    CHIUSO               // relazione finale trasmessa
}

