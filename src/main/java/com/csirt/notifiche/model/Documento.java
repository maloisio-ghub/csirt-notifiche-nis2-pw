package com.csirt.notifiche.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;

/**
 * Un adempimento verso il CSIRT. Ogni tipo può avere più versioni: una versione
 * trasmessa non si modifica, un aggiornamento è una versione nuova.
 */
@Entity
@Table(name = "documento", uniqueConstraints = @UniqueConstraint(
        name = "uk_documento_versione", columnNames = {"incidente_id", "tipo", "versione"}))
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incidente_id", nullable = false)
    private Incidente incidente;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoDocumento tipo;

    @Column(name = "versione", nullable = false)
    private int versione;

    @Enumerated(EnumType.STRING)
    @Column(name = "stato", nullable = false, length = 20)
    private StatoDocumento stato;

    @Embedded
    private ContenutoNotifica contenuto;

    @Column(name = "data_creazione", nullable = false)
    private Instant dataCreazione;

    @Column(name = "data_ultima_modifica", nullable = false)
    private Instant dataUltimaModifica;

    @Column(name = "data_trasmissione")
    private Instant dataTrasmissione;

    /** Impronta SHA-256, calcolata alla trasmissione. */
    @Column(name = "hash_sha256", length = 64)
    private String hashSha256;

    /** Classificazione in vigore al momento della trasmissione: non cambia se l'incidente viene riclassificato. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classificazione_id")
    private Classificazione classificazione;

    /** Vero se nel database il documento risulta già trasmesso. Non è una colonna. */
    @Transient
    private boolean trasmessoNelDatabase;

    protected Documento() {
        // richiesto da JPA
    }

    public Documento(Incidente incidente, TipoDocumento tipo, int versione, ContenutoNotifica contenuto,
                     Instant adesso) {
        this.incidente = incidente;
        this.tipo = tipo;
        this.versione = versione;
        this.contenuto = contenuto.copia();
        this.stato = StatoDocumento.BOZZA;
        this.dataCreazione = adesso;
        this.dataUltimaModifica = adesso;
    }

    /** Salvataggio in bozza. */
    public void aggiornaContenuto(ContenutoNotifica nuovo, Instant adesso) {
        verificaBozza();
        this.contenuto = nuovo.copia();
        this.dataUltimaModifica = adesso;
    }

    /** Trasmissione: da qui in poi il documento è immutabile. */
    public void trasmetti(Instant momento, Classificazione classificazioneInVigore, String impronta) {
        verificaBozza();
        this.stato = StatoDocumento.TRASMESSO;
        this.dataTrasmissione = momento;
        this.classificazione = classificazioneInVigore;
        this.hashSha256 = impronta;
    }

    private void verificaBozza() {
        if (stato != StatoDocumento.BOZZA) {
            throw new IllegalStateException(tipo.getEtichetta() + " v" + versione
                    + " è già stata trasmessa: non è modificabile");
        }
    }

    // linea di difesa: anche se qualcuno aggirasse i metodi sopra,
    // Hibernate rifiuta di aggiornare o cancellare una riga già trasmessa.

    @PostLoad
    @PostPersist
    @PostUpdate
    void sincronizzaStatoDatabase() {
        trasmessoNelDatabase = stato == StatoDocumento.TRASMESSO;
    }

    @PreUpdate
    @PreRemove
    void impedisciModificaTrasmesso() {
        if (trasmessoNelDatabase) {
            throw new IllegalStateException("Il documento " + id + " è trasmesso: non può essere modificato né cancellato");
        }
    }

    public boolean isBozza() {
        return stato == StatoDocumento.BOZZA;
    }

    public boolean isTrasmesso() {
        return stato == StatoDocumento.TRASMESSO;
    }

    public ContenutoNotifica getContenuto() {
        return contenuto == null ? new ContenutoNotifica() : contenuto;
    }

    public Long getId() { return id; }
    public Incidente getIncidente() { return incidente; }
    public TipoDocumento getTipo() { return tipo; }
    public int getVersione() { return versione; }
    public StatoDocumento getStato() { return stato; }
    public Instant getDataCreazione() { return dataCreazione; }
    public Instant getDataUltimaModifica() { return dataUltimaModifica; }
    public Instant getDataTrasmissione() { return dataTrasmissione; }
    public String getHashSha256() { return hashSha256; }
    public Classificazione getClassificazione() { return classificazione; }
    
}