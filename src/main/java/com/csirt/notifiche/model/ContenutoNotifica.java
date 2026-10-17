package com.csirt.notifiche.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * Contenuto comune ai tre adempimenti. */
@Embeddable
public class ContenutoNotifica {

    @Column(name = "referente_nome", length = 200)
    private String referenteNome;

    @Column(name = "referente_email", length = 200)
    private String referenteEmail;

    @Column(name = "descrizione_sintetica", length = 2000)
    private String descrizioneSintetica;

    @Column(name = "sospetto_malevolo")
    private Boolean sospettoMalevolo;

    @Column(name = "impatto_transfrontaliero")
    private Boolean impattoTransfrontaliero;

    @Enumerated(EnumType.STRING)
    @Column(name = "gravita", length = 20)
    private GravitaIncidente gravita;

    @Column(name = "impatto", length = 4000)
    private String impatto;

    @Column(name = "indicatori_compromissione", length = 4000)
    private String indicatoriCompromissione;

    @Column(name = "descrizione_dettagliata", length = 8000)
    private String descrizioneDettagliata;

    @Column(name = "causa_principale", length = 4000)
    private String causaPrincipale;

    @Column(name = "misure_mitigazione", length = 4000)
    private String misureMitigazione;

    public ContenutoNotifica() {
        // contenuto vuoto (richiesto anche da JPA)
    }

    public ContenutoNotifica(String referenteNome, String referenteEmail, String descrizioneSintetica,
                             Boolean sospettoMalevolo, Boolean impattoTransfrontaliero, GravitaIncidente gravita,
                             String impatto, String indicatoriCompromissione, String descrizioneDettagliata,
                             String causaPrincipale, String misureMitigazione) {
        this.referenteNome = referenteNome;
        this.referenteEmail = referenteEmail;
        this.descrizioneSintetica = descrizioneSintetica;
        this.sospettoMalevolo = sospettoMalevolo;
        this.impattoTransfrontaliero = impattoTransfrontaliero;
        this.gravita = gravita;
        this.impatto = impatto;
        this.indicatoriCompromissione = indicatoriCompromissione;
        this.descrizioneDettagliata = descrizioneDettagliata;
        this.causaPrincipale = causaPrincipale;
        this.misureMitigazione = misureMitigazione;
    }

    /** Contenuto iniziale di un incidente senza documenti */
    public static ContenutoNotifica conDescrizione(String descrizioneSintetica) {
        ContenutoNotifica c = new ContenutoNotifica();
        c.descrizioneSintetica = descrizioneSintetica;
        return c;
    }

    /** Copia indipendente */
    public ContenutoNotifica copia() {
        return new ContenutoNotifica(referenteNome, referenteEmail, descrizioneSintetica, sospettoMalevolo,
                impattoTransfrontaliero, gravita, impatto, indicatoriCompromissione, descrizioneDettagliata,
                causaPrincipale, misureMitigazione);
    }

    public String getReferenteNome() { return referenteNome; }
    public String getReferenteEmail() { return referenteEmail; }
    public String getDescrizioneSintetica() { return descrizioneSintetica; }
    public Boolean getSospettoMalevolo() { return sospettoMalevolo; }
    public Boolean getImpattoTransfrontaliero() { return impattoTransfrontaliero; }
    public GravitaIncidente getGravita() { return gravita; }
    public String getImpatto() { return impatto; }
    public String getIndicatoriCompromissione() { return indicatoriCompromissione; }
    public String getDescrizioneDettagliata() { return descrizioneDettagliata; }
    public String getCausaPrincipale() { return causaPrincipale; }
    public String getMisureMitigazione() { return misureMitigazione; }
}