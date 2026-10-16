package com.csirt.notifiche.model;

import com.csirt.notifiche.significativita.ImpattoServizio;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Impatto su un servizio come era al momento della valutazione (base utenti compresa). */
@Embeddable
public class ImpattoValutato {

    @Column(name = "codice_servizio", nullable = false, length = 10)
    private String codiceServizio;

    @Column(name = "base_utenti", nullable = false)
    private long baseUtenti;

    @Column(name = "minuti_indisponibilita_totale", nullable = false)
    private int minutiIndisponibilitaTotale;

    @Column(name = "minuti_disponibilita_limitata", nullable = false)
    private int minutiDisponibilitaLimitata;

    @Column(name = "utenti_disponibilita_limitata", nullable = false)
    private long utentiDisponibilitaLimitata;

    @Column(name = "compromissione_malevola", nullable = false)
    private boolean compromissioneMalevola;

    @Column(name = "utenti_compromissione", nullable = false)
    private long utentiCompromissione;

    protected ImpattoValutato() {
        // richiesto da JPA
    }

    public ImpattoValutato(ImpattoServizio i) {
        this.codiceServizio = i.codiceServizio();
        this.baseUtenti = i.baseUtenti();
        this.minutiIndisponibilitaTotale = i.minutiIndisponibilitaTotale();
        this.minutiDisponibilitaLimitata = i.minutiDisponibilitaLimitata();
        this.utentiDisponibilitaLimitata = i.utentiDisponibilitaLimitata();
        this.compromissioneMalevola = i.compromissioneMalevola();
        this.utentiCompromissione = i.utentiCompromissione();
    }

    public ImpattoServizio comeImpatto() {
        return new ImpattoServizio(codiceServizio, baseUtenti, minutiIndisponibilitaTotale,
                minutiDisponibilitaLimitata, utentiDisponibilitaLimitata, compromissioneMalevola, utentiCompromissione);
    }
}