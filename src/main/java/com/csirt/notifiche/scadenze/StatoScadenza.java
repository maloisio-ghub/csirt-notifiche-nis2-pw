package com.csirt.notifiche.scadenze;

public enum StatoScadenza {
    NON_ATTIVA,  // l'incidente non è (ancora) significativo: nessun obbligo
    IN_ATTESA,   // il termine non decorre ancora (relazione finale prima della notifica)
    IN_CORSO,    // termine aperto
    SCADUTA      // termine superato
}