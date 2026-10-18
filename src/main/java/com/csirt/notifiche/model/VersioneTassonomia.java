package com.csirt.notifiche.model;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Una versione della TC-ACN caricata nel sistema. Una volta caricata non si modifica. */
@Entity
@Table(name = "versione_tassonomia")
public class VersioneTassonomia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codice", nullable = false, unique = true, length = 50)
    private String codice;

    @Column(name = "titolo", nullable = false, length = 300)
    private String titolo;

    @Column(name = "fonte_url", length = 500)
    private String fonteUrl;

    @Column(name = "data_pubblicazione")
    private LocalDate dataPubblicazione;

    /** Data di acquisizione del documento ufficiale (requisito 2 della traccia). */
    @Column(name = "data_acquisizione", nullable = false)
    private LocalDate dataAcquisizione;

    /** Impronta del PDF ufficiale: identifica la copia esatta usata. */
    @Column(name = "sha256_documento", length = 64)
    private String sha256Documento;

    @Column(name = "file_origine", nullable = false, length = 200)
    private String fileOrigine;

    @Column(name = "data_caricamento", nullable = false)
    private Instant dataCaricamento;

    /** Solo la versione attiva si usa per le nuove classificazioni. */
    @Column(name = "attiva", nullable = false)
    private boolean attiva;

    protected VersioneTassonomia() {
        // richiesto da JPA
    }

    public VersioneTassonomia(String codice, String titolo, String fonteUrl, LocalDate dataPubblicazione,
                              LocalDate dataAcquisizione, String sha256Documento, String fileOrigine,
                              Instant dataCaricamento, boolean attiva) {
        this.codice = codice;
        this.titolo = titolo;
        this.fonteUrl = fonteUrl;
        this.dataPubblicazione = dataPubblicazione;
        this.dataAcquisizione = dataAcquisizione;
        this.sha256Documento = sha256Documento;
        this.fileOrigine = fileOrigine;
        this.dataCaricamento = dataCaricamento;
        this.attiva = attiva;
    }

    public void disattiva() {
        this.attiva = false;
    }

    public Long getId() { return id; }
    public String getCodice() { return codice; }
    public String getTitolo() { return titolo; }
    public String getFonteUrl() { return fonteUrl; }
    public LocalDate getDataPubblicazione() { return dataPubblicazione; }
    public LocalDate getDataAcquisizione() { return dataAcquisizione; }
    public String getSha256Documento() { return sha256Documento; }
    public String getFileOrigine() { return fileOrigine; }
    public Instant getDataCaricamento() { return dataCaricamento; }
    public boolean isAttiva() { return attiva; }
}