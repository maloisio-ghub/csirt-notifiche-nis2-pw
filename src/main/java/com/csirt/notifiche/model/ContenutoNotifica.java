package com.csirt.notifiche.model;

import static com.csirt.notifiche.model.TipoDocumento.NOTIFICA;
import static com.csirt.notifiche.model.TipoDocumento.PRE_NOTIFICA;
import static com.csirt.notifiche.model.TipoDocumento.RELAZIONE_FINALE;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * Contenuto comune ai tre adempimenti. I Boolean sono oggetti e non primitivi:
 * null significa "non ancora indicato", che è diverso da "no".
 */
@Embeddable
public class ContenutoNotifica {

    @CampoNotifica(ordine = 1, etichetta = "Referente - nome",
            obbligatorioIn = {PRE_NOTIFICA, NOTIFICA, RELAZIONE_FINALE}, riferimento = "punto di contatto")
    @Column(name = "referente_nome", length = 200)
    private String referenteNome;

    @CampoNotifica(ordine = 2, etichetta = "Referente - e-mail",
            obbligatorioIn = {PRE_NOTIFICA, NOTIFICA, RELAZIONE_FINALE}, riferimento = "punto di contatto")
    @Column(name = "referente_email", length = 200)
    private String referenteEmail;

    @CampoNotifica(ordine = 3, etichetta = "Descrizione sintetica",
            obbligatorioIn = {PRE_NOTIFICA, NOTIFICA, RELAZIONE_FINALE}, riferimento = "art. 23.4")
    @Column(name = "descrizione_sintetica", length = 2000)
    private String descrizioneSintetica;

    @CampoNotifica(ordine = 4, etichetta = "Sospetto di atto illegittimo o malevolo",
            obbligatorioIn = {PRE_NOTIFICA, NOTIFICA}, facoltativoIn = {RELAZIONE_FINALE}, riferimento = "art. 23.4.a")
    @Column(name = "sospetto_malevolo")
    private Boolean sospettoMalevolo;

    @CampoNotifica(ordine = 5, etichetta = "Impatto transfrontaliero",
            obbligatorioIn = {PRE_NOTIFICA, NOTIFICA, RELAZIONE_FINALE}, riferimento = "art. 23.4.a, 23.4.d.iv")
    @Column(name = "impatto_transfrontaliero")
    private Boolean impattoTransfrontaliero;

    @CampoNotifica(ordine = 6, etichetta = "Gravità",
            obbligatorioIn = {NOTIFICA, RELAZIONE_FINALE}, riferimento = "art. 23.4.b, 23.4.d.i")
    @Enumerated(EnumType.STRING)
    @Column(name = "gravita", length = 20)
    private GravitaIncidente gravita;

    @CampoNotifica(ordine = 7, etichetta = "Impatto",
            obbligatorioIn = {NOTIFICA, RELAZIONE_FINALE}, riferimento = "art. 23.4.b, 23.4.d.i")
    @Column(name = "impatto", length = 4000)
    private String impatto;

    @CampoNotifica(ordine = 8, etichetta = "Indicatori di compromissione",
            facoltativoIn = {NOTIFICA, RELAZIONE_FINALE}, riferimento = "art. 23.4.b")
    @Column(name = "indicatori_compromissione", length = 4000)
    private String indicatoriCompromissione;

    @CampoNotifica(ordine = 9, etichetta = "Descrizione dettagliata",
            obbligatorioIn = {RELAZIONE_FINALE}, riferimento = "art. 23.4.d.i")
    @Column(name = "descrizione_dettagliata", length = 8000)
    private String descrizioneDettagliata;

    @CampoNotifica(ordine = 10, etichetta = "Tipo di minaccia o causa principale",
            obbligatorioIn = {RELAZIONE_FINALE}, riferimento = "art. 23.4.d.ii")
    @Column(name = "causa_principale", length = 4000)
    private String causaPrincipale;

    @CampoNotifica(ordine = 11, etichetta = "Misure di attenuazione",
            obbligatorioIn = {RELAZIONE_FINALE}, facoltativoIn = {NOTIFICA}, riferimento = "art. 23.4.d.iii")
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

    public static ContenutoNotifica conDescrizione(String descrizioneSintetica) {
        ContenutoNotifica c = new ContenutoNotifica();
        c.descrizioneSintetica = descrizioneSintetica;
        return c;
    }

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