package com.csirt.notifiche.scadenze;

public enum StatoScadenza {
    NON_ATTIVA,            // l'incidente non è significativo: nessun obbligo
    IN_ATTESA,             // il termine non decorre ancora (relazione finale prima della notifica)
    IN_CORSO,              // termine aperto, documento non ancora trasmesso
    SCADUTA,               // termine superato senza trasmissione
    RISPETTATA,            // trasmesso entro il termine
    TRASMESSA_IN_RITARDO   // trasmesso dopo il termine
}