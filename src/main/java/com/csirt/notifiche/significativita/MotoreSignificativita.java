package com.csirt.notifiche.significativita;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Applica le regole R1-R10 (Reg. di esecuzione UE 2024/2690, artt. 3 e 10).
 * Tutte le soglie sono "più di": si usa il confronto stretto.
 */
public class MotoreSignificativita {

    public EsitoValutazione valuta(DatiValutazione dati, ParametriSoglia p) {
        List<RegolaScattata> scattate = new ArrayList<>();

        // Criteri orizzontali (art. 3)
        BigDecimal sogliaPerdita = p.sogliaPerdita();
        if (dati.perditaEconomica().compareTo(sogliaPerdita) > 0) {
            scattate.add(new RegolaScattata("R1", "art. 3.1.a",
                    "perdita economica " + dati.perditaEconomica() + " € > " + sogliaPerdita + " €"));
        }
        if (dati.esfiltrazioneSegretiCommerciali()) {
            scattate.add(new RegolaScattata("R2", "art. 3.1.b", "esfiltrazione di segreti commerciali"));
        }
        if (dati.decesso()) {
            scattate.add(new RegolaScattata("R3", "art. 3.1.c", "decesso di una persona fisica"));
        }
        if (dati.dannoConsiderevoleSalute()) {
            scattate.add(new RegolaScattata("R4", "art. 3.1.d", "danno considerevole alla salute"));
        }
        if (dati.accessoMalevoloConGravePerturbazione()) {
            scattate.add(new RegolaScattata("R5", "art. 3.1.e",
                    "accesso non autorizzato presumibilmente malevolo, con possibile grave perturbazione"));
        }

        // Criteri per  (art. 10)
        for (ImpattoServizio s : dati.impatti()) {
            long sogliaUtenti = p.sogliaUtenti(s.baseUtenti());
            String servizio = " [" + s.codiceServizio() + "]";

            if (s.minutiIndisponibilitaTotale() > p.minutiIndisponibilitaTotale()) {
                scattate.add(new RegolaScattata("R7", "art. 10.a" + servizio,
                        "indisponibilità totale " + s.minutiIndisponibilitaTotale() + " min > "
                                + p.minutiIndisponibilitaTotale() + " min"));
            }
            if (s.utentiDisponibilitaLimitata() > sogliaUtenti
                    && s.minutiDisponibilitaLimitata() > p.minutiDisponibilitaLimitata()) {
                scattate.add(new RegolaScattata("R8", "art. 10.b" + servizio,
                        "disponibilità limitata per " + s.utentiDisponibilitaLimitata() + " utenti > "
                                + sogliaUtenti + " per " + s.minutiDisponibilitaLimitata() + " min > "
                                + p.minutiDisponibilitaLimitata() + " min"));
            }
            if (s.compromissioneMalevola()) {
                scattate.add(new RegolaScattata("R9", "art. 10.c" + servizio,
                        "dati o servizi compromessi da azione presumibilmente malevola"));
            }
            if (s.utentiCompromissione() > sogliaUtenti) {
                scattate.add(new RegolaScattata("R10", "art. 10.d" + servizio,
                        "compromissione per " + s.utentiCompromissione() + " utenti > " + sogliaUtenti));
            }
        }
        return new EsitoValutazione(scattate);
    }
}
