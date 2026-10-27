package com.csirt.notifiche.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Una voce della TC-ACN (macrocategoria, predicato o valore). */
@Entity
@Table(name = "voce_tassonomia", uniqueConstraints = @UniqueConstraint(
        name = "uk_voce_versione_codice", columnNames = {"versione_id", "codice"}))
public class VoceTassonomia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "versione_id", nullable = false)
    private VersioneTassonomia versione;

    /** Livello superiore: null per le macrocategorie. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "padre_id")
    private VoceTassonomia padre;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoVoce tipo;

    /** Codice ufficiale, riportato come nel documento ACN. */
    @Column(name = "codice", nullable = false, length = 40)
    private String codice;

    @Column(name = "nome", nullable = false, length = 200)
    private String nome;

    @Column(name = "descrizione", length = 2000)
    private String descrizione;

    /** Solo per i predicati: se un incidente può avere più valori di questo predicato. */
    @Column(name = "selezione_multipla", nullable = false)
    private boolean selezioneMultipla;

    /** Sotto-predicato di raggruppamento (solo valori di Involved Asset). */
    @Column(name = "gruppo", length = 100)
    private String gruppo;

    /** Ordine di presentazione, uguale a quello del documento ufficiale. */
    @Column(name = "ordine", nullable = false)
    private int ordine;

    protected VoceTassonomia() {
        // richiesto da JPA
    }

    public VoceTassonomia(VersioneTassonomia versione, VoceTassonomia padre, TipoVoce tipo, String codice,
                          String nome, String descrizione, boolean selezioneMultipla, String gruppo, int ordine) {
        this.versione = versione;
        this.padre = padre;
        this.tipo = tipo;
        this.codice = codice;
        this.nome = nome;
        this.descrizione = descrizione;
        this.selezioneMultipla = selezioneMultipla;
        this.gruppo = gruppo;
        this.ordine = ordine;
    }

    public Long getId() { return id; }
    public VersioneTassonomia getVersione() { return versione; }
    public VoceTassonomia getPadre() { return padre; }
    public TipoVoce getTipo() { return tipo; }
    public String getCodice() { return codice; }
    public String getNome() { return nome; }
    public String getDescrizione() { return descrizione; }
    public boolean isSelezioneMultipla() { return selezioneMultipla; }
    public String getGruppo() { return gruppo; }
    public int getOrdine() { return ordine; }
}